package com.ai.chat.service;

import com.alibaba.dashscope.audio.ttsv2.SpeechSynthesisParam;
import com.alibaba.dashscope.audio.ttsv2.SpeechSynthesizer;
import org.springframework.stereotype.Service;

import java.nio.ByteBuffer;

@Service
public class TextToVoiceService {
    private static String model = "cosyvoice-v3-plus";
    private static String voice = "longanyang";

    public byte[] streamAudioDataToSpeaker(String input) {
        SpeechSynthesisParam param =
                SpeechSynthesisParam.builder()
                        .apiKey(System.getenv("ALIYUN_API_KEY"))
                        .model(model)
                        .voice(voice)
                        .build();

        SpeechSynthesizer synthesizer = new SpeechSynthesizer(param, null);
        ByteBuffer audio = null;
        try {
            audio = synthesizer.call(input);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            synthesizer.getDuplexApi().close(1000, "bye");
        }

        byte[] audioBytes = new byte[audio.remaining()];
        audio.get(audioBytes);

        return audioBytes;
    }

}

