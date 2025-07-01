// src/main/java/com/example/washgo/mapper/ProductMapper.java
package com.example.washgo.mapper;

import com.example.washgo.dtos.PricingDTO;
import com.example.washgo.dtos.ProductDTO;
import com.example.washgo.dtos.ProductWithPricingDTO;
import com.example.washgo.dtos.ScheduleSummaryDTO;
import com.example.washgo.enums.BookingStatus;
import com.example.washgo.model.Pricing;
import com.example.washgo.model.Product;
import com.example.washgo.model.Schedule;
import com.example.washgo.repository.ProductRepository;
import com.example.washgo.service.ProductService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class ProductMapper {
	
	

	
    // PricingDTO and ScheduleSummaryDTO mapping methods remain the same...

    public PricingDTO toPricingDTO(Pricing pricing) {
        if (pricing == null) return null;
        return new PricingDTO(
                pricing.getId(),
                pricing.getPrice(),
                pricing.getCurrency()
        );
    }

    public List<PricingDTO> toPricingDTOList(List<Pricing> pricings) {
        if (pricings == null) return Collections.emptyList();
        return pricings.stream().map(this::toPricingDTO).collect(Collectors.toList());
    }

    // --- UPDATED Schedule Summary Mapping ---
    public ScheduleSummaryDTO toScheduleSummaryDTO(Schedule schedule) {
        if (schedule == null) return null; //
        // Calculate active bookings count here


        return new ScheduleSummaryDTO(
                schedule.getId(), //
                schedule.getAvailableFrom(), //
                schedule.getAvailableTo(), //
                schedule.getCapacity() // --- ADDED --- // (using updated model)
        );
    }

    public List<ScheduleSummaryDTO> toScheduleSummaryDTOList(List<Schedule> schedules) {
        if (schedules == null) return Collections.emptyList();
        return schedules.stream().map(this::toScheduleSummaryDTO).collect(Collectors.toList());
    }


    // --- Updated Product Mapper ---
    public ProductDTO toProductDTO(Product product) {
        if (product == null) return null;
        return new ProductDTO(
                product.getId(),
                product.getName(), // Use the product's specific name
                product.getDescription(), // Use the product's specific description
                product.isActive(),
                product.getProductMaster() != null ? product.getProductMaster().getId() : null,
                product.getProductMaster() != null ? product.getProductMaster().getName() : null, // Add master name
                product.getCarwashOwner() != null ? product.getCarwashOwner().getId() : null, // Add owner id
               product.getPricing(),
                product.getTimeming()        );
    }

    public List<ProductDTO> toProductDTOList(List<Product> products) {
        if (products == null) return Collections.emptyList();
        return products.stream().map(this::toProductDTO).collect(Collectors.toList());
    }
    
    public static ProductWithPricingDTO toProductWithPricingDTO(Product product) {
        ProductWithPricingDTO dto = new ProductWithPricingDTO();
        dto.setProductId(product.getId());
        dto.setProductName(product.getName());
        dto.setDescription(product.getDescription());

        if (product.getPricing() != null) {
            dto.setPrice(product.getPricing().getPrice());
            dto.setUnit(product.getPricing().getCurrency());
        }

        return dto;
    }


    

}