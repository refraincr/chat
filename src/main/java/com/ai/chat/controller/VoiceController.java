package com.ai.chat.controller;

import com.ai.chat.service.VoiceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("voice")
@RequiredArgsConstructor
public class VoiceController {
    private final VoiceService voiceService;

    @PostMapping(path = "asr",consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public String  asr(@RequestParam("file") MultipartFile file){
        return voiceService.recognize(file);
    }
}
