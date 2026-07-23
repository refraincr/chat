package com.ai.chat.controller;

import com.ai.chat.bo.ChatBO;
import com.ai.chat.bo.VoiceChatBO;
import com.ai.chat.service.ChatService;
import com.ai.chat.service.VoiceToVoiceService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("test")
@RequiredArgsConstructor
public class TestController {
    private final ChatService chatService;
    private final VoiceToVoiceService voiceToVoiceService;

    @GetMapping(path = "chat",produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chat(@RequestBody ChatBO chatBO){
        SseEmitter emitter = new SseEmitter(0L); // 流式返回(0L:永不超时)
        CompletableFuture.runAsync(() -> chatService.chat(chatBO,emitter));

        return emitter;
    }

    @PostMapping(path = "vtv",consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Resource> vtv(VoiceChatBO voiceChatBO){
        byte[] audioBytes = voiceToVoiceService.voiceToVoice(voiceChatBO);

        ByteArrayResource resource = new ByteArrayResource(audioBytes);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("audio/wav"))
                // inline 表示在浏览器内直接播放；若换成 attachment 则强制下载
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"output.mp3\"")
                .contentLength(audioBytes.length)
                .body(resource);
    }
}
