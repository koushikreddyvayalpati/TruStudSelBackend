package com.example.demo.repository;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression;
import com.amazonaws.services.dynamodbv2.datamodeling.PaginatedScanList;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.example.demo.model.Product;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class ProductRepository {

    private final DynamoDBMapper dynamoDBMapper;

    @Autowired
    public ProductRepository(DynamoDBMapper dynamoDBMapper) {
        this.dynamoDBMapper = dynamoDBMapper;
    }

    public Product save(Product product) {
        dynamoDBMapper.save(product);
        return product;
    }

    public Product findById(String id) {
        return dynamoDBMapper.load(Product.class, id);
    }

    public List<Product> findByEmail(String email) {
        Map<String, AttributeValue> eav = new HashMap<>();
        eav.put(":email", new AttributeValue().withS(email));
        
        DynamoDBScanExpression scanExpression = new DynamoDBScanExpression()
                .withFilterExpression("email = :email")
                .withExpressionAttributeValues(eav);
        
        return dynamoDBMapper.scan(Product.class, scanExpression);
    }
    
    public List<Product> findByCategory(String category) {
        Map<String, AttributeValue> eav = new HashMap<>();
        eav.put(":category", new AttributeValue().withS(category));
        
        DynamoDBScanExpression scanExpression = new DynamoDBScanExpression()
                .withFilterExpression("category = :category")
                .withExpressionAttributeValues(eav);
        
        return dynamoDBMapper.scan(Product.class, scanExpression);
    }
    
    public List<Product> findByUniversity(String university) {
        Map<String, AttributeValue> eav = new HashMap<>();
        eav.put(":university", new AttributeValue().withS(university));
        
        DynamoDBScanExpression scanExpression = new DynamoDBScanExpression()
                .withFilterExpression("university = :university")
                .withExpressionAttributeValues(eav);
        
        return dynamoDBMapper.scan(Product.class, scanExpression);
    }
    
    public List<Product> findByCity(String city) {
        Map<String, AttributeValue> eav = new HashMap<>();
        eav.put(":city", new AttributeValue().withS(city));
        
        DynamoDBScanExpression scanExpression = new DynamoDBScanExpression()
                .withFilterExpression("city = :city")
                .withExpressionAttributeValues(eav);
        
        return dynamoDBMapper.scan(Product.class, scanExpression);
    }
    
    public List<Product> findAll() {
        return dynamoDBMapper.scan(Product.class, new DynamoDBScanExpression());
    }

    public void delete(Product product) {
        dynamoDBMapper.delete(product);
    }
} 