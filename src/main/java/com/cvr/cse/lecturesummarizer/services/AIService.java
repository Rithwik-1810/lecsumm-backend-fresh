package com.cvr.cse.lecturesummarizer.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger logger = LoggerFactory.getLogger(AIService.class);

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
        private double durationSeconds;   // <-- NEW FIELD
    }

    public AIResponse processLecture(String filePath, String language,
                                     boolean extractTasks, boolean generateSummary) throws Exception {

        String url = aiServiceUrl + "/process";
        logger.info("Calling AI service at: {}", url);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

        File file = new File(filePath);
        logger.info("Sending file: {} (size: {} bytes)", file.getName(), file.length());

        HttpEntity<byte[]> fileEntity = new HttpEntity<>(Files.readAllBytes(file.toPath()), createFileHeaders(file.getName()));
        body.add("file", fileEntity);
        body.add("language", language);
        body.add("extractTasks", String.valueOf(extractTasks));
        body.add("generateSummary", String.valueOf(generateSummary));

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, requestEntity, String.class);
            logger.info("AI service responded with status: {}", response.getStatusCode());
            logger.info("Raw response body: {}", response.getBody());
            return objectMapper.readValue(response.getBody(), AIResponse.class);
        } catch (Exception e) {
            logger.error("Error calling AI service: {}", e.getMessage(), e);
            throw e;
        }
    }

    private HttpHeaders createFileHeaders(String filename) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("file", filename);
        return headers;
    }
}