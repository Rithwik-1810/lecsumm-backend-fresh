package com.cvr.cse.lecturesummarizer.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.nio.file.Files;
import java.util.List;

@Service
public class AIService {

    @Value("${ai.service.url}")
    private String aiServiceUrl;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Data
    public static class SummaryDTO {
        private String content;
        private List<String> keyPoints;
        private List<String> topics;
        private int confidence;
    }

    @Data
    public static class TaskDTO {
        private String title;
        private String description;
        private String priority;
        private String deadline;
    }

    @Data
    public static class AIResponse {
        private String transcript;
        private SummaryDTO summary;
        private List<TaskDTO> tasks;
    }

    public AIResponse processLecture(String filePath, String language, 
                                     boolean extractTasks, boolean generateSummary) throws Exception {
        
        String url = aiServiceUrl + "/process";
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        
        File file = new File(filePath);
        
        HttpEntity<byte[]> fileEntity = new HttpEntity<>(Files.readAllBytes(file.toPath()), createFileHeaders(file.getName()));
        body.add("file", fileEntity);
        body.add("language", language);
        body.add("extractTasks", String.valueOf(extractTasks));
        body.add("generateSummary", String.valueOf(generateSummary));

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(url, requestEntity, String.class);
        
        return objectMapper.readValue(response.getBody(), AIResponse.class);
    }

    private HttpHeaders createFileHeaders(String filename) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("file", filename);
        return headers;
    }
}