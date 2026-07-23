package com.ai.chat.controller;

import com.ai.chat.bo.ChatBO;
import com.ai.chat.service.ChatService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("test")
public class TestController {
    private final ChatService chatService;

    public TestController(ChatService chatService) {
        this.chatService = chatService;
    }

    @GetMapping(path = "chat",produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chat(@RequestBody ChatBO chatBO){
        SseEmitter emitter = new SseEmitter(0L); // 流式返回(0L:永不超时)
        CompletableFuture.runAsync(() -> chatService.chat(chatBO,emitter));

        return emitter;
    }


}
