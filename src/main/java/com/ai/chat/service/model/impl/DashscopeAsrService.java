package com.ai.chat.service.model.impl;

import com.ai.chat.service.model.AsrService;
import org.springframework.stereotype.Service;

import java.io.File;

@Service
public class DashscopeAsrService implements AsrService {

    @Override
    public String speechToText(File file) {
        return "";
    }
}
