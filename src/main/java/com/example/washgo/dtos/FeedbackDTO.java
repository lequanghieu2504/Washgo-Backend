// src/main/java/com/example/washgo/dtos/FeedbackDTO.java
package com.example.washgo.dtos;

import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackDTO {
    private Long bookingId;
    private Integer rating;
    private String comment;
    private List<String> imageUrls;
    private Long carwashID;
    private Long clientID;
}
