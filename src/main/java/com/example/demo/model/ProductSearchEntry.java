package com.example.demo.model;

import com.amazonaws.services.dynamodbv2.datamodeling.*;
import lombok.Data;

@Data
@DynamoDBTable(tableName = "product_search")
public class ProductSearchEntry {
    // Combined key for efficient searching: keyword or LOCATION:keyword
    @DynamoDBHashKey
    private String searchKey;
    
    // Product ID as range key to ensure uniqueness
    @DynamoDBRangeKey
    private String productId;
    
    // Store information needed for filtering and sorting
    @DynamoDBAttribute
    private String productName;
    
    @DynamoDBAttribute
    private String category;
    
    @DynamoDBAttribute
    private String status;
    
    @DynamoDBAttribute
    private String price;
    
    @DynamoDBAttribute
    private String university;
    
    @DynamoDBAttribute
    private String city;
    
    // URL of primary image for displaying in search results
    @DynamoDBAttribute
    private String imageUrl;
    
    // When the item was posted (for sorting by newest)
    @DynamoDBAttribute
    private String postingDate;
    
    // Optional TTL attribute to auto-expire old entries
    @DynamoDBAttribute
    private Long ttl;
} 