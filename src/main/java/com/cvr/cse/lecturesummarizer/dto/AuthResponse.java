package com.cvr.cse.lecturesummarizer.dto;

import com.cvr.cse.lecturesummarizer.models.User;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuthResponse {
    private String token;
    private User user;
}