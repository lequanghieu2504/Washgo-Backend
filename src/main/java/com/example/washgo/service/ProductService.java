// src/main/java/com/example/washgo/service/ProductService.java
package com.example.washgo.service;
import com.example.washgo.dtos.ProductDTO; // Import DTO
import com.example.washgo.dtos.ProductWithPricingDTO;
import com.example.washgo.dtos.ScheduleSummaryDTO;
import com.example.washgo.dtos.subProductDTO;
import com.example.washgo.enums.BookingStatus;
import com.example.washgo.enums.UserRole; // Import UserRole
import com.example.washgo.mapper.ProductMapper; // Import Mapper
import com.example.washgo.model.Product;
import com.example.washgo.model.ProductMaster;
import com.example.washgo.model.UserAccount;
import com.example.washgo.model.UserInformation; // Import UserInformation
import com.example.washgo.repository.ProductMasterRepository;
import com.example.washgo.repository.ProductRepository;
import com.example.washgo.repository.UserAccountRepository;
// Removed unused ScheduleRepository import
import com.example.washgo.repository.UserInformationRepository; // Import User Repo
import org.springframework.http.HttpStatus; // Import HttpStatus
import org.springframework.security.access.AccessDeniedException; // For auth checks
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // Import Transactional
import org.springframework.web.server.ResponseStatusException; // Import ResponseStatusException

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductMasterRepository productMasterRepository;
    // Removed unused ScheduleRepository field
    private final UserInformationRepository userRepository; // Inject User Repo
    private final ProductMapper productMapper; // Inject Mapper
    private final UserAccountRepository userAccountRepository;


    public ProductService(ProductRepository productRepository,
                          ProductMasterRepository productMasterRepository,
                          // Removed unused ScheduleRepository parameter
                          UserInformationRepository userRepository, // Add User Repo
                          ProductMapper productMapper, UserAccountRepository userAccountRepository) { // Add Mapper
        this.productRepository = productRepository;
        this.productMasterRepository = productMasterRepository;
        // Removed unused ScheduleRepository assignment
        this.userRepository = userRepository; // Assign User Repo
        this.productMapper = productMapper; // Assign Mapper
        this.userAccountRepository = userAccountRepository;
    }


    // --- Updated Create Method ---
    @Transactional
    public ProductDTO createProductOffering(Long carwashOwnerId, Long productMasterId, ProductDTO productDetails) {
        Product newProduct = new Product();

        // 1. Tìm carwash owner
        UserInformation owner = userRepository.findById(carwashOwnerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Carwash owner user not found with ID: " + carwashOwnerId));

        // 2. Kiểm tra quyền của người dùng
        if (owner.getAccount().getRole() != UserRole.CARWASH) {
            throw new AccessDeniedException("User " + carwashOwnerId + " is not a carwash owner.");
        }


        // 4. Gán product master nếu có
        ProductMaster master = productMasterRepository.findById(productMasterId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Product master not found with ID: " + productMasterId));
        newProduct.setProductMaster(master);

        // 5. Gán các thuộc tính name, description: ưu tiên lấy từ productDetails, nếu null thì lấy từ master
        newProduct.setName(productDetails.getName() != null ? productDetails.getName() : master.getName());
        newProduct.setDescription(productDetails.getDescription() != null ? productDetails.getDescription() : master.getDescription());

        // 6. Gán các trường còn lại
        newProduct.setActive(productDetails.isActive());
        newProduct.setTimeming(productDetails.getTiming()); // Kiểm tra typo nếu cần: "timing"
        newProduct.setCarwashOwner(owner);

        // 7. Lưu và trả về DTO
        Product savedProduct = productRepository.save(newProduct);
        return productMapper.toProductDTO(savedProduct);
    }


    @Transactional
    public ProductDTO createSubProduct(Long carwashOwnerId,subProductDTO productDetails) {
        Product newProduct = new Product();

        UserInformation owner = userRepository.findById(carwashOwnerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Carwash owner user not found with ID: " + carwashOwnerId));

        // Add check: Ensure the user has the CARWASH role
        if (owner.getAccount().getRole() != UserRole.CARWASH) {
            throw new AccessDeniedException("User " + carwashOwnerId + " is not a carwash owner.");
        }

        Product parentProduct = productRepository.findById(productDetails.getParentId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product parent can not found with ID: " + productDetails.getParentId()));
        // Check if this carwash already offers this ProductMaster
        

        // Copy relevant details (name, description can default from master or be overridden)
        newProduct.setEffectiveFrom(productDetails.getEffectiveFrom());
        newProduct.setEffectiveTo(productDetails.getEffectiveTo());
        newProduct.setActive(productDetails.isActive()); // Default is true in model
        newProduct.setTimeming(productDetails.getTiming());
        newProduct.setCarwashOwner(owner); // Link to the specific carwash owner
        //neu nhu parent product khong co thi khong set
        if(parentProduct != null) {
            newProduct.setParent(parentProduct);        	
        }

        Product savedProduct = productRepository.save(newProduct);
        return productMapper.toProductDTO(savedProduct); // Return DTO
    }
    
    // --- NEW METHOD ---
    @Transactional(readOnly = true)
    public List<ProductDTO> findProductsByCarwash(Long carwashOwnerId) {
        // Optional: Check if user exists first and is CARWASH role
        if (!userRepository.existsById(carwashOwnerId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Carwash owner user not found with ID: " + carwashOwnerId);
        }
        // Fetch products associated with this owner
        List<Product> products = productRepository.findByCarwashOwnerId(carwashOwnerId);
        return productMapper.toProductDTOList(products);
    }


    // --- Update existing methods with Owner Checks ---
    // Keep internal findById returning entity for service logic
    // Find By ID (no owner check needed here, could be anyone viewing)
    public Product findById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Product offering not found with ID: " + id));
    }

    @Transactional(readOnly = true) // Make DTO conversion readOnly
    public ProductDTO findProductDTOById(Long id) {
        return productMapper.toProductDTO(findById(id));
    }

    // Find All (Maybe Admin only? Or needs context?) - Returning DTO List
    @Transactional(readOnly = true) // Make readOnly
    public List<ProductDTO> findAll() {
        return productMapper.toProductDTOList(productRepository.findAll());
    }


    @Transactional
    public ProductDTO update(Long productId, Product updatedDetails, Long requestingUserId) {
        Product existing = findById(productId);

        // Security Check: Ensure the user updating owns this product offering
        if (!existing.getCarwashOwner().getId().equals(requestingUserId)) {
            throw new AccessDeniedException("User " + requestingUserId + " does not have permission to update product offering " + productId);
        }

        // Validation for dates
        if (updatedDetails.getEffectiveFrom() != null && updatedDetails.getEffectiveTo() != null &&
                updatedDetails.getEffectiveFrom().isAfter(updatedDetails.getEffectiveTo())) {
            throw new IllegalArgumentException("Effective From date cannot be after Effective To date.");
        }

        // Update fields (only allow updating certain fields)
        existing.setName(updatedDetails.getName() != null ? updatedDetails.getName() : existing.getName());
        existing.setDescription(updatedDetails.getDescription() != null ? updatedDetails.getDescription() : existing.getDescription());
        existing.setEffectiveFrom(updatedDetails.getEffectiveFrom());
        existing.setEffectiveTo(updatedDetails.getEffectiveTo());
        existing.setActive(updatedDetails.isActive());
        // DO NOT allow changing productMaster or carwashOwner here

        Product savedProduct = productRepository.save(existing);
        return productMapper.toProductDTO(savedProduct); // Return DTO
    }


    @Transactional
    public void delete(Long productId, Long requestingUserId) {
        Product product = findById(productId);

        // Security Check: Ensure the user deleting owns this product offering
        if (!product.getCarwashOwner().getId().equals(requestingUserId)) {
            throw new AccessDeniedException("User " + requestingUserId + " does not have permission to delete product offering " + productId);
        }

        // Prevent deletion if active schedules/bookings exist
        // Note: This check might be complex depending on booking status definition

//        if (!product.getSchedules().isEmpty()) {
//            // More robust check: Check if any associated schedule has *active* bookings
//            boolean hasActiveBookings = product.getSchedules().stream()
//                    .anyMatch(schedule -> schedule.getBookings().stream()
//                            .anyMatch(booking ->  BookingStatus.ACCEPTED == (booking.getStatus()))); // Assuming "ACTIVE" status means cannot delete
//            if (hasActiveBookings) {
//                throw new IllegalStateException("Cannot delete product offering " + productId + " because it has active bookings.");
//            }
//        }

        // Note: Cascade should handle deleting associated Pricing/Schedules if configured correctly in Product entity
        productRepository.delete(product);
    }
    
    public List<ProductWithPricingDTO> getProductsWithPricingByCarwash(Long carwashId) {
        List<Product> products = productRepository.findAllWithPricingByCarwashId(carwashId);

        return products.stream()
                .map(ProductMapper::toProductWithPricingDTO) 
                .collect(Collectors.toList());
    }
	public  List<Product> toProductList(List<Long> products) {
		List<Product> resultListProduct = new ArrayList<Product>();
		
		for(Long p : products) {
			Product newProduct =findById(p);
			resultListProduct.add(newProduct);
		}
		
		return resultListProduct;
	}

//    public List<ProductDTO> findAvailableProducts(String category, LocalDateTime at) {
//        List<Product> products = productRepository.findAvailableProductsByCategoryAndTime(category, at);
//        return products.stream()
//                .map(p -> {
//                    ProductDTO dto = new ProductDTO();
//                    dto.setId(p.getId());
//                    dto.setName(p.getName());
//                    dto.setDescription(p.getDescription());
//                    dto.setEffectiveFrom(p.getEffectiveFrom());
//                    dto.setEffectiveTo(p.getEffectiveTo());
//                    dto.setProductMasterId(p.getProductMaster().getId());
//                    dto.setProductMasterName(p.getProductMaster().getName());
//                    dto.setCarwashOwnerId(p.getCarwashOwner().getId());
//                    dto.setPricing(p.getPricing());
//
//                    // Vì đã JOIN FETCH, p.getSchedules() chỉ có những schedule match
//                    List<ScheduleSummaryDTO> scheds = p.getSchedules().stream()
//                            .map(s -> {
//                                ScheduleSummaryDTO sd = new ScheduleSummaryDTO();
//                                sd.setId(s.getId());
//                                sd.setAvailableFrom(s.getAvailableFrom());
//                                sd.setAvailableTo(s.getAvailableTo());
//                                sd.setCapacity(s.getCapacity());
//                                sd.setActiveBookingCount(s.getBookings().size());
//                                return sd;
//                            })
//                            .collect(Collectors.toList());
//
//                    dto.setSchedules(scheds);
//                    return dto;
//                })
//                .collect(Collectors.toList());
//    }

}
// Removed duplicate package com.example.washgo.service;