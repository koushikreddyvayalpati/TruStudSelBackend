package com.example.demo.controller;

import com.example.demo.model.UserDetails;
import com.example.demo.service.UserDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/users")
public class UserDetailsController {

    private final UserDetailsService userDetailsService;

    @Autowired
    public UserDetailsController(UserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    // Create a new user for profile filling screen
    @PostMapping
    public ResponseEntity<UserDetails> createUser(@RequestBody UserDetails userDetails) {
        UserDetails createdUser = userDetailsService.saveUser(userDetails);
        return new ResponseEntity<>(createdUser, HttpStatus.CREATED);
    }

    @GetMapping("/{email}")
    public ResponseEntity<?> getUserByEmail(@PathVariable String email) {
        // university, name, email, productssold, userphoto
        UserDetails userDetails = userDetailsService.getUserByEmail(email);
        if (userDetails != null) {
            Map<String, Object> limitedUserDetails = new HashMap<>();
            limitedUserDetails.put("university", userDetails.getUniversity());
            limitedUserDetails.put("name", userDetails.getName());
            limitedUserDetails.put("email", userDetails.getEmail());
            limitedUserDetails.put("productssold", userDetails.getProductssold());
            limitedUserDetails.put("userphoto", userDetails.getUserphoto());
            limitedUserDetails.put("mobile", userDetails.getMobile());
            limitedUserDetails.put("city", userDetails.getCity());
            limitedUserDetails.put("zipcode", userDetails.getZipcode());
            
            return new ResponseEntity<>(limitedUserDetails, HttpStatus.OK);
        }
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    // update user details for profilepic, name 
    @PutMapping("/{email}")
    public ResponseEntity<UserDetails> updateUser(@PathVariable String email, @RequestBody UserDetails userDetails) {
        // We only set the email, all other fields from the request body 
        // will be applied conditionally in the service layer
        userDetails.setEmail(email);
        try {
            // Use updateUser which now uses DynamoDBMapper with SaveBehavior.UPDATE
            UserDetails updatedUser = userDetailsService.updateUser(userDetails);
            return new ResponseEntity<>(updatedUser, HttpStatus.OK);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
    
    // Add a specific endpoint just for updating name
    @PutMapping("/{email}/update-name")
    public ResponseEntity<UserDetails> updateUserName(
            @PathVariable String email, 
            @RequestParam String name) {
        try {
            Map<String, Object> updates = new HashMap<>();
            updates.put("name", name);
            
            // Use direct UpdateItem operation
            UserDetails updatedUser = userDetailsService.patchUser(email, updates);
            return new ResponseEntity<>(updatedUser, HttpStatus.OK);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    // delete user details based on email
    @DeleteMapping("/{email}")
    public ResponseEntity<Void> deleteUser(@PathVariable String email) {
        userDetailsService.deleteUser(email);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
    
    // Get complete user details for testing
    @GetMapping("/{email}/full")
    public ResponseEntity<UserDetails> getFullUserByEmail(@PathVariable String email) {
        UserDetails userDetails = userDetailsService.getUserByEmail(email);
        if (userDetails != null) {
            return new ResponseEntity<>(userDetails, HttpStatus.OK);
        }
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
    
    // Partial update (PATCH) endpoint for user details
    @PatchMapping("/{email}")
    public ResponseEntity<UserDetails> patchUser(@PathVariable String email, @RequestBody Map<String, Object> updates) {
        try {
            UserDetails updatedUser = userDetailsService.patchUser(email, updates);
            return new ResponseEntity<>(updatedUser, HttpStatus.OK);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
    
    // add products to wishlist
    @PutMapping("/{email}/wishlist/add")
    public ResponseEntity<UserDetails> addToWishlist(@PathVariable String email, @RequestParam String productId) {
        try {
            UserDetails updatedUser = userDetailsService.addToWishlist(email, productId);
            return new ResponseEntity<>(updatedUser, HttpStatus.OK);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
    
    // remove products from wishlist
    @PutMapping("/{email}/wishlist/remove")
    public ResponseEntity<UserDetails> removeFromWishlist(@PathVariable String email, @RequestParam String productId) {
        try {
            UserDetails updatedUser = userDetailsService.removeFromWishlist(email, productId);
            return new ResponseEntity<>(updatedUser, HttpStatus.OK);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
    
    // @PutMapping("/{email}/categories")
    // public ResponseEntity<UserDetails> updateInterestedCategories(@PathVariable String email, @RequestBody List<String> categories) {
    //     try {
    //         UserDetails updatedUser = userDetailsService.updateInterestedCategories(email, categories);
    //         return new ResponseEntity<>(updatedUser, HttpStatus.OK);
    //     } catch (RuntimeException e) {
    //         return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    //     }
    // }
    
    @PutMapping("/{email}/products/increment-listed")
    public ResponseEntity<UserDetails> incrementProductsListed(@PathVariable String email) {
        try {
            UserDetails updatedUser = userDetailsService.incrementProductsListed(email);
            return new ResponseEntity<>(updatedUser, HttpStatus.OK);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
    
    @PutMapping("/{email}/products/increment-sold")
    public ResponseEntity<UserDetails> incrementProductsSold(@PathVariable String email) {
        try {
            UserDetails updatedUser = userDetailsService.incrementProductsSold(email);
            return new ResponseEntity<>(updatedUser, HttpStatus.OK);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @PutMapping("/{email}/field")
    public ResponseEntity<UserDetails> updateUserField(
            @PathVariable String email, 
            @RequestParam String field,
            @RequestParam String value) {
        try {
            UserDetails existingUser = userDetailsService.getUserByEmail(email);
            if (existingUser == null) {
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
            
            // Create a Map for the field updates
            Map<String, Object> updates = new HashMap<>();
            updates.put(field, value);
            
            UserDetails updatedUser = userDetailsService.patchUser(email, updates);
            return new ResponseEntity<>(updatedUser, HttpStatus.OK);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
} 