package com.ai.chat.service;

import com.ai.chat.service.model.impl.DashscopeAsrService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;

@Service
public class VoiceService {
    private final DashscopeAsrService dashscopeAsrService;

    public VoiceService(DashscopeAsrService dashscopeAsrService) {
        this.dashscopeAsrService = dashscopeAsrService;
    }

    public String recognize(MultipartFile file) {
        return dashscopeAsrService.speechToText((File) file);
    }
}
