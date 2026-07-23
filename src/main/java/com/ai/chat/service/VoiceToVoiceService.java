package com.ai.chat.service;

import com.ai.chat.bo.ChatBO;
import com.ai.chat.bo.VoiceChatBO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class VoiceToVoiceService {
    private final TextToTextNoStreamService textToTextNoStreamService;
    private final VoiceToTextService voiceToTextService;
    private final TextToVoiceService textToVoiceService;

    public byte[] voiceToVoice(VoiceChatBO voiceChatBO) {
        String vttResult = voiceToTextService.voiceToText(voiceChatBO.getFile());

        ChatBO chatBO = new ChatBO();
        chatBO.setUserId(voiceChatBO.getUserId());
        chatBO.setSessionId(voiceChatBO.getSessionId());
        chatBO.setMessage(vttResult);
        String tttResult = textToTextNoStreamService.textToText(chatBO);

        return textToVoiceService.streamAudioDataToSpeaker(tttResult);
    }
}
