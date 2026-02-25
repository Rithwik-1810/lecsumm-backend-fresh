package com.cvr.cse.lecturesummarizer.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class LectureDTO {
    private String title;
    private String language;
    private boolean extractTasks;
    private boolean generateSummary;
    private MultipartFile file;
}