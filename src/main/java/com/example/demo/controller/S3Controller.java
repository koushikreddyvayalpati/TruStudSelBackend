package com.example.demo.controller;

import com.example.demo.service.S3Service;
import com.example.demo.service.UserDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/files")
public class S3Controller {

    private final S3Service s3Service;
    private final UserDetailsService userDetailsService;

    @Autowired
    public S3Controller(S3Service s3Service, UserDetailsService userDetailsService) {
        this.s3Service = s3Service;
        this.userDetailsService = userDetailsService;
    }

    @PostMapping("/upload")
    public ResponseEntity<Map<String, String>> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            String fileName = s3Service.uploadFile(file);
            Map<String, String> response = new HashMap<>();
            response.put("fileName", fileName);
            response.put("message", "File uploaded successfully");
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (IOException e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    @PostMapping("/upload-multiple")
    public ResponseEntity<Map<String, Object>> uploadMultipleFiles(@RequestParam("files") MultipartFile[] files) {
        try {
            List<String> fileNames = new ArrayList<>();
            for (MultipartFile file : files) {
                String fileName = s3Service.uploadFile(file);
                fileNames.add(fileName);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("fileNames", fileNames);
            response.put("message", "Files uploaded successfully");
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (IOException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("error", e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    @PostMapping("/product-images")
    public ResponseEntity<Map<String, Object>> uploadProductImages(@RequestParam("images") MultipartFile[] files) {
        try {
            if (files.length > 5) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Maximum 5 images allowed per product");
                return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
            }
            
            List<String> fileNames = new ArrayList<>();
            String primaryImage = null;
            List<String> additionalImages = new ArrayList<>();
            
            // Process each file
            for (int i = 0; i < files.length; i++) {
                String fileName = s3Service.uploadFile(files[i]);
                fileNames.add(fileName);
                
                // First image is primary, rest are additional
                if (i == 0) {
                    primaryImage = fileName;
                } else {
                    additionalImages.add(fileName);
                }
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("fileNames", fileNames);
            response.put("primaryImage", primaryImage);
            response.put("additionalImages", additionalImages);
            response.put("message", "Product images uploaded successfully");
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (IOException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("error", e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    @PostMapping("/user/{email}/photo")
    public ResponseEntity<Map<String, String>> uploadUserPhoto(
            @PathVariable String email, 
            @RequestParam("file") MultipartFile file) {
        try {
            // Upload file to S3
            String fileName = s3Service.uploadFile(file);
            
            // Update user profile with photo URL
            userDetailsService.updateUserPhoto(email, fileName);
            
            Map<String, String> response = new HashMap<>();
            response.put("fileName", fileName);
            response.put("message", "User photo uploaded successfully");
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (IOException e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (RuntimeException e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/download/{fileName}")
    public ResponseEntity<ByteArrayResource> downloadFile(@PathVariable String fileName) {
        try {
            byte[] data = s3Service.downloadFile(fileName);
            ByteArrayResource resource = new ByteArrayResource(data);
            
            return ResponseEntity
                    .ok()
                    .contentLength(data.length)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                    .body(resource);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/delete/{fileName}")
    public ResponseEntity<Map<String, String>> deleteFile(@PathVariable String fileName) {
        s3Service.deleteFile(fileName);
        Map<String, String> response = new HashMap<>();
        response.put("message", "File deleted successfully");
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
} 