package com.cvr.cse.lecturesummarizer.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Configuration
@EnableMongoRepositories(basePackages = "com.cvr.cse.lecturesummarizer.repositories")
@EnableMongoAuditing
public class MongoConfig {
}