package com.example.washgo.model;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonBackReference; // For JSON serialization to handle back references
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*; // For JPA annotations like @Entity, @Id, @OneToOne, etc.
import lombok.Data; // For generating getter, setter, toString, equals, and hashcode methods

@Entity
@Table(name = "carwash_profile") // Specifies the table name for the CarwashProfile entity
@Data
public class CarwashProfile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Automatically generates the primary key for 'id'
    private Long id;

    // One-to-one relationship with UserInformation

    @OneToOne
    @MapsId // Maps this entity's id to the associated UserInformation id
    @JoinColumn(name = "id") // Specifies the foreign key column in this entity referring to 'UserInformation'
    @JsonBackReference("carwash-profile") // Prevents infinite recursion when serializing to JSON
    private UserInformation user;

    @Column(nullable = false) // Ensures that the 'carwashName' is not null in the database
    private String carwashName;

    @Column(nullable = false) // Ensures that the 'location' is not null in the database
    private String location;

    @Column(length = 1000) // Allows a longer description for the carwash
    private String description;


    // --- NEW COORDINATE FIELDS ---
    @Column(nullable = true) // Allow null if geocoding fails or hasn't run
    private String latitude;

    @Column(nullable = true) // Allow null if geocoding fails or hasn't run
    private String longitude;
    // --- END NEW COORDINATE FIELDS ---


    // --- NEW FIELD ---
    @Column(nullable = true) // Allow null if no ratings yet
    private Double averageRating = 5.0;

    // --- Optional: Add a field to count the number of ratings ---
    // @Column(nullable = false)
    private Integer ratingCount = 0;

    @OneToOne(mappedBy = "carwash", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference
    private Schedule schedule;
    
    @OneToMany(mappedBy = "carwash", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Booking> bookings = new ArrayList<>();

}
