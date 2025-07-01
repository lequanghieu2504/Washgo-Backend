// src/main/java/com/example/washgo/mapper/FeedbackMapper.java
package com.example.washgo.mapper;

import com.example.washgo.dtos.FeedbackDTO;
import com.example.washgo.dtos.FeedbackReadDTO;
import com.example.washgo.model.Feedback;
import com.example.washgo.model.FeedbackImage;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class FeedbackMapper {

    public FeedbackReadDTO toFeedbackReadDTO(Feedback feedback) {
        if (feedback == null) {
            return null;
        }
        return new FeedbackReadDTO(
                feedback.getId(),
                feedback.getRating(),
                feedback.getComment(),
                feedback.getCreatedAt(),
                feedback.getBooking() != null ? feedback.getBooking().getId() : null,
                feedback.getCarwash() != null ? feedback.getCarwash().getId() : null,
                feedback.getCarwash() != null ? feedback.getCarwash().getCarwashName() : null, // Get carwash name
                feedback.getClientId(), // Keep client ID
                feedback.getBooking() != null && feedback.getBooking().getUserInformation() != null ? feedback.getBooking().getUserInformation().getAccount().getUsername() : null, // Get client username safely
                feedback.getImages() != null ? feedback.getImages().stream().map(FeedbackImage::getImageUrl).collect(Collectors.toList()) : Collections.emptyList()
        );
    }

    public List<FeedbackReadDTO> toFeedbackReadDTOList(List<Feedback> feedbacks) {
        if (feedbacks == null) {
            return Collections.emptyList();
        }
        return feedbacks.stream()
                .map(this::toFeedbackReadDTO)
                .collect(Collectors.toList());
    }

    // You might need a method to map from FeedbackDTO (create) to Feedback entity if needed,
    // but the current service handles creation directly.
}