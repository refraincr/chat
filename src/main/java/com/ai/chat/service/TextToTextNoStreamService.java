package com.ai.chat.service;

import com.ai.chat.bo.ChatBO;
import com.ai.chat.common.Token;
import com.ai.chat.constants.BasePrompt;
import com.ai.chat.constants.ModelName;
import com.ai.chat.memory.user.ChatMemoryManager;
import com.ai.chat.memory.user.ChatMessage;
import com.ai.chat.memory.user.SessionMemory;
import com.openai.client.OpenAIClient;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TextToTextNoStreamService {
    private final OpenAIClient client;
    private final ChatMemoryManager memoryManager;

    // 无工具调用，无流式
    public String textToText(ChatBO chatBO) {
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
        for (int i = messages.size() - 1; i >= 0; i--) {
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

        // 语音输出简洁,设定最大 token
        builder.maxCompletionTokens(2048);

        ChatCompletionCreateParams params = builder.build();

        // 模型回复
        ChatCompletion chatCompletion = client.chat().completions().create(params);

        // 取得结果

        return chatCompletion.choices().get(0).message().content().orElse("");
    }
}
