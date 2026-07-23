package com.ai.chat.common;

import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer;
import ai.djl.huggingface.tokenizers.Encoding;

import java.io.IOException;
import java.nio.file.Paths;

public class Token {
    public static int getTokens(String text) {
        try (HuggingFaceTokenizer tokenizer = HuggingFaceTokenizer.newInstance(
                Paths.get("D:\\86157\\Project\\JAVA\\resource\\tokenizer.json")
        )) {
            Encoding encoding =  tokenizer.encode(text);
            return encoding.getTokens().length;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return 0;
    }
}
