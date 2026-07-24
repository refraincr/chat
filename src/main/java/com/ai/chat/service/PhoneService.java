package com.ai.chat.service;

public interface PhoneService {
    String screenCut();
    String press(String x,String y);
    String skip(String x1,String y1,String x2,String y2);
    String home();
    String power();
}
