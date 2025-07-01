// src/main/java/com/example/washgo/dtos/FeedbackReadDTO.java
package com.example.washgo.dtos;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackReadDTO {
    private Long id;
    private Integer rating;
    private String comment;
    private LocalDateTime createdAt;
    private Long bookingId;
    private Long carwashId; // ID of the carwash the feedback is for
    private String carwashName; // Name of the carwash
    private Long clientId; // ID of the client who gave feedback
    private String clientUsername; // Username of the client
    private List<String> imageUrls; // Keep image URLs
}