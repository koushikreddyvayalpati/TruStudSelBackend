package com.example.demo.model;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import lombok.Data;

import java.util.List;

@Data
@DynamoDBTable(tableName = "userdetails")
public class UserDetails {
    
    @DynamoDBAttribute
    private String name;
    
    @DynamoDBHashKey(attributeName = "email")
    private String email;
    
    @DynamoDBAttribute
    private String mobile;
    
    @DynamoDBAttribute
    private String city;
    
    @DynamoDBAttribute
    private String state;
    
    @DynamoDBAttribute
    private String zipcode;
    
    @DynamoDBAttribute
    private String university;
    
    @DynamoDBAttribute
    private String userphoto;
    
    @DynamoDBAttribute
    private List<String> ProductsCategoriesIntrested;
    
    @DynamoDBAttribute
    private String productsListed;
    
    @DynamoDBAttribute
    private String productssold;
    
    @DynamoDBAttribute
    private List<String> productswishlist;
} 