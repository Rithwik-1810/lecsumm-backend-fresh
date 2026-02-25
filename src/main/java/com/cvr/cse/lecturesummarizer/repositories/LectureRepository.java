package com.cvr.cse.lecturesummarizer.repositories;

import com.cvr.cse.lecturesummarizer.models.Lecture;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LectureRepository extends MongoRepository<Lecture, String> {
    List<Lecture> findByUserIdOrderByCreatedAtDesc(String userId);
    void deleteByUserId(String userId);
}