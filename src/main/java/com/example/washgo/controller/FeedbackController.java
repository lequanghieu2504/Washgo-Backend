// src/main/java/com/example/washgo/controller/FeedbackController.java
package com.example.washgo.controller;

import com.example.washgo.dtos.FeedbackDTO;
import com.example.washgo.dtos.FeedbackReadDTO; // Import Read DTO
import com.example.washgo.service.FeedbackService;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid; // For potential future validation on DTOs
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus; // Import HttpStatus
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize; // For authorization
import org.springframework.security.core.Authentication; // To get user details
import org.springframework.security.core.GrantedAuthority; // To get roles
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException; // For exceptions

import java.util.List; // Import List
import java.util.stream.Collectors; // Import Collectors

@RestController
@RequestMapping("/api/feedbacks")
@RequiredArgsConstructor
public class FeedbackController {

    private final FeedbackService feedbackService;

    // --- CREATE ---
    @PostMapping("/add")
    @PreAuthorize("hasRole('CLIENT')") // Only clients can create feedback
    public ResponseEntity<FeedbackReadDTO> createFeedback(@Valid @RequestBody FeedbackDTO feedbackDTO, Authentication authentication) {
        try {
            // Pass the authenticated username to the service for authorization checks
            FeedbackReadDTO createdFeedback = feedbackService.createFeedback(feedbackDTO, authentication.getName());
            return ResponseEntity.status(HttpStatus.CREATED).body(createdFeedback);
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        } catch (EntityNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        } catch (AccessDeniedException e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage(), e);
        } catch (Exception e) { // Catch unexpected errors
            // Log the exception e
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", e);
        }
    }

    // --- READ ---

    /**
     * Get a specific feedback by its ID.
     * Accessible by ADMIN or the CLIENT who wrote it or the CARWASH owner who received it.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @feedbackService.getFeedbackById(#id).clientId == T(Long).parseLong(authentication.principal.claims['userId']) or @feedbackService.getFeedbackById(#id).carwashId == T(Long).parseLong(authentication.principal.claims['userId'])") // Simplified - Needs careful testing! Assumes user ID is in JWT claim 'userId'
    // Alternative/Better: Implement checks in service layer based on Authentication object
    public ResponseEntity<FeedbackReadDTO> getFeedbackById(@PathVariable Long id, Authentication authentication) {
        try {
            // TODO: Add fine-grained access control in the service if @PreAuthorize is too complex
            return ResponseEntity.ok(feedbackService.getFeedbackById(id));
        } catch (EntityNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        }
    }


    /**
     * Get all feedbacks for a specific carwash.
     * Publicly accessible or requires authentication? Let's assume authenticated for now.
     */
    @GetMapping("/carwash/{carwashId}")
    @PreAuthorize("isAuthenticated()") // Allow any authenticated user to see carwash feedbacks
    public ResponseEntity<List<FeedbackReadDTO>> getFeedbacksForCarwash(@PathVariable Long carwashId) {
        // Optional: Add check if carwashId exists
        return ResponseEntity.ok(feedbackService.getFeedbacksByCarwash(carwashId));
    }

    /**
     * Get all feedbacks submitted by a specific client.
     * Only accessible by the client themselves or an ADMIN.
     */
    @GetMapping("/client/{clientId}")
    @PreAuthorize("isAuthenticated()") // Needs claim check
    // Alternative: Check in service: feedbackService.getFeedbacksByClient(clientId, authentication.getName());
    public ResponseEntity<List<FeedbackReadDTO>> getFeedbacksByClient(@PathVariable Long clientId, Authentication authentication) {
        try {
            // Pass username for service-layer authorization check
            return ResponseEntity.ok(feedbackService.getFeedbacksByClient(clientId, authentication.getName()));
        } catch (AccessDeniedException e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage(), e);
        } catch (EntityNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Client user not found", e);
        }
    }

    /**
     * Get all feedbacks in the system.
     * Only accessible by ADMIN.
     */
    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<FeedbackReadDTO>> getAllFeedbacks() {
        return ResponseEntity.ok(feedbackService.getAllFeedbacks());
    }


    // --- DELETE ---
    /**
     * Deletes a feedback by its ID.
     * Accessible only by ADMIN or the CLIENT who wrote the feedback.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @feedbackService.getFeedbackById(#id).clientId == T(Long).parseLong(authentication.principal.claims['userId'])") // Needs claim check
    // Alternative: Pass roles and username to service for check
    public ResponseEntity<Void> deleteFeedback(@PathVariable Long id, Authentication authentication) {
        try {
            // Extract roles for service layer check
            List<String> roles = authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toList());
            feedbackService.deleteFeedback(id, authentication.getName(), roles);
            return ResponseEntity.noContent().build(); // HTTP 204
        } catch (EntityNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        } catch (AccessDeniedException e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage(), e);
        } catch (Exception e) {
            // Log exception e
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error deleting feedback", e);
        }
    }


    // --- UPDATE (Example - If needed) ---
    /*
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @feedbackService.getFeedbackById(#id).clientId == T(Long).parseLong(authentication.principal.claims['userId'])") // Needs claim check
    public ResponseEntity<FeedbackReadDTO> updateFeedback(@PathVariable Long id, @Valid @RequestBody FeedbackUpdateDTO updateDTO, Authentication authentication) {
         try {
             FeedbackReadDTO updatedFeedback = feedbackService.updateFeedback(id, updateDTO, authentication.getName());
             return ResponseEntity.ok(updatedFeedback);
         } catch (EntityNotFoundException e) {
             throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
         } catch (AccessDeniedException e) {
             throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage(), e);
         } catch (IllegalArgumentException | IllegalStateException e) {
             throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
         } catch (Exception e) {
             // Log exception e
             throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error updating feedback", e);
         }
    }
    */
}