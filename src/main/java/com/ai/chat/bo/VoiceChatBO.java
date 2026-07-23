package com.ai.chat.bo;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class VoiceChatBO {
    private String sessionId;
    private String userId;
    private MultipartFile file;
}
