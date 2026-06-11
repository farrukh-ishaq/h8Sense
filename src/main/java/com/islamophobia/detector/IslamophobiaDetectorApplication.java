package com.islamophobia.detector;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Islamophobia Content Detector Application
 *
 * AI-powered system to detect hate speech against Islam and provide
 * counter-arguments based on Quran and Hadith references.
 */
@SpringBootApplication(exclude = {
    // Exclude OpenAI auto-configuration when using mock or no API key
    org.springframework.ai.autoconfigure.openai.OpenAiAutoConfiguration.class,
    org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration.class
})
@EnableScheduling
public class IslamophobiaDetectorApplication {

    public static void main(String[] args) {
        SpringApplication.run(IslamophobiaDetectorApplication.class, args);
    }
}
