package com.cvr.cse.lecturesummarizer.models;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Document(collection = "tasks")
public class Task {
    @Id
    private String id;
    
    private String userId;
    
    private String lectureId;
    
    private String title;
    
    private String description;
    
    private String course;
    
    private String priority; // "High", "Medium", "Low"
    
    private String status; // "pending", "in-progress", "completed"
    
    private LocalDate deadline;
    
    private int progress;
    
    private List<Subtask> subtasks;
    
    private LocalDateTime createdAt = LocalDateTime.now();
    
    private LocalDateTime updatedAt = LocalDateTime.now();
    
    @Data
    public static class Subtask {
        private String title;
        private boolean completed = false;
    }
}