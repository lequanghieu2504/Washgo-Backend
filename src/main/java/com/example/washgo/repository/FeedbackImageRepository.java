// src/main/java/com/example/washgo/repositories/FeedbackImageRepository.java
package com.example.washgo.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.washgo.model.FeedbackImage;

public interface FeedbackImageRepository extends JpaRepository<FeedbackImage, Long> {}
