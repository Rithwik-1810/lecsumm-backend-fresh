package com.cvr.cse.lecturesummarizer.models;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@Document(collection = "users")
public class User {
    @Id
    private String id;
    
    private String name;
    
    @Indexed(unique = true)
    private String email;
    
    private String password;
    
    private String role = "USER";
    
    private UserStats stats = new UserStats();
    
    private List<String> achievements = new ArrayList<>();
    
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Data
    public static class UserStats {
        private int totalSummaries = 0;
        private int totalTasks = 0;
        private int completedTasks = 0;
        private double hoursSaved = 0.0;   // <-- must be double
    }
}