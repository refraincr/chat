package com.ai.chat.service.impl;

import com.ai.chat.service.PhoneService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

@Slf4j
@Service
public class PhoneServiceImpl implements PhoneService {
    private static String exec(String cmd) {
        Process process = null;
        StringBuilder result = new StringBuilder();
        BufferedReader reader = null;

        try {
            process = Runtime.getRuntime().exec(cmd);

            reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            reader.lines().forEach(l->result.append(l).append("\n"));

            // 处理错误(拼到结果后面)
            if (process.waitFor() != 0) {
                reader = new  BufferedReader(new InputStreamReader(process.getErrorStream()));
                reader.lines().forEach(l-> result.append(l).append("\n"));
            }
        } catch (Exception e) {
            log.error(e.getMessage());
        } finally {
            if (process != null) process.destroy();
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    log.error(e.getMessage());
                }
            }
        }

        return result.toString();
    }
    @Override
    public String screenCut() {
        // 图片还存在手机里
        return exec("adb shell screencap -p /sdcard/screenshot.png");
    }

    @Override
    public String press(String x, String y) {
        return exec(String.format("adb shell input tap %s %s", x, y));
    }

    @Override
    public String skip(String x1,String y1,String x2,String y2) {
        // 从 (x1,y1) 到 (x2,y2)
        return exec(String.format("adb shell input swipe %s %s %s %s", x1, y1, x2, y2));
    }

    @Override
    public String home() {
        return exec("adb shell input keyevent 3");
    }

    @Override
    public String power() {
        return exec("adb shell input keyevent 26");
    }
}
