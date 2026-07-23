package com.ai.chat.service.model;

import java.io.File;

public interface AsrService {
    String speechToText(File file);
}
