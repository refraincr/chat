package com.ai.chat.service;

import java.io.IOException;
import java.util.*;

import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversation;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationParam;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationResult;
import com.alibaba.dashscope.common.MultiModalMessage;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.exception.UploadFileException;
import com.alibaba.dashscope.utils.JsonUtils;
import com.alibaba.dashscope.utils.Constants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Service
public class VoiceToTextService {
    static {Constants.baseHttpApiUrl="https://llm-22dref9z04zuih6n.cn-beijing.maas.aliyuncs.com/api/v1";}
    private String simpleMultiModalConversationCall(MultipartFile file)
            throws ApiException, NoApiKeyException, UploadFileException, IOException {
        MultiModalConversation conv = new MultiModalConversation();

        // 将传来的 MultipartFile 转为base64编码
        String base64Audio = Base64.getEncoder().encodeToString(file.getBytes());

        // 拼接成符合要求的 url
        String audioDataUri = "data:audio/wav;base64," +  base64Audio;

        MultiModalMessage userMessage = MultiModalMessage.builder()
                .role(Role.USER.getValue())
                .content(List.of(
                        Collections.singletonMap("audio", audioDataUri)))
                .build();

        MultiModalMessage sysMessage = MultiModalMessage.builder().role(Role.SYSTEM.getValue())
                // Configure the context for customized recognition
                .content(List.of(Collections.singletonMap("text", "")))
                .build();

        Map<String, Object> asrOptions = new HashMap<>();
        asrOptions.put("enable_lid", true);
        asrOptions.put("enable_itn", false);
        // asrOptions.put("language", "zh"); // Optional. If you know the language in the audio, provide this parameter to improve recognition accuracy
        MultiModalConversationParam param = MultiModalConversationParam.builder()
                // If the environment variable is not set, replace it with your Model Studio API key: .apiKey("sk-xxx")
                .apiKey(System.getenv("ALIYUN_API_KEY"))
                .model("qwen3-asr-flash")
                .message(userMessage)
                .message(sysMessage)
                .parameter("asr_options", asrOptions)
                .build();
        MultiModalConversationResult result = conv.call(param);
        String res = JsonUtils.toJson(result);
        ObjectMapper om = new ObjectMapper();
        JsonNode jn = om.readTree(res);
        return jn.at("/output/choices/0/message/content/0/text").toString();
    }

    public String voiceToText(MultipartFile file) {
        String result = "";
        try {
            result = simpleMultiModalConversationCall(file);
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return result;
    }
}
