package com.example.demo.controller;

import com.example.demo.model.UserDetails;
import com.example.demo.service.UserDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/field-updates")
public class FieldUpdatesController {

    private final UserDetailsService userDetailsService;

    @Autowired
    public FieldUpdatesController(UserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    @PutMapping("/users/{email}/name")
    public ResponseEntity<UserDetails> updateUserName(
            @PathVariable String email,
            @RequestParam String value) {
        try {
            UserDetails userDetails = userDetailsService.getUserByEmail(email);
            if (userDetails == null) {
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
            
            // Create update map
            Map<String, Object> updates = new HashMap<>();
            updates.put("name", value);
            
            // Use patch method which uses direct UpdateItem
            UserDetails updatedUser = userDetailsService.patchUser(email, updates);
            return new ResponseEntity<>(updatedUser, HttpStatus.OK);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
    
    @PutMapping("/users/{email}/university")
    public ResponseEntity<UserDetails> updateUniversity(
            @PathVariable String email,
            @RequestParam String value) {
        try {
            UserDetails userDetails = userDetailsService.getUserByEmail(email);
            if (userDetails == null) {
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
            
            // Create update map
            Map<String, Object> updates = new HashMap<>();
            updates.put("university", value);
            
            // Use patch method which uses direct UpdateItem
            UserDetails updatedUser = userDetailsService.patchUser(email, updates);
            return new ResponseEntity<>(updatedUser, HttpStatus.OK);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
    
    @PutMapping("/users/{email}/userphoto")
    public ResponseEntity<UserDetails> updatePhoto(
            @PathVariable String email,
            @RequestParam String value) {
        try {
            UserDetails userDetails = userDetailsService.getUserByEmail(email);
            if (userDetails == null) {
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
            
            // Create update map
            Map<String, Object> updates = new HashMap<>();
            updates.put("userphoto", value);
            
            // Use patch method which uses direct UpdateItem
            UserDetails updatedUser = userDetailsService.patchUser(email, updates);
            return new ResponseEntity<>(updatedUser, HttpStatus.OK);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
} 