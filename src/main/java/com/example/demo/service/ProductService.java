package com.example.demo.service;

import com.example.demo.model.Product;
import com.example.demo.repository.ProductRepository;
import com.example.demo.repository.UserDetailsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.Map;
import java.util.Comparator;
import java.util.HashMap;
import java.util.stream.Collectors;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final UserDetailsService userDetailsService;
    private final DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;
    private final DynamoDBService dynamoDBService;

    @Autowired
    public ProductService(ProductRepository productRepository, UserDetailsService userDetailsService, DynamoDBService dynamoDBService) {
        this.productRepository = productRepository;
        this.userDetailsService = userDetailsService;
        this.dynamoDBService = dynamoDBService;
    }

    public Product createProduct(Product product) {
        // Generate unique ID if not provided
        if (product.getId() == null || product.getId().isEmpty()) {
            product.setId(UUID.randomUUID().toString());
        }
        
        // Add posting date as current timestamp
        if (product.getPostingdate() == null || product.getPostingdate().isEmpty()) {
            product.setPostingdate(LocalDateTime.now().format(formatter));
        }
        
        // Set default status if not provided
        if (product.getStatus() == null || product.getStatus().isEmpty()) {
            product.setStatus("available");
        }
        
        // Initialize images list if null
        if (product.getImages() == null) {
            product.setImages(new ArrayList<>());
        }
        
        // Save the product
        Product savedProduct = productRepository.save(product);
        
        // Increment product count for user
        try {
            userDetailsService.updateProductCountForUser(product.getEmail(), "listed");
        } catch (Exception e) {
            // Just log error, don't fail the product creation
            System.err.println("Failed to update user statistics: " + e.getMessage());
        }
        
        return savedProduct;
    }

    public Product getProductById(String id) {
        return productRepository.findById(id);
    }
    
    public List<Product> getProductsByEmail(String email) {
        return productRepository.findByEmail(email);
    }
    
    public List<Product> getProductsByCategory(String category) {
        return productRepository.findByCategory(category);
    }
    
    public List<Product> getProductsByUniversity(String university) {
        return productRepository.findByUniversity(university);
    }
    
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    public Product updateProduct(Product product) {
        // Check if product exists
        Product existingProduct = productRepository.findById(product.getId());
        if (existingProduct == null) {
            throw new RuntimeException("Product not found with ID: " + product.getId());
        }
        
        // Preserve posting date if not explicitly changed
        if (product.getPostingdate() == null || product.getPostingdate().isEmpty()) {
            product.setPostingdate(existingProduct.getPostingdate());
        }
        
        return productRepository.save(product);
    }
    
    public Product updateProductStatus(String id, String status) {
        Product product = productRepository.findById(id);
        if (product == null) {
            throw new RuntimeException("Product not found with ID: " + id);
        }
        
        // If status is changed to "sold", update user's sold products count
        if ("sold".equals(status) && !"sold".equals(product.getStatus())) {
            try {
                userDetailsService.updateProductCountForUser(product.getEmail(), "sold");
            } catch (Exception e) {
                System.err.println("Failed to update user statistics: " + e.getMessage());
            }
        }
        
        product.setStatus(status);
        return productRepository.save(product);
    }
    
    public Product addProductImage(String id, String imageUrl) {
        Product product = productRepository.findById(id);
        if (product == null) {
            throw new RuntimeException("Product not found with ID: " + id);
        }
        
        if (product.getImages() == null) {
            product.setImages(new ArrayList<>());
        } else {
            List<String> imagesList = new ArrayList<>(product.getImages());
            imagesList.add(imageUrl);
            product.setImages(imagesList);
        }
        
        return productRepository.save(product);
    }
    
    public Product removeProductImage(String id, String imageUrl) {
        Product product = productRepository.findById(id);
        if (product == null) {
            throw new RuntimeException("Product not found with ID: " + id);
        }
        
        if (product.getImages() == null) {
            return product;
        }
        
        List<String> images = product.getImages();
        images.remove(imageUrl);
        
        return productRepository.save(product);
    }

    public void deleteProduct(String id) {
        Product product = productRepository.findById(id);
        if (product != null) {
            productRepository.delete(product);
        }
    }

    // New methods for university-based filtering
    public Map<String, Object> getProductsByUniversityWithFilters(
            String university,
            String category,
            String sortBy,
            String condition,
            String sellingType,
            int page,
            int size) {
        List<Product> products = productRepository.findByUniversity(university);
        
        // Apply filters
        if (category != null && !category.isEmpty()) {
            products = products.stream()
                .filter(p -> category.equals(p.getCategory()))
                .collect(Collectors.toList());
        }
        
        if (condition != null && !condition.isEmpty()) {
            products = products.stream()
                .filter(p -> condition.equals(p.getProductage()))
                .collect(Collectors.toList());
        }
        
        if (sellingType != null && !sellingType.isEmpty()) {
            products = products.stream()
                .filter(p -> sellingType.equals(p.getSellingtype()))
                .collect(Collectors.toList());
        }
        
        // Apply sorting
        if (sortBy != null && !sortBy.isEmpty()) {
            switch (sortBy) {
                case "price_low_high":
                    products.sort((p1, p2) -> {
                        double price1 = Double.parseDouble(p1.getPrice());
                        double price2 = Double.parseDouble(p2.getPrice());
                        return Double.compare(price1, price2);
                    });
                    break;
                case "price_high_low":
                    products.sort((p1, p2) -> {
                        double price1 = Double.parseDouble(p1.getPrice());
                        double price2 = Double.parseDouble(p2.getPrice());
                        return Double.compare(price2, price1);
                    });
                    break;
                case "newest":
                    products.sort(Comparator.comparing(Product::getPostingdate).reversed());
                    break;
                case "popularity":
                    // Implement popularity-based sorting if needed
                    break;
            }
        }
        
        // Apply pagination
        int totalItems = products.size();
        int startIndex = page * size;
        int endIndex = Math.min(startIndex + size, totalItems);
        List<Product> paginatedProducts = products.subList(startIndex, endIndex);
        
        Map<String, Object> response = new HashMap<>();
        response.put("products", paginatedProducts);
        response.put("totalItems", totalItems);
        response.put("currentPage", page);
        response.put("totalPages", (int) Math.ceil((double) totalItems / size));
        
        return response;
    }

    public Map<String, Object> getProductsByCityWithFilters(
            String city,
            String university,
            String category,
            String sortBy,
            String condition,
            String sellingType,
            int page,
            int size) {
        List<Product> products = productRepository.findByCity(city);
        
        // Apply university filter if specified
        if (university != null && !university.isEmpty()) {
            products = products.stream()
                .filter(p -> university.equals(p.getUniversity()))
                .collect(Collectors.toList());
        }
        
        // Apply other filters
        if (category != null && !category.isEmpty()) {
            products = products.stream()
                .filter(p -> category.equals(p.getCategory()))
                .collect(Collectors.toList());
        }
        
        if (condition != null && !condition.isEmpty()) {
            products = products.stream()
                .filter(p -> condition.equals(p.getProductage()))
                .collect(Collectors.toList());
        }
        
        if (sellingType != null && !sellingType.isEmpty()) {
            products = products.stream()
                .filter(p -> sellingType.equals(p.getSellingtype()))
                .collect(Collectors.toList());
        }
        
        // Apply sorting
        if (sortBy != null && !sortBy.isEmpty()) {
            switch (sortBy) {
                case "price_low_high":
                    products.sort((p1, p2) -> {
                        double price1 = Double.parseDouble(p1.getPrice());
                        double price2 = Double.parseDouble(p2.getPrice());
                        return Double.compare(price1, price2);
                    });
                    break;
                case "price_high_low":
                    products.sort((p1, p2) -> {
                        double price1 = Double.parseDouble(p1.getPrice());
                        double price2 = Double.parseDouble(p2.getPrice());
                        return Double.compare(price2, price1);
                    });
                    break;
                case "newest":
                    products.sort(Comparator.comparing(Product::getPostingdate).reversed());
                    break;
                case "popularity":
                    // Implement popularity-based sorting if needed
                    break;
            }
        }
        
        // Apply pagination
        int totalItems = products.size();
        int startIndex = page * size;
        int endIndex = Math.min(startIndex + size, totalItems);
        List<Product> paginatedProducts = products.subList(startIndex, endIndex);
        
        Map<String, Object> response = new HashMap<>();
        response.put("products", paginatedProducts);
        response.put("totalItems", totalItems);
        response.put("currentPage", page);
        response.put("totalPages", (int) Math.ceil((double) totalItems / size));
        
        return response;
    }

    public List<Map<String, String>> getNearbyUniversities(String zipcode, int radiusInMiles) {
        // This would typically integrate with a geocoding service or database
        // For now, return a mock list of nearby universities
        List<Map<String, String>> universities = new ArrayList<>();
        Map<String, String> university1 = new HashMap<>();
        university1.put("name", "San Jose State University");
        university1.put("distance", "0.5 miles");
        universities.add(university1);
        
        Map<String, String> university2 = new HashMap<>();
        university2.put("name", "Santa Clara University");
        university2.put("distance", "2.3 miles");
        universities.add(university2);
        
        return universities;
    }

    public List<Product> getFeaturedProducts(String university, String city, int limit) {
        // Get all products for the university and city
        List<Product> products = productRepository.findByUniversity(university);
        
        // Filter by city
        products = products.stream()
            .filter(p -> city.equals(p.getCity()))
            .collect(Collectors.toList());
            
        // Sort by number of views/interests (this would be implemented when we add view tracking)
        // For now, we'll sort by posting date to show recent products
        products.sort(Comparator.comparing(Product::getPostingdate).reversed());
        
        // Limit the number of products returned
        return products.stream()
            .limit(limit)
            .collect(Collectors.toList());
    }

    public List<Product> getNewArrivals(String university, int limit) {
        List<Product> products = productRepository.findByUniversity(university);
        return products.stream()
            .sorted(Comparator.comparing(Product::getPostingdate).reversed())
            .limit(limit)
            .collect(Collectors.toList());
    }
} 