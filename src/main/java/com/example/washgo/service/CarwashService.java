// src/main/java/com/example/washgo/service/CarwashService.java
package com.example.washgo.service;

import com.example.washgo.dtos.CarwashDTO;
import com.example.washgo.enums.UserRole;
import com.example.washgo.mapper.UserMapper;
import com.example.washgo.model.CarwashProfile;
import com.example.washgo.model.UserAccount;
import com.example.washgo.model.UserInformation;
import com.example.washgo.repository.CarwashProfileRepository;
import com.example.washgo.repository.UserAccountRepository;
import com.example.washgo.repository.UserInformationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class CarwashService {


    private final UserInformationRepository userRepository;
    private final CarwashProfileRepository carwashProfileRepository;
    private final UserMapper userMapper;
    private final UserAccountRepository userAccountRepository;
    public CarwashService(UserInformationRepository userRepository,
                          CarwashProfileRepository carwashProfileRepository,
                          UserMapper userMapper, UserAccountRepository userAccountRepository) {
        this.userRepository = userRepository;
        this.carwashProfileRepository = carwashProfileRepository;
        this.userMapper = userMapper;
        this.userAccountRepository = userAccountRepository;
    }

    @Transactional(readOnly = true)
    public List<CarwashDTO> findCarwashes(Optional<String> nameFilter,
                                          Optional<String> locationFilter,
                                          Optional<String> sortBy,
                                          Optional<Sort.Direction> sortDirection) {

        // Start with users who are carwash owners
    	Stream<UserInformation> carwashUsersStream = userRepository.findAll().stream()
    		    .filter(infor -> infor.getAccount().getRole() == UserRole.CARWASH)
    		    .filter(infor -> infor != null)
    		    .filter(infor -> infor.getCarwashProfile() != null);


        // Apply filters (same as before)
        if (nameFilter.isPresent() && !nameFilter.get().isBlank()) {
            String nameLower = nameFilter.get().toLowerCase();
            carwashUsersStream = carwashUsersStream.filter(user ->
                    user.getCarwashProfile().getCarwashName() != null &&
                            user.getCarwashProfile().getCarwashName().toLowerCase().contains(nameLower)
            );
        }

        if (locationFilter.isPresent() && !locationFilter.get().isBlank()) {
            String locationLower = locationFilter.get().toLowerCase();
            carwashUsersStream = carwashUsersStream.filter(user ->
                    user.getCarwashProfile().getLocation() != null &&
                            user.getCarwashProfile().getLocation().toLowerCase().contains(locationLower)
            );
        }

        // Prepare comparator for sorting
        Comparator<CarwashDTO> comparator = null;
        if (sortBy.isPresent()) {
            // --- ADD SORT BY RATING ---
            comparator = switch (sortBy.get().toLowerCase()) {
                case "name" -> Comparator.comparing(CarwashDTO::getCarwashName, Comparator.nullsLast(String::compareToIgnoreCase));
                case "location" -> Comparator.comparing(CarwashDTO::getLocation, Comparator.nullsLast(String::compareToIgnoreCase));
                case "rating" -> Comparator.comparing(CarwashDTO::getAverageRating, Comparator.nullsLast(Double::compareTo)); // Sort by rating, nulls last
                // Add more sortable fields if needed
                default -> null; // No specific sort or invalid field
            };

            if (comparator != null && sortDirection.isPresent() && sortDirection.get() == Sort.Direction.DESC) {
                comparator = comparator.reversed();
                // Special handling for nulls when reversing rating sort (to keep nulls last)
                if (sortBy.get().equalsIgnoreCase("rating")) {
                    comparator = Comparator.comparing(CarwashDTO::getAverageRating, Comparator.nullsLast(Double::compareTo)).reversed();
                }
            }
        }

        // Map to DTOs *before* final sorting if sorting is applied
        List<CarwashDTO> carwashDTOs = carwashUsersStream
                .map(userMapper::toCarwashDTO)
                .collect(Collectors.toList());

        // Apply sorting if comparator was created
        if (comparator != null) {
            carwashDTOs.sort(comparator);
        } else {
            // Default sort if no specific sort requested (e.g., by ID or name)
            carwashDTOs.sort(Comparator.comparing(CarwashDTO::getId, Comparator.nullsLast(Long::compareTo)));
        }

        return carwashDTOs;
    }
//return DTO
    @Transactional(readOnly = true)
    public Optional<CarwashDTO> findCarwashDTOById(Long id) {
        return userRepository.findById(id)
                .filter(user -> user != null 
                        && user.getAccount().getRole() != null 
                        && user.getAccount().getRole() == UserRole.CARWASH 
                        && user.getCarwashProfile() != null)
                .map(userMapper::toCarwashDTO);
    }
    
//return carwashProfile
    @Transactional(readOnly = true)
    public CarwashProfile findCarwashByUserId(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }

        return userRepository.findById(userId)
            .filter(user -> user.getAccount().getRole() == UserRole.CARWASH && user.getCarwashProfile() != null)
            .map(UserInformation::getCarwashProfile)
            .orElseThrow(() -> new RuntimeException("Carwash profile not found or user is not a carwash" + userId));
    }

    public void save(CarwashProfile carwashProfile) {
    	carwashProfileRepository.save(carwashProfile);
	}


//    private static final double DEFAULT_RADIUS_KM = 50.0;
//    public List<Map<String, Object>> filterCarwashes(
//            Double userLat,
//            Double userLon,
//            String category,
//            LocalDateTime requestedTime,
//            Double radiusKm
//    ) {
//        List<Object[]> results = carwashProfileRepository.findAvailableCarwashesByCategoryAndTime(userLat, userLon, category, requestedTime, radiusKm);
//
//        List<Map<String, Object>> nearbyCarwashes = new ArrayList<>(results.size());
//        for (Object[] row : results) {
//            Map<String, Object> carwashData = new HashMap<>();
//            carwashData.put("id",           ((Number) row[0]).longValue());
//            carwashData.put("carwash_name", (String) row[1]);
//            //convert latitude/longitude từ string -> double:
//            try {
//                carwashData.put("latitude",  Double.valueOf((String) row[2]));
//                carwashData.put("longitude", Double.valueOf((String) row[3]));
//            } catch (NumberFormatException ex) {
//                //string không parse thì giữ nguyên string hoặc cho giá trị null
//                carwashData.put("latitude",  row[2]);
//                carwashData.put("longitude", row[3]);
//            }
//            carwashData.put("distance",   (Double) row[4]);
//            nearbyCarwashes.add(carwashData);
//        }
//        return nearbyCarwashes;
//    }

    public List<Map<String, Object>> filterNearbyCarwashes(LocalTime inputTime, double userLat, double userLon, double radiusKm, int limit) {
        List<CarwashProfile> availableCarwashes = carwashProfileRepository.findAvailableCarwashesAtTime(inputTime);

        return availableCarwashes.stream()
                .map(carwash -> {
                    double distance = calculateDistance(
                            userLat, userLon,
                            carwash.getLatitude(), carwash.getLongitude()
                    );
                    if (distance < radiusKm) {
                        Map<String, Object> map = new HashMap<>();
                        map.put("id", carwash.getId());
                        map.put("carwashName", carwash.getCarwashName());
                        map.put("latitude", carwash.getLatitude());
                        map.put("longitude", carwash.getLongitude());
                        map.put("distanceKm", distance);  // Thêm distance vào đây
                        return map;
                    }
                    return null; // filter ra ngoài
                })
                .filter(Objects::nonNull)
                .sorted(Comparator.comparingDouble(map -> (Double) map.get("distanceKm")))
                .limit(limit)
                .collect(Collectors.toList());
    }

    private double calculateDistance(double lat1, double lon1, String lat2Str, String lon2Str) {
        final int R = 6371; // Earth radius in km
        double lat2 = parseDoubleSafe(lat2Str);
        double lon2 = parseDoubleSafe(lon2Str);

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    private double parseDoubleSafe(String value) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException | NullPointerException e) {
            throw new IllegalArgumentException("Invalid coordinate value: " + value);
        }
    }

	public List<CarwashProfile> findActiveCarwashesByUserTime(LocalTime localTime) {
		
		return carwashProfileRepository.findAvailableCarwashesAtTime(localTime);
	}

}