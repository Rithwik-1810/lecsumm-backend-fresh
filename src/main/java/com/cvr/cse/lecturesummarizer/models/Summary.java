package com.cvr.cse.lecturesummarizer.models;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Document(collection = "summaries")
public class Summary {
    @Id
    private String id;
    
    private String lectureId;
    
    private String userId;
    
    private String content;
    
    private List<String> keyPoints;
    
    private List<String> topics;
    
    private String transcript;
    
    private int confidence;
    
    private boolean isSaved = false;
    
    private LocalDateTime createdAt = LocalDateTime.now();
}