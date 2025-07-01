// src/main/java/com/example/washgo/service/FeedbackService.java
package com.example.washgo.service;

import com.example.washgo.dtos.FeedbackDTO;
import com.example.washgo.dtos.FeedbackReadDTO; // Import Read DTO
import com.example.washgo.mapper.FeedbackMapper; // Import Mapper
import com.example.washgo.model.*;
import com.example.washgo.repository.FeedbackRepository;

import jakarta.persistence.EntityNotFoundException; // Import
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger; // Import Logger
import org.slf4j.LoggerFactory; // Import LoggerFactory
import org.springframework.security.access.AccessDeniedException; // For authorization
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // Import Transactional

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FeedbackService {

    private static final Logger logger = LoggerFactory.getLogger(FeedbackService.class); // Add Logger

    private final FeedbackRepository feedbackRepository;
    private final BookingService bookingService;
    private final CarwashService carwashService;
    private final FeedbackMapper feedbackMapper; // Inject Mapper
    private final UserRegistrationService userService; // Inject UserService for user checks

    // --- CREATE ---
    @Transactional
    public FeedbackReadDTO createFeedback(FeedbackDTO dto, String authenticatedUsername) { // Accept authenticated username
        // Fetch related entities
        Booking booking = bookingService.findById(dto.getBookingId());
        CarwashProfile carwash = carwashService.findCarwashByUserId(dto.getCarwashID());
        // Fetch the client user who is submitting the feedback
        UserInformation clientUser = userService.findUserEntityById(dto.getClientID());

        // --- Business Logic Checks ---
        // 1. Authorization: Ensure the authenticated user is the client associated with the booking
        if (!booking.getUserInformation().getAccount().getUsername().equals(authenticatedUsername)) {
            throw new AccessDeniedException("User '" + authenticatedUsername + "' is not authorized to leave feedback for this booking.");
        }
        // 2. Ensure Client ID matches booking's client ID
        if (!booking.getUserInformation().getId().equals(dto.getClientID())) {
            throw new IllegalArgumentException("Provided client ID does not match the client ID on the booking.");
        }
        // 3. Prevent Duplicate Feedback: Check if feedback already exists for this booking
        if (feedbackRepository.findByBookingId(dto.getBookingId()).isPresent()) {
            throw new IllegalStateException("Feedback has already been submitted for booking ID: " + dto.getBookingId());
        }
        // 4. Ensure Booking belongs to the correct Carwash
        if (!booking.getCarwash().getId().equals(dto.getCarwashID())) {
            throw new IllegalArgumentException("Booking does not belong to the specified carwash.");
        }
        // 5. Validate Rating (e.g., 1-5)
        if (dto.getRating() < 1 || dto.getRating() > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5.");
        }
        // --- End Business Logic Checks ---


        Feedback feedback = new Feedback();
        feedback.setRating(dto.getRating());
        feedback.setComment(dto.getComment());
        feedback.setBooking(booking);
        feedback.setClientId(dto.getClientID()); // Store client ID
        feedback.setCarwash(carwash);
        // CreatedAt is set automatically

        List<FeedbackImage> images = dto.getImageUrls().stream().map(url -> {
            FeedbackImage img = new FeedbackImage();
            img.setImageUrl(url);
            img.setFeedback(feedback);
            return img;
        }).collect(Collectors.toList());
        feedback.setImages(images);

        Feedback savedFeedback = feedbackRepository.save(feedback);
        logger.info("Feedback created successfully for booking ID: {}", dto.getBookingId());

        // Update carwash average rating
        updateCarwashRating(carwash.getId());

        return feedbackMapper.toFeedbackReadDTO(savedFeedback); // Return Read DTO
    }

    // --- READ ---
    @Transactional(readOnly = true)
    public FeedbackReadDTO getFeedbackById(Long id) {
        Feedback feedback = feedbackRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Feedback not found with ID: " + id));
        // Basic read, no complex auth here, handled in controller
        return feedbackMapper.toFeedbackReadDTO(feedback);
    }

    @Transactional(readOnly = true)
    public List<FeedbackReadDTO> getFeedbacksByCarwash(Long carwashId) {
        // Verify carwash exists (optional, depends on desired behavior)
        // carwashService.findCarwashByUserId(carwashId); // Throws if not found
        List<Feedback> feedbacks = feedbackRepository.findByCarwashId(carwashId);
        return feedbackMapper.toFeedbackReadDTOList(feedbacks);
    }

    @Transactional(readOnly = true)
    public List<FeedbackReadDTO> getFeedbacksByClient(Long clientId, String authenticatedUsername) {
        // Authorization: Ensure the authenticated user matches the requested client ID
        UserInformation requestedClient = userService.findUserEntityById(clientId);
        if (!requestedClient.getAccount().getUsername().equals(authenticatedUsername)) {
            throw new AccessDeniedException("User '" + authenticatedUsername + "' cannot access feedback for client ID " + clientId);
        }
        List<Feedback> feedbacks = feedbackRepository.findByClientId(clientId);
        return feedbackMapper.toFeedbackReadDTOList(feedbacks);
    }

    @Transactional(readOnly = true)
    public List<FeedbackReadDTO> getAllFeedbacks() {
        // Typically restricted to ADMINs, authorization done in controller
        List<Feedback> feedbacks = feedbackRepository.findAll();
        return feedbackMapper.toFeedbackReadDTOList(feedbacks);
    }


    // --- DELETE ---
    @Transactional
    public void deleteFeedback(Long id, String authenticatedUsername, List<String> roles) {
        Feedback feedback = feedbackRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Feedback not found with ID: " + id));

        // --- Authorization Check ---
        boolean isAdmin = roles.contains("ROLE_ADMIN");
        boolean isClientOwner = feedback.getBooking().getUserInformation().getAccount().getUsername().equals(authenticatedUsername);

        if (!isAdmin && !isClientOwner) {
            throw new AccessDeniedException("User '" + authenticatedUsername + "' does not have permission to delete feedback ID " + id);
        }
        // --- End Authorization Check ---

        Long carwashId = feedback.getCarwash().getId();

        feedbackRepository.delete(feedback);
        logger.info("Feedback deleted successfully: ID {}", id);

        // --- Business Logic: Recalculate Rating ---
        updateCarwashRating(carwashId);
        // --- End Recalculate Rating ---
    }


    // --- Helper method to update/recalculate carwash rating ---
    private void updateCarwashRating(Long carwashId) {
        CarwashProfile carwash = carwashService.findCarwashByUserId(carwashId); // Fetch fresh profile
        Double newAverage = feedbackRepository.getAverageRatingByCarwashId(carwashId);
        Integer newCount = feedbackRepository.getCountByCarwashId(carwashId);

        carwash.setAverageRating(newAverage != null ? newAverage : 0.0); // Handle case with no ratings
        carwash.setRatingCount(newCount != null ? newCount : 0);

        carwashService.save(carwash); // Save the updated carwash profile
        logger.info("Updated rating for Carwash ID {}: Average = {}, Count = {}", carwashId, carwash.getAverageRating(), carwash.getRatingCount());
    }

    // --- UPDATE (Example - Often Not Implemented for Feedback) ---
    /*
    @Transactional
    public FeedbackReadDTO updateFeedback(Long id, FeedbackUpdateDTO updateDto, String authenticatedUsername) {
        Feedback feedback = feedbackRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Feedback not found with ID: " + id));

        // Authorization: Only the client who wrote it can update (or admin)
        if (!feedback.getBooking().getClient().getUsername().equals(authenticatedUsername)) {
             // Optional: Add admin check here if needed
             throw new AccessDeniedException("User '" + authenticatedUsername + "' cannot update feedback ID " + id);
        }

        // Business Logic: Maybe disallow updates after a certain time?
        // e.g., if (feedback.getCreatedAt().isBefore(LocalDateTime.now().minusHours(1))) { ... }

        // Update allowed fields (e.g., comment, maybe rating - decide if rating update recalculates average)
        boolean changed = false;
        if (updateDto.getComment() != null && !updateDto.getComment().equals(feedback.getComment())) {
            feedback.setComment(updateDto.getComment());
            changed = true;
        }
        // If rating changes, decide whether to recalculate immediately
        // if (updateDto.getRating() != null && !updateDto.getRating().equals(feedback.getRating())) {
        //     if (updateDto.getRating() < 1 || updateDto.getRating() > 5) {
        //         throw new IllegalArgumentException("Rating must be between 1 and 5.");
        //     }
        //     feedback.setRating(updateDto.getRating());
        //     changed = true;
        //     // updateCarwashRating(feedback.getCarwash().getId()); // Recalculate if desired
        // }

        if (changed) {
            Feedback updatedFeedback = feedbackRepository.save(feedback);
            logger.info("Feedback updated successfully: ID {}", id);
            // If rating wasn't recalculated above, do it here if necessary
            // updateCarwashRating(updatedFeedback.getCarwash().getId());
            return feedbackMapper.toFeedbackReadDTO(updatedFeedback);
        } else {
            return feedbackMapper.toFeedbackReadDTO(feedback); // No changes, return current state
        }
    }
    */

}