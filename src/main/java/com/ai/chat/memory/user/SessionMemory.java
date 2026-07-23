package com.ai.chat.memory.user;

import lombok.Getter;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class SessionMemory {
    @Getter
    private final List<ChatMessage> history = new CopyOnWriteArrayList<>();
    public void add(ChatMessage chatMessage) {history.add(chatMessage);}
}
