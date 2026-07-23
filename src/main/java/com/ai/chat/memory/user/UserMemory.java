package com.ai.chat.memory.user;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class UserMemory {
    private final Map<String,SessionMemory> sessions = new ConcurrentHashMap<>();
    public SessionMemory getSession(String sessionId) {
        return sessions.computeIfAbsent(sessionId,key -> new SessionMemory());
    }
}
