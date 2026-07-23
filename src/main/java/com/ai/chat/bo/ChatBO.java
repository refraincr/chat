package com.ai.chat.bo;

import lombok.Data;

@Data
public class ChatBO {
    private String sessionId;
    private String userId;
    private String message;
}
