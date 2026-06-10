package com.islamophobia.detector;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Islamophobia Content Detector Application
 * 
 * AI-powered system to detect hate speech against Islam and provide
 * counter-arguments based on Quran and Hadith references.
 */
@SpringBootApplication
@EnableScheduling
public class IslamophobiaDetectorApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(IslamophobiaDetectorApplication.class, args);
    }
}
