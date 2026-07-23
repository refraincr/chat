package com.ai.chat.service;

import com.ai.chat.bo.ChatBO;
import com.ai.chat.common.Token;
import com.ai.chat.constants.BasePrompt;
import com.ai.chat.constants.ModelName;
import com.ai.chat.memory.user.ChatMemoryManager;
import com.ai.chat.memory.user.ChatMessage;
import com.ai.chat.memory.user.RecursionSummary;
import com.ai.chat.memory.user.SessionMemory;
import com.ai.chat.tool.ToolRegistry;
import com.openai.client.OpenAIClient;
import com.openai.core.JsonValue;
import com.openai.core.http.StreamResponse;
import com.openai.models.chat.completions.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Slf4j
@Service
@RequiredArgsConstructor  // 带有final的成员变量自动在构造函数里赋值
public class ChatService {
    private final OpenAIClient client;
    private final ChatMemoryManager memoryManager;
    private final RecursionSummary recursionSummary;
    private final ToolRegistry toolRegistry;

    // 防止死循环
    private static final int MAX_TOOL_ROUNDS = 5;

    public void chat(ChatBO chatBO, SseEmitter emitter) {
        // 构造参数
        ChatCompletionCreateParams.Builder builder =
                ChatCompletionCreateParams.builder().model(ModelName.QWEN_3DOT7_MAX);

        // 系统消息
        builder.addSystemMessage(BasePrompt.BASE);

        // 所有的用户消息
        SessionMemory history = memoryManager.getSession(
                chatBO.getUserId(),
                chatBO.getSessionId()
        );

        // 取得最近token<=ChatMemoryManager.maxCurrentToken的消息
        List<ChatMessage> messages = history.getHistory();
        int currentToken = 0;
        for (int i = messages.size()-1;i>=0;i--) {
            ChatMessage message = messages.get(i);
            if ("user".equals(message.getRole())) {
                builder.addUserMessage(message.getMessage());
            } else {
                builder.addAssistantMessage(message.getMessage());
            }
            currentToken += Token.getTokens(message.getMessage());
            if (currentToken >= ChatMemoryManager.maxCurrentToken) break;
        }

        // 添加当前消息
        builder.addUserMessage(chatBO.getMessage());

        // 摘要？
        if (recursionSummary.summaryNeed(chatBO.getUserId(), chatBO.getSessionId())) {
            recursionSummary.summary(chatBO.getUserId(), chatBO.getSessionId());
            builder.addAssistantMessage(recursionSummary.getCurrentSummary());
        }

        // 注册工具
        toolRegistry.getTools().forEach(builder::addTool);

        StringBuilder finalAnswer = new StringBuilder();
        try {
            runConversation(builder,emitter,finalAnswer,0);

            history.add(new ChatMessage(chatBO.getMessage(), "user"));
            history.add(new ChatMessage(finalAnswer.toString(), "assistant"));

            emitter.complete();
        } catch (IOException e) {
            log.error("chat error", e);
            emitter.completeWithError(e);
        }
    }

    private void runConversation(ChatCompletionCreateParams.Builder builder,
                                 SseEmitter emitter,
                                 StringBuilder finalAnswer,
                                 int round) throws IOException {
        // 大于限定递归调用值退出
        if (round >= MAX_TOOL_ROUNDS) {
            log.warn("已达到最大工具调用轮次({})，强制结束", MAX_TOOL_ROUNDS);
            return;
        }

        ChatCompletionCreateParams params = builder.build();

        // index -> 累加器, 用于拼接分片的 tool_call
        Map<Long,ToolCallAccumulator> toolCallMap = new TreeMap<>();
        StringBuilder roundContent = new StringBuilder();

        try (StreamResponse<ChatCompletionChunk> stream = client.chat().completions().createStreaming(params)) {
            stream.stream().forEach(chunk -> {
                if (chunk.choices().isEmpty()) return;
                var delta = chunk.choices().get(0).delta();

                // 正文
                delta.content().ifPresent(content -> {
                    if (!content.isBlank()) {
                        finalAnswer.append(content);
                        roundContent.append(content);
                        try {
                            emitter.send(content);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                });

                // 工具拼接
                delta.toolCalls().ifPresent(toolCalls -> {
                    for (var tc : toolCalls) {
                        long idx = tc.index();

                        ToolCallAccumulator acc = toolCallMap.computeIfAbsent(idx, k -> new ToolCallAccumulator());
                        tc.id().ifPresent(acc::setId);
                        tc.function().ifPresent(fn->{
                            fn.name().ifPresent(acc::setName);
                            fn.arguments().ifPresent(acc::setArguments);
                        });
                    }
                });
            });
        }

        // 没有工具 => 返回
        if (toolCallMap.isEmpty()) return;

        // 构造参数，准备下一轮的请求

        // 工具调用以及 roundContent 加入上下文
        List<ChatCompletionMessageToolCall> toolCallParams = new ArrayList<>();
        for (ToolCallAccumulator acc : toolCallMap.values()) {
            toolCallParams.add(ChatCompletionMessageToolCall.ofFunction(
                            ChatCompletionMessageFunctionToolCall.builder()
                                    .id(acc.id)
                                    .type(JsonValue.from("function"))
                                    .function(
                                            ChatCompletionMessageFunctionToolCall.Function.builder()
                                                    .name(acc.getName())
                                                    .arguments(acc.getArguments())
                                                    .build())
                                    .build()
                    )
            );
        }

        ChatCompletionAssistantMessageParam.Builder assistantMsgBuilder =
                ChatCompletionAssistantMessageParam.builder();
        // 内容
        if (!roundContent.isEmpty()) {
            assistantMsgBuilder.content(roundContent.toString());
        }
        // 工具
        assistantMsgBuilder.toolCalls(toolCallParams);
        builder.addMessage(assistantMsgBuilder.build());

        // 工具调用以及 roundContent 加入上下文完毕，执行工具
        for (ToolCallAccumulator acc : toolCallMap.values()) {
            String result;
            try {
                result = toolRegistry.execute(acc.getName(), acc.getArguments());
            } catch (Exception e) {
                log.error("工具调用失败: name={}, args={}", acc.getName(), acc.getArguments(), e);
                result = "工具执行出错: " + e.getMessage();
            }
            builder.addMessage(
                    ChatCompletionToolMessageParam.builder()
                            .toolCallId(acc.getId())
                            .content(result)
                            .build()
            );
        }

        // 递归调用
        runConversation(builder, emitter, finalAnswer, round + 1);
    }

    // 流式场景下单个 tool_call 分片累加器
    private static class ToolCallAccumulator {
        private String id = "";
        private final StringBuilder name = new StringBuilder();
        private final StringBuilder arguments = new StringBuilder();

        void setId(String id) {if (id != null && id.isBlank()) this.id = id;}
        void setName(String n) {if (n != null) name.append(n);}
        void setArguments(String a) {if (a != null) arguments.append(a);}
        String getId() {return id;}
        String getName() {return name.toString();}
        String getArguments() {return arguments.toString();}
    }
}
