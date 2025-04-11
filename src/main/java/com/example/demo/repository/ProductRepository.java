package com.example.demo.repository;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression;
import com.amazonaws.services.dynamodbv2.datamodeling.PaginatedScanList;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.example.demo.model.Product;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
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
    
    /**
     * Search for products by keyword in name and description
     * 
     * @param keyword The search term to look for in product name and description
     * @return List of products matching the search term
     */
    public List<Product> searchByKeyword(final String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return findAll();
        }
        
        final String keywordLower = keyword.toLowerCase();
        
        List<Product> allProducts = findAll();
        return allProducts.stream()
            .filter(product -> 
                (product.getName() != null && product.getName().toLowerCase().contains(keywordLower)) ||
                (product.getDescription() != null && product.getDescription().toLowerCase().contains(keywordLower)))
            .collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * Batch load multiple products by their IDs in a single DynamoDB operation.
     * This is more efficient than loading each product separately.
     *
     * @param productIds List of product IDs to retrieve
     * @return List of Products matching the provided IDs
     */
    public List<Product> batchLoad(List<String> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return new ArrayList<>();
        }
        
        // Create a list of Product objects with just the ID set
        List<Object> keysObjects = new ArrayList<>();
        for (String id : productIds) {
            Product keyObject = new Product();
            keyObject.setId(id);
            keysObjects.add(keyObject);
        }
        
        // Perform the batch load operation
        Map<String, List<Object>> batchLoadResult = dynamoDBMapper.batchLoad(keysObjects);
        
        // Extract and return the products from the result
        List<Object> resultItems = batchLoadResult.get("products");
        if (resultItems == null || resultItems.isEmpty()) {
            return new ArrayList<>();
        }
        
        // Convert to list of Product objects
        List<Product> products = new ArrayList<>();
        for (Object item : resultItems) {
            if (item instanceof Product) {
                products.add((Product) item);
            }
        }
        
        return products;
    }
} 