package com.example.demo.model;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import lombok.Data;

import java.util.List;
import java.util.ArrayList;

@Data
@DynamoDBTable(tableName = "products")
public class Product {
    
    @DynamoDBHashKey(attributeName = "id")
    private String id;
    
    @DynamoDBAttribute
    private String name;
    
    @DynamoDBAttribute
    private String category;
    
    @DynamoDBAttribute
    private String description;
    
    @DynamoDBAttribute
    private String price;
    
    @DynamoDBAttribute
    private String email;
    
    @DynamoDBAttribute
    private String city;
    
    @DynamoDBAttribute
    private String zipcode;
    
    @DynamoDBAttribute
    private String university;
    
    @DynamoDBAttribute
    private String primaryImage;
    
    @DynamoDBAttribute
    private List<String> additionalImages;
    
    @DynamoDBAttribute
    private String postingdate;
    
    @DynamoDBAttribute
    private String productage;
    
    @DynamoDBAttribute
    private String sellingtype;
    
    @DynamoDBAttribute
    private String status;
    
    @DynamoDBAttribute
    private List<String> images;

    // Helper method to get all images (primary + additional)
    public List<String> getAllImages() {
        List<String> allImages = new java.util.ArrayList<>();
        if (primaryImage != null) {
            allImages.add(primaryImage);
        }
        if (additionalImages != null) {
            allImages.addAll(additionalImages);
        }
        return allImages;
    }
    
    // Helper method to set all images (first one becomes primary)
    public void setAllImages(List<String> images) {
        if (images == null || images.isEmpty()) {
            this.primaryImage = null;
            this.additionalImages = new java.util.ArrayList<>();
            return;
        }
        
        this.primaryImage = images.get(0);
        
        if (images.size() > 1) {
            this.additionalImages = new java.util.ArrayList<>(images.subList(1, images.size()));
        } else {
            this.additionalImages = new java.util.ArrayList<>();
        }
    }
    
    // Methods to support backward compatibility with ProductService
    public List<String> getImages() {
        if (images == null) {
            images = new ArrayList<>();
        }
        return images;
    }
    
    public void setImages(List<String> images) {
        this.images = images;
    }
} 