package com.example.demo.model;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBRangeKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import lombok.Data;

@Data
@DynamoDBTable(tableName = "userwishlist")
public class WishlistItem {
    
    @DynamoDBHashKey(attributeName = "email")
    private String email;
    
    @DynamoDBRangeKey(attributeName = "productId")
    private String productId;
    
    @DynamoDBAttribute
    private String addedAt;
    
    @DynamoDBAttribute
    private String productName;
    
    @DynamoDBAttribute
    private String productImage;
    
    @DynamoDBAttribute
    private String productPrice;
    
    // Default constructor required by DynamoDB mapper
    public WishlistItem() {
    }
    
    // Convenience constructor for creating new wishlist items
    public WishlistItem(String email, String productId, String addedAt, String productName, 
                        String productImage, String productPrice) {
        this.email = email;
        this.productId = productId;
        this.addedAt = addedAt;
        this.productName = productName;
        this.productImage = productImage;
        this.productPrice = productPrice;
    }
} 