package com.ai.chat.memory.user;

import com.ai.chat.common.Token;
import com.ai.chat.constants.ModelName;
import com.ai.chat.constants.SummaryPrompt;
import com.openai.client.OpenAIClient;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class RecursionSummary {
    private final OpenAIClient client;
    private final ChatMemoryManager memoryManager;

    // 开始总结摘要的 token 值
    private final int TOKEN_TO_SUMMARY = 8192;

    // checkPointer
    private int pointer = 0;

    // 摘要
    @Getter
    private String currentSummary = "";

    public RecursionSummary(OpenAIClient client, ChatMemoryManager memoryManager) {
        this.client = client;
        this.memoryManager = memoryManager;
    }

    @Async
    public void summary(String userId,String sessionId) {
        String messagesPrompt = GetMessages(userId, sessionId);

        String userPrompt = "Summary:\n\n" + currentSummary + "\n\n" +  messagesPrompt;

        // 发送给 qwen3.7-plus 模型
        ChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
                .model(ModelName.QWEN_3DOT7_PLUS)
                .temperature(0.1)
                .addSystemMessage(SummaryPrompt.SUMMARY_PROMPT)
                .addUserMessage(userPrompt)
                .build();

        ChatCompletion completion = client.chat().completions().create(params);

        currentSummary = completion.choices().get(0).message().content().orElse("");
    }

    public boolean summaryNeed(String userId,String sessionId) {
        // 统计 token 数 （单个 session 而言）
        try {
            SessionMemory sessionMemory = memoryManager.getSession(userId,sessionId);

            // 取得单节回话的所有消息
            List<ChatMessage> messages = sessionMemory.getHistory();
            int token = 0;
            int start = pointer == -1 ? 0 : pointer;
            for (int i = start; i < messages.size(); i++) {
                ChatMessage  message = messages.get(i);
                token+= Token.getTokens(message.getMessage());
                if (token>=TOKEN_TO_SUMMARY){
                    pointer = i - 1;
                    break;
                }
            }
            return token >= TOKEN_TO_SUMMARY;
        } catch (Exception e) {
            log.error(e.getMessage());
            return false;
        }
    }

    private String GetMessages(String userId,String sessionId) {
        // 读取当前用户聊天记录
        SessionMemory sessionMemory = memoryManager.getSession(userId,sessionId);

        /*
        * 格式
        *   History:

            [1]
            User:
            你好

            Assistant:
            你好
        * */
        List<ChatMessage> messages = sessionMemory.getHistory();
        StringBuilder messagesPromptBuilder = new StringBuilder();
        messagesPromptBuilder.append("History:\n\n");
        int round = 1;
        int start = pointer == -1 ? 0 : pointer;
        for (int i = start;i<messages.size();i+=2) {
            messagesPromptBuilder.append("[").append(round++).append("]\n");

            messagesPromptBuilder.append("User:\n")
                    .append(messages.get(i).getMessage())
                    .append("\n\n");

            if (i + 1 < messages.size()) {
                messagesPromptBuilder.append("Assistant:\n")
                        .append(messages.get(i + 1).getMessage())
                        .append("\n\n");
            }
        }
        return messagesPromptBuilder.toString();
    }
}
