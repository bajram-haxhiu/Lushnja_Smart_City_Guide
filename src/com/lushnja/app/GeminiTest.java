package com.lushnja.app;

import com.lushnja.services.AIChatService;

public class GeminiTest {

    public static void main(String[] args) {
        AIChatService ai = new AIChatService();

        String answer = ai.askGemini(
                "What can I visit in Lushnje?",
                "en"
        );

        System.out.println(answer);
    }
}