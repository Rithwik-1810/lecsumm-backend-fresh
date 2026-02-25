package com.cvr.cse.lecturesummarizer.models;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Document(collection = "lectures")
public class Lecture {
    @Id
    private String id;
    
    private String userId;
    
    private String title;
    
    private String fileName;
    
    private String fileUrl;
    
    private long fileSize;
    
    private String fileType; // "audio" or "video"
    
    private String duration;
    
    private String status; // "uploading", "processing", "completed", "failed"
    
    private String language = "english";
    
    private boolean extractTasks = true;
    
    private boolean generateSummary = true;
    
    private LocalDateTime createdAt = LocalDateTime.now();
    
    private LocalDateTime updatedAt = LocalDateTime.now();
}