package com.example.washgo.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Feedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Integer rating;

    @Column(length = 1000)
    private String comment;
    
    private LocalDateTime createdAt = LocalDateTime.now();
    private Long clientId;

    // Đổi @OneToMany thành @ManyToOne vì mỗi Feedback chỉ thuộc về một Booking
    @ManyToOne
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    // Quan hệ đúng với FeedbackImage
    @OneToMany(mappedBy = "feedback", cascade = CascadeType.ALL)
    private List<FeedbackImage> images;

    // Quan hệ với CarwashProfile
    @ManyToOne
    @JoinColumn(name = "carwash_id", referencedColumnName = "id")
    private CarwashProfile carwash;
}
