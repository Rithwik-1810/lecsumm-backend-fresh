package com.cvr.cse.lecturesummarizer.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class TaskDTO {
    private String title;
    private String description;
    private String course;
    private String priority;
    private LocalDate deadline;
    private int progress;
}