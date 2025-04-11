package com.example.demo.repository;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.example.demo.model.WishlistItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class WishlistRepository {

    private final DynamoDBMapper dynamoDBMapper;

    @Autowired
    public WishlistRepository(DynamoDBMapper dynamoDBMapper) {
        this.dynamoDBMapper = dynamoDBMapper;
    }

    public WishlistItem save(WishlistItem wishlistItem) {
        dynamoDBMapper.save(wishlistItem);
        return wishlistItem;
    }

    public void delete(WishlistItem wishlistItem) {
        dynamoDBMapper.delete(wishlistItem);
    }

    public WishlistItem findByEmailAndProductId(String email, String productId) {
        // Load directly by composite key (email + productId)
        WishlistItem key = new WishlistItem();
        key.setEmail(email);
        key.setProductId(productId);
        
        return dynamoDBMapper.load(WishlistItem.class, email, productId);
    }

    public List<WishlistItem> findByEmail(String email) {
        Map<String, AttributeValue> eav = new HashMap<>();
        eav.put(":email", new AttributeValue().withS(email));

        DynamoDBQueryExpression<WishlistItem> queryExpression = new DynamoDBQueryExpression<WishlistItem>()
                .withKeyConditionExpression("email = :email")
                .withExpressionAttributeValues(eav);

        return dynamoDBMapper.query(WishlistItem.class, queryExpression);
    }
    
    // Find most wishlisted products (using GSI, once you add it)
    public List<WishlistItem> findMostWishlistedProducts(int limit) {
        // Implement after adding the GSI
        // This would query the productId-count GSI
        throw new UnsupportedOperationException("Not implemented yet - requires GSI");
    }
} 