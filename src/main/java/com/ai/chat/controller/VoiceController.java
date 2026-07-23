package com.ai.chat.controller;

import com.ai.chat.service.TextToVoiceService;
import com.ai.chat.service.VoiceToTextService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.Resource;

@RestController
@RequestMapping("voice")
@RequiredArgsConstructor
public class VoiceController {
    private final VoiceToTextService voiceToTextService;
    private final TextToVoiceService textToVoiceService;

    @PostMapping(path = "test",consumes= MediaType.MULTIPART_FORM_DATA_VALUE)
    public String input(MultipartFile file){
        return voiceToTextService.voiceToText(file);
    }

    @GetMapping("play")
    public ResponseEntity<Resource> output(String text) {
        // 模拟使用
        byte[] audioBytes = textToVoiceService.streamAudioDataToSpeaker(text);

        ByteArrayResource resource = new ByteArrayResource(audioBytes);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("audio/wav"))
                // inline 表示在浏览器内直接播放；若换成 attachment 则强制下载
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"output.mp3\"")
                .contentLength(audioBytes.length)
                .body(resource);
    }
}
