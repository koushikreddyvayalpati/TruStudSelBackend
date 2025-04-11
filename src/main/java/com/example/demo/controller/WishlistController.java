package com.example.demo.controller;

import com.example.demo.model.Product;
import com.example.demo.model.WishlistItem;
import com.example.demo.service.ProductService;
import com.example.demo.service.WishlistService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/wishlist")
public class WishlistController {

    private final WishlistService wishlistService;
    private final ProductService productService;

    @Autowired
    public WishlistController(WishlistService wishlistService, ProductService productService) {
        this.wishlistService = wishlistService;
        this.productService = productService;
    }

    @GetMapping("/{email}")
    public ResponseEntity<List<WishlistItem>> getWishlist(@PathVariable String email) {
        try {
            List<WishlistItem> wishlistItems = wishlistService.getWishlistByEmail(email);
            return new ResponseEntity<>(wishlistItems, HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/{email}/products")
    public ResponseEntity<List<Product>> getWishlistProducts(@PathVariable String email) {
        try {
            // Get wishlist items
            List<WishlistItem> wishlistItems = wishlistService.getWishlistByEmail(email);
            
            // Extract product IDs
            List<String> productIds = wishlistItems.stream()
                .map(WishlistItem::getProductId)
                .collect(Collectors.toList());
                
            // Load full product details using our efficient batch loader
            List<Product> products = productService.getWishlistProducts(productIds);
            
            return new ResponseEntity<>(products, HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/{email}")
    public ResponseEntity<WishlistItem> addToWishlist(
            @PathVariable String email, 
            @RequestParam String productId) {
        try {
            WishlistItem wishlistItem = wishlistService.addToWishlist(email, productId);
            return new ResponseEntity<>(wishlistItem, HttpStatus.CREATED);
        } catch (RuntimeException e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @DeleteMapping("/{email}/{productId}")
    public ResponseEntity<Void> removeFromWishlist(
            @PathVariable String email, 
            @PathVariable String productId) {
        try {
            wishlistService.removeFromWishlist(email, productId);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    @GetMapping("/{email}/check/{productId}")
    public ResponseEntity<Boolean> isInWishlist(
            @PathVariable String email, 
            @PathVariable String productId) {
        try {
            WishlistItem wishlistItem = wishlistService.getWishlistItem(email, productId);
            return new ResponseEntity<>(wishlistItem != null, HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
} 