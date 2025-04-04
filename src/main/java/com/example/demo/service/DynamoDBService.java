package com.example.demo.service;

import com.example.demo.model.Product;
import com.example.demo.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class DynamoDBService {

    private final ProductRepository productRepository;

    @Autowired
    public DynamoDBService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public Product saveProduct(Product product) {
        return productRepository.save(product);
    }

    public Product getProductById(String id) {
        return productRepository.findById(id);
    }

    public List<Product> getProductsByEmail(String email) {
        return productRepository.findByEmail(email);
    }

    public List<Product> getProductsByCategory(String category) {
        return productRepository.findByCategory(category);
    }

    public List<Product> getProductsByUniversity(String university) {
        return productRepository.findByUniversity(university);
    }

    public List<Product> getProductsByCity(String city) {
        return productRepository.findByCity(city);
    }

    public Product updateProduct(Product product) {
        return productRepository.save(product);
    }

    public void deleteProduct(String id) {
        Product product = productRepository.findById(id);
        if (product != null) {
            productRepository.delete(product);
        }
    }

    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }
} 