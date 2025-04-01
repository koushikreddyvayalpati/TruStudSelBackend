package com.example.demo.repository;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperConfig;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.AttributeAction;
import com.amazonaws.services.dynamodbv2.model.AttributeValueUpdate;
import com.amazonaws.services.dynamodbv2.model.UpdateItemRequest;
import com.example.demo.model.UserDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class UserDetailsRepository {

    private final DynamoDBMapper dynamoDBMapper;
    private final AmazonDynamoDB amazonDynamoDB;

    @Autowired
    public UserDetailsRepository(DynamoDBMapper dynamoDBMapper, AmazonDynamoDB amazonDynamoDB) {
        this.dynamoDBMapper = dynamoDBMapper;
        this.amazonDynamoDB = amazonDynamoDB;
    }

    public UserDetails save(UserDetails userDetails) {
        System.out.println("Repository - Saving user: " + userDetails);
        
        // Use CLOBBER as default for full updates or creates
        DynamoDBMapperConfig config = DynamoDBMapperConfig.builder()
                .withSaveBehavior(DynamoDBMapperConfig.SaveBehavior.CLOBBER)
                .build();
        
        dynamoDBMapper.save(userDetails, config);
        System.out.println("Repository - User saved successfully");
        return userDetails;
    }
    
    /**
     * Updates only the non-null fields of the user details.
     * 
     * @param userDetails The user details with only non-null fields to update
     * @return The updated user details
     */
    public UserDetails updateNonNullFields(UserDetails userDetails) {
        System.out.println("Repository - Updating user: " + userDetails);
        
        // Configure DynamoDBMapper to use SaveBehavior.UPDATE
        // This preserves any attributes not specified in the save
        DynamoDBMapperConfig config = DynamoDBMapperConfig.builder()
                .withSaveBehavior(DynamoDBMapperConfig.SaveBehavior.UPDATE)
                .build();
        
        System.out.println("Repository - Using SaveBehavior.UPDATE to preserve existing attributes");
        
        // Save the entity with UPDATE behavior
        dynamoDBMapper.save(userDetails, config);
        System.out.println("Repository - User updated successfully");
        
        // Return the updated user
        return findByEmail(userDetails.getEmail());
    }
    
    /**
     * Updates specific attributes of a user using DynamoDB's direct UpdateItem operation.
     * This ensures other attributes remain unchanged.
     * 
     * @param email The email (primary key) of the user to update
     * @param updates Map of attribute names to their new values
     * @return The updated user details
     */
    public UserDetails updateAttributes(String email, Map<String, Object> updates) {
        System.out.println("Repository - Using direct UpdateItem for user: " + email);
        System.out.println("Repository - Updates: " + updates);
        
        if (updates == null || updates.isEmpty()) {
            System.out.println("Repository - No updates provided, skipping operation");
            return findByEmail(email);
        }
        
        // Prepare key condition
        Map<String, AttributeValue> key = new HashMap<>();
        key.put("email", new AttributeValue().withS(email));
        
        // Build attribute updates
        Map<String, AttributeValueUpdate> attributeUpdates = new HashMap<>();
        
        for (Map.Entry<String, Object> entry : updates.entrySet()) {
            String attributeName = entry.getKey();
            Object value = entry.getValue();
            
            if (value == null) {
                // Skip null values
                continue;
            }
            
            AttributeValue attributeValue;
            if (value instanceof String) {
                attributeValue = new AttributeValue().withS((String) value);
            } else if (value instanceof List) {
                attributeValue = new AttributeValue().withSS((List<String>) value);
            } else if (value instanceof Number) {
                attributeValue = new AttributeValue().withN(value.toString());
            } else {
                // Skip unsupported types
                continue;
            }
            
            attributeUpdates.put(attributeName, new AttributeValueUpdate()
                .withValue(attributeValue)
                .withAction(AttributeAction.PUT));
        }
        
        // Create UpdateItem request
        UpdateItemRequest request = new UpdateItemRequest()
                .withTableName("userdetails")
                .withKey(key)
                .withAttributeUpdates(attributeUpdates);
        
        // Execute the update
        try {
            amazonDynamoDB.updateItem(request);
            System.out.println("Repository - UpdateItem executed successfully");
        } catch (Exception e) {
            System.err.println("Repository - Error during UpdateItem: " + e.getMessage());
            throw new RuntimeException("Failed to update user: " + email, e);
        }
        
        // Return the updated user
        return findByEmail(email);
    }

    public UserDetails findByEmail(String email) {
        return dynamoDBMapper.load(UserDetails.class, email);
    }

    public void delete(UserDetails userDetails) {
        dynamoDBMapper.delete(userDetails);
    }
} 