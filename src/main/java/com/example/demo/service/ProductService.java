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

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final UserDetailsService userDetailsService;
    private final DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;

    @Autowired
    public ProductService(ProductRepository productRepository, UserDetailsService userDetailsService) {
        this.productRepository = productRepository;
        this.userDetailsService = userDetailsService;
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
        
        List<String> images = product.getImages();
        if (images == null) {
            images = new ArrayList<>();
            product.setImages(images);
        }
        
        if (!images.contains(imageUrl)) {
            images.add(imageUrl);
        }
        
        return productRepository.save(product);
    }
    
    public Product removeProductImage(String id, String imageUrl) {
        Product product = productRepository.findById(id);
        if (product == null) {
            throw new RuntimeException("Product not found with ID: " + id);
        }
        
        List<String> images = product.getImages();
        if (images != null) {
            images.remove(imageUrl);
        }
        
        return productRepository.save(product);
    }

    public void deleteProduct(String id) {
        Product product = productRepository.findById(id);
        if (product != null) {
            productRepository.delete(product);
        }
    }
} 