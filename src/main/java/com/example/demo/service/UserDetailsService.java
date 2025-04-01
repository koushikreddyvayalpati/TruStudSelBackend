package com.example.demo.service;

import com.example.demo.model.UserDetails;
import com.example.demo.repository.UserDetailsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class UserDetailsService {

    private final UserDetailsRepository repository;

    @Autowired
    public UserDetailsService(UserDetailsRepository repository) {
        this.repository = repository;
    }

    public UserDetails saveUser(UserDetails userDetails) {
        System.out.println("Service - Saving user: " + userDetails);
        return repository.save(userDetails);
    }

    public UserDetails getUserByEmail(String email) {
        return repository.findByEmail(email);
    }

    public UserDetails updateUser(UserDetails userDetails) {
        System.out.println("Service - Updating user details: " + userDetails);
        
        // Load existing user first
        UserDetails existingUser = repository.findByEmail(userDetails.getEmail());
        if (existingUser == null) {
            throw new RuntimeException("User not found with email: " + userDetails.getEmail());
        }
        
        System.out.println("Service - Existing user before update: " + existingUser);
        
        // Only update the non-null fields from userDetails
        if (userDetails.getName() != null) {
            existingUser.setName(userDetails.getName());
        }
        if (userDetails.getMobile() != null) {
            existingUser.setMobile(userDetails.getMobile());
        }
        if (userDetails.getCity() != null) {
            existingUser.setCity(userDetails.getCity());
        }
        if (userDetails.getState() != null) {
            existingUser.setState(userDetails.getState());
        }
        if (userDetails.getZipcode() != null) {
            existingUser.setZipcode(userDetails.getZipcode());
        }
        if (userDetails.getUniversity() != null) {
            existingUser.setUniversity(userDetails.getUniversity());
        }
        if (userDetails.getUserphoto() != null) {
            existingUser.setUserphoto(userDetails.getUserphoto());
        }
        if (userDetails.getProductsCategoriesIntrested() != null) {
            existingUser.setProductsCategoriesIntrested(userDetails.getProductsCategoriesIntrested());
        }
        if (userDetails.getProductsListed() != null) {
            existingUser.setProductsListed(userDetails.getProductsListed());
        }
        if (userDetails.getProductssold() != null) {
            existingUser.setProductssold(userDetails.getProductssold());
        }
        if (userDetails.getProductswishlist() != null) {
            existingUser.setProductswishlist(userDetails.getProductswishlist());
        }
        
        // Use updateNonNullFields method which uses SaveBehavior.UPDATE
        return repository.updateNonNullFields(existingUser);
    }

    public void deleteUser(String email) {
        UserDetails userDetails = repository.findByEmail(email);
        if (userDetails != null) {
            repository.delete(userDetails);
        }
    }
    
    public UserDetails addToWishlist(String email, String productId) {
        UserDetails userDetails = repository.findByEmail(email);
        if (userDetails == null) {
            throw new RuntimeException("User not found with email: " + email);
        }
        
        List<String> wishlist = userDetails.getProductswishlist();
        if (wishlist == null || !wishlist.contains(productId)) {
            if (wishlist == null) {
                wishlist = new java.util.ArrayList<>();
            }
            wishlist.add(productId);
            userDetails.setProductswishlist(wishlist);
        }
        
        return repository.save(userDetails);
    }
    
    public UserDetails removeFromWishlist(String email, String productId) {
        UserDetails userDetails = repository.findByEmail(email);
        if (userDetails == null) {
            throw new RuntimeException("User not found with email: " + email);
        }
        
        List<String> wishlist = userDetails.getProductswishlist();
        if (wishlist != null && wishlist.contains(productId)) {
            wishlist.remove(productId);
            userDetails.setProductswishlist(wishlist);
        }
        
        return repository.save(userDetails);
    }
    
    public UserDetails updateInterestedCategories(String email, List<String> categories) {
        UserDetails userDetails = repository.findByEmail(email);
        if (userDetails == null) {
            throw new RuntimeException("User not found with email: " + email);
        }
        
        userDetails.setProductsCategoriesIntrested(categories);
        return repository.save(userDetails);
    }
    
    public UserDetails incrementProductsListed(String email) {
        UserDetails userDetails = repository.findByEmail(email);
        if (userDetails == null) {
            throw new RuntimeException("User not found with email: " + email);
        }
        
        int count = Integer.parseInt(userDetails.getProductsListed());
        userDetails.setProductsListed(String.valueOf(count + 1));
        return repository.save(userDetails);
    }
    
    public UserDetails incrementProductsSold(String email) {
        UserDetails userDetails = repository.findByEmail(email);
        if (userDetails == null) {
            throw new RuntimeException("User not found with email: " + email);
        }
        
        int count = Integer.parseInt(userDetails.getProductssold());
        userDetails.setProductssold(String.valueOf(count + 1));
        return repository.save(userDetails);
    }
    
    public UserDetails updateUserPhoto(String email, String photoFileName) {
        UserDetails userDetails = repository.findByEmail(email);
        if (userDetails == null) {
            throw new RuntimeException("User not found with email: " + email);
        }
        
        userDetails.setUserphoto(photoFileName);
        return repository.save(userDetails);
    }

    public void updateProductCountForUser(String email, String operation) {
        UserDetails userDetails = repository.findByEmail(email);
        if (userDetails == null) {
            return; // Skip if user not found
        }
        
        if ("listed".equals(operation)) {
            incrementProductsListed(email);
        } else if ("sold".equals(operation)) {
            incrementProductsSold(email);
        }
    }
    
    public UserDetails patchUser(String email, Map<String, Object> updates) {
        UserDetails userDetails = repository.findByEmail(email);
        if (userDetails == null) {
            throw new RuntimeException("User not found with email: " + email);
        }
        
        // Use direct UpdateItem operation to update specific attributes
        return repository.updateAttributes(email, updates);
    }
    
    // Method to save the user directly with all fields (for use when we've already manually handled merging)
    public UserDetails saveUserDirectly(UserDetails userDetails) {
        System.out.println("Saving user directly with all fields: " + userDetails);
        return repository.save(userDetails);
    }
} 