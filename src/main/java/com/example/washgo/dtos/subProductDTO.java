// src/main/java/com/example/washgo/dtos/ProductDTO.java
package com.example.washgo.dtos;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.hibernate.annotations.Parent;

import com.example.washgo.model.Pricing;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class subProductDTO {

	private Long id;
    private String name; // Specific name for this offering
    private String description; // Specific description
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
    private boolean isActive;
    private String productMasterName; // Name of the template used
    private Long carwashOwnerId; // ID of the car wash offering this
    private Pricing pricing; // Carwash specific pricing
    private List<ScheduleSummaryDTO> schedules; // Carwash specific schedules
    private LocalTime timing;
    private Long parentId;
}	