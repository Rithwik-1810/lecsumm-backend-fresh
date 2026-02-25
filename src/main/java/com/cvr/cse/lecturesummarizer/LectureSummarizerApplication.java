package com.cvr.cse.lecturesummarizer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class LectureSummarizerApplication {
    public static void main(String[] args) {
        SpringApplication.run(LectureSummarizerApplication.class, args);
        System.out.println("========================================");
        System.out.println("🚀 AI Lecture Summarizer Backend Started");
        System.out.println("📍 http://localhost:8080");
        System.out.println("========================================");
    }
}