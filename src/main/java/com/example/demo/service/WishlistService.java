package com.example.demo.service;

import com.example.demo.model.Product;
import com.example.demo.model.WishlistItem;
import com.example.demo.repository.WishlistRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class WishlistService {

    private final WishlistRepository wishlistRepository;
    private final ProductService productService;
    private final DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;

    @Autowired
    public WishlistService(WishlistRepository wishlistRepository, ProductService productService) {
        this.wishlistRepository = wishlistRepository;
        this.productService = productService;
    }

    public WishlistItem addToWishlist(String email, String productId) {
        // Check if item is already in wishlist
        WishlistItem existingItem = wishlistRepository.findByEmailAndProductId(email, productId);
        if (existingItem != null) {
            return existingItem; // Item already in wishlist
        }

        // Get product details to cache them
        Product product = productService.getProductById(productId);
        if (product == null) {
            throw new RuntimeException("Product not found with ID: " + productId);
        }

        // Create new wishlist item with cached product details
        WishlistItem wishlistItem = new WishlistItem(
            email,
            productId,
            LocalDateTime.now().format(formatter),
            product.getName(),
            product.getPrimaryImage(),
            product.getPrice()
        );

        return wishlistRepository.save(wishlistItem);
    }

    public void removeFromWishlist(String email, String productId) {
        WishlistItem wishlistItem = wishlistRepository.findByEmailAndProductId(email, productId);
        if (wishlistItem != null) {
            wishlistRepository.delete(wishlistItem);
        }
    }

    public List<WishlistItem> getWishlistByEmail(String email) {
        return wishlistRepository.findByEmail(email);
    }
    
    public WishlistItem getWishlistItem(String email, String productId) {
        return wishlistRepository.findByEmailAndProductId(email, productId);
    }
} 