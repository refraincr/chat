package com.ai.chat.memory.user;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ChatMemoryManager {
    public static int maxCurrentToken = 8192;

    private final Map<String,UserMemory> users =
            new ConcurrentHashMap<>();

    public SessionMemory getSession(String userId,String sessionId) {
        UserMemory userMemory = getUser(userId);

        return userMemory.getSession(sessionId);
    }

    public UserMemory getUser(String userId) {
        return users.computeIfAbsent(userId,key -> new UserMemory());
    }
}
