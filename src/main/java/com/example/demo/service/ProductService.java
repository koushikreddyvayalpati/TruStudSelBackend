package com.example.demo.service;

import com.example.demo.model.Product;
import com.example.demo.repository.ProductRepository;
import com.example.demo.repository.UserDetailsRepository;
import com.example.demo.service.ProductSearchService;
import com.example.demo.config.FeatureToggleConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.Map;
import java.util.Comparator;
import java.util.HashMap;
import java.util.stream.Collectors;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final UserDetailsService userDetailsService;
    private final DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;
    private final DynamoDBService dynamoDBService;
    private final ProductSearchService searchService;
    private final FeatureToggleConfig featureToggleConfig;

    @Autowired
    public ProductService(ProductRepository productRepository, UserDetailsService userDetailsService, DynamoDBService dynamoDBService, ProductSearchService searchService, FeatureToggleConfig featureToggleConfig) {
        this.productRepository = productRepository;
        this.userDetailsService = userDetailsService;
        this.dynamoDBService = dynamoDBService;
        this.searchService = searchService;
        this.featureToggleConfig = featureToggleConfig;
    }

    public Product createProduct(Product product) {
        // Generate unique ID if not provided
        if (product.getId() == null || product.getId().isEmpty()) {
            product.setId(UUID.randomUUID().toString());
        }
        
        // Add posting date as current timestamp
        if (product.getPostingdate() == null || product.getPostingdate().isEmpty()) {
            product.setPostingdate(LocalDateTime.now().format(formatter));
        }
        
        // Set default status if not provided
        if (product.getStatus() == null || product.getStatus().isEmpty()) {
            product.setStatus("available");
        }
        
        // Initialize images list if null
        if (product.getImages() == null) {
            product.setImages(new ArrayList<>());
        }
        
        // Save the product
        Product savedProduct = productRepository.save(product);
        
        // Index the product for search
        searchService.indexProduct(savedProduct);
        
        // Increment product count for user
        try {
            userDetailsService.updateProductCountForUser(product.getEmail(), "listed");
        } catch (Exception e) {
            // Just log error, don't fail the product creation
            System.err.println("Failed to update user statistics: " + e.getMessage());
        }
        
        return savedProduct;
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
    
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    public Product updateProduct(String id, Product product) {
        // Check if product exists
        Product existingProduct = productRepository.findById(id);
        if (existingProduct == null) {
            throw new RuntimeException("Product not found with ID: " + id);
        }
        
        // Preserve posting date if not explicitly changed
        if (product.getPostingdate() == null || product.getPostingdate().isEmpty()) {
            product.setPostingdate(existingProduct.getPostingdate());
        }
        
        // Save the updated product
        Product updatedProduct = productRepository.save(product);
        
        // Re-index the product for search
        searchService.indexProduct(updatedProduct);
        
        return updatedProduct;
    }
    
    public Product updateProductStatus(String id, String status) {
        Product product = productRepository.findById(id);
        if (product == null) {
            throw new RuntimeException("Product not found with ID: " + id);
        }
        
        // If status is changed to "sold", update user's sold products count
        if ("sold".equals(status) && !"sold".equals(product.getStatus())) {
            try {
                userDetailsService.updateProductCountForUser(product.getEmail(), "sold");
            } catch (Exception e) {
                System.err.println("Failed to update user statistics: " + e.getMessage());
            }
        }
        
        product.setStatus(status);
        return productRepository.save(product);
    }
    
    public Product addProductImage(String id, String imageUrl) {
        Product product = productRepository.findById(id);
        if (product == null) {
            throw new RuntimeException("Product not found with ID: " + id);
        }
        
        if (product.getImages() == null) {
            product.setImages(new ArrayList<>());
        } else {
            List<String> imagesList = new ArrayList<>(product.getImages());
            imagesList.add(imageUrl);
            product.setImages(imagesList);
        }
        
        return productRepository.save(product);
    }
    
    public Product removeProductImage(String id, String imageUrl) {
        Product product = productRepository.findById(id);
        if (product == null) {
            throw new RuntimeException("Product not found with ID: " + id);
        }
        
        if (product.getImages() == null) {
            return product;
        }
        
        List<String> images = product.getImages();
        images.remove(imageUrl);
        
        return productRepository.save(product);
    }

    public void deleteProduct(String id) {
        Product product = getProductById(id);
        
        if (product != null) {
            // Delete from main table
            productRepository.delete(product);
            
            // Delete from search index
            searchService.deleteEntriesForProduct(id);
        }
    }

    /**
     * Get products by university with filters
     * This method can now use either the original implementation or the new search table
     * based on feature toggle configuration
     */
    public Map<String, Object> getProductsByUniversityWithFilters(
            String university, String category, String sortBy, String condition, 
            String sellingType, int page, int size) {
        
        // Add logging to debug search table feature
        System.out.println("getProductsByUniversityWithFilters called for university: " + university);
        
        // Use the correct method to check feature toggle
        boolean useSearchTable = featureToggleConfig.useSearchTableForUniversity();
        System.out.println("Feature toggle for search table enabled: " + useSearchTable);
        
        if (useSearchTable) {
            System.out.println("Using search table implementation for university: " + university);
            return getProductsByUniversityWithFiltersUsingSearchTable(
                university, category, sortBy, condition, sellingType, page, size);
        } else {
            System.out.println("Using repository implementation for university: " + university);
            return getProductsByUniversityWithFiltersUsingRepository(
                university, category, sortBy, condition, sellingType, page, size);
        }
    }
    
    /**
     * New implementation using search table for university filtering
     * This maintains the same response format as the original method
     */
    private Map<String, Object> getProductsByUniversityWithFiltersUsingSearchTable(
            String university, String category, String sortBy, String condition, 
            String sellingType, int page, int size) {
        
        System.out.println("getProductsByUniversityWithFiltersUsingSearchTable called for university: " + university);
        
        try {
            // Use the search service's cost-efficient method to get products by university
            // This avoids full table scans in DynamoDB
            Map<String, Object> searchResults = searchService.getProductsByUniversity(university, category, page, size);
            
            // If we got results from the search table, apply any additional filters
            if (searchResults.containsKey("products") && !((List<?>)searchResults.get("products")).isEmpty()) {
                List<Product> products = (List<Product>) searchResults.get("products");
                int originalCount = products.size();
                System.out.println("Successfully used search table for university: " + university);
                System.out.println("Found " + originalCount + " products using search table");
                
                // Apply condition filter if provided
                if (condition != null && !condition.isEmpty()) {
                    products = products.stream()
                        .filter(p -> condition.equals(p.getProductage()))
                        .collect(Collectors.toList());
                    System.out.println("After condition filtering: " + products.size() + " of " + originalCount + " products");
                }
                
                // Apply selling type filter if provided
                if (sellingType != null && !sellingType.isEmpty()) {
                    products = products.stream()
                        .filter(p -> sellingType.equals(p.getSellingtype()))
                        .collect(Collectors.toList());
                    System.out.println("After selling type filtering: " + products.size() + " of " + originalCount + " products");
                }
                
                // Apply custom sorting if needed (search service already sorts by date)
                if (sortBy != null && !sortBy.isEmpty() && !sortBy.equals("newest")) {
                    applySorting(products, sortBy);
                }
                
                // Update the results with the filtered products
                searchResults.put("products", products);
                searchResults.put("totalItems", products.size());
                searchResults.put("totalPages", (int) Math.ceil((double) products.size() / size));
                
                return searchResults;
            }
            
            // If we couldn't get results from the search table (no entries found),
            // fall back to repository method
            System.out.println("No results from search table, falling back to repository for university: " + university);
            return getProductsByUniversityWithFiltersUsingRepository(
                university, category, sortBy, condition, sellingType, page, size);
        } catch (Exception e) {
            // Log the error
            System.err.println("Error in search table implementation: " + e.getMessage());
            e.printStackTrace();
            
            // Fall back to the original repository implementation
            System.out.println("Falling back to repository implementation for university: " + university);
            return getProductsByUniversityWithFiltersUsingRepository(
                university, category, sortBy, condition, sellingType, page, size);
        }
    }
    
    /**
     * Original repository-based implementation for university filters, extracted for fallback
     */
    public Map<String, Object> getProductsByUniversityWithFiltersUsingRepository(
            String university, String category, String sortBy, String condition, 
            String sellingType, int page, int size) {
        
        // Original implementation
        List<Product> products = productRepository.findByUniversity(university);
        System.out.println("Found " + products.size() + " products for university " + university + " directly from repository");
        
        // Convert to ArrayList to avoid DynamoDB List implementation issues
        products = new ArrayList<>(products);
        
        // Apply filters
        if (category != null && !category.isEmpty()) {
            products = products.stream()
                .filter(p -> category.equals(p.getCategory()))
                .collect(Collectors.toList());
        }
        
        if (condition != null && !condition.isEmpty()) {
            products = products.stream()
                .filter(p -> condition.equals(p.getProductage()))
                .collect(Collectors.toList());
        }
        
        if (sellingType != null && !sellingType.isEmpty()) {
            products = products.stream()
                .filter(p -> sellingType.equals(p.getSellingtype()))
                .collect(Collectors.toList());
        }
        
        // Apply sorting
        applySorting(products, sortBy);
        
        // Apply pagination
        int totalItems = products.size();
        int startIndex = page * size;
        int endIndex = Math.min(startIndex + size, totalItems);
        
        List<Product> paginatedProducts = new ArrayList<>();
        if (startIndex < endIndex) {
            for (int i = startIndex; i < endIndex; i++) {
                paginatedProducts.add(products.get(i));
            }
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("products", paginatedProducts);
        response.put("totalItems", totalItems);
        response.put("currentPage", page);
        response.put("totalPages", (int) Math.ceil((double) totalItems / size));
        
        return response;
    }

    public Map<String, Object> getProductsByCityWithFilters(
            String city, String category, String sortBy, String condition, 
            String sellingType, int page, int size) {
        
        // Add logging to debug search table feature
        System.out.println("getProductsByCityWithFilters called for city: " + city);
        
        // Use the correct method to check feature toggle
        boolean useSearchTable = featureToggleConfig.useSearchTableForCity();
        System.out.println("Feature toggle for search table enabled: " + useSearchTable);
        
        if (useSearchTable) {
            System.out.println("Using search table implementation for city: " + city);
            return getProductsByCityWithFiltersUsingSearchTable(
                city, category, sortBy, condition, sellingType, page, size);
        } else {
            System.out.println("Using repository implementation for city: " + city);
            return getProductsByCityWithFiltersUsingRepository(
                city, category, sortBy, condition, sellingType, page, size);
        }
    }
    
    /**
     * Alternative implementation for city filtering using search table
     */
    private Map<String, Object> getProductsByCityWithFiltersUsingSearchTable(
            String city, String category, String sortBy, String condition, 
            String sellingType, int page, int size) {
        
        try {
            System.out.println("getProductsByCityWithFiltersUsingSearchTable called for city: " + city);
            
            // Skip the search service entirely and use repository directly
            // This bypasses any DynamoDB query issues with the search table
            // Use exact city name without converting to lowercase
            List<Product> products = productRepository.findByCity(city);
            System.out.println("Found " + products.size() + " products for city: " + city);
            
            // Filter by category if provided
            if (category != null && !category.isEmpty()) {
                products = products.stream()
                    .filter(p -> category.equals(p.getCategory()))
                    .collect(Collectors.toList());
                System.out.println("After category filtering: " + products.size() + " products");
            }
            
            // Filter by condition if provided
            if (condition != null && !condition.isEmpty()) {
                products = products.stream()
                    .filter(p -> condition.equals(p.getProductage()))
                    .collect(Collectors.toList());
                System.out.println("After condition filtering: " + products.size() + " products");
            }
            
            // Filter by selling type if provided
            if (sellingType != null && !sellingType.isEmpty()) {
                products = products.stream()
                    .filter(p -> sellingType.equals(p.getSellingtype()))
                    .collect(Collectors.toList());
                System.out.println("After selling type filtering: " + products.size() + " products");
            }
            
            // Apply sorting
            applySorting(products, sortBy);
            
            // Create pagination response
            int totalItems = products.size();
            int totalPages = (int) Math.ceil((double) totalItems / size);
            
            // Apply pagination
            int startIndex = Math.min(page * size, totalItems);
            int endIndex = Math.min(startIndex + size, totalItems);
            List<Product> paginatedProducts = 
                (startIndex < endIndex) ? products.subList(startIndex, endIndex) : new ArrayList<>();
                
            // Create the response map
            Map<String, Object> response = new HashMap<>();
            response.put("products", paginatedProducts);
            response.put("totalItems", totalItems);
            response.put("currentPage", page);
            response.put("totalPages", totalPages);
            
            return response;
        } catch (Exception e) {
            // Log the error
            System.err.println("Error in search table implementation: " + e.getMessage());
            e.printStackTrace();
            
            // Fall back to the original repository implementation
            System.out.println("Falling back to repository implementation for city: " + city);
            return getProductsByCityWithFiltersUsingRepository(
                city, category, sortBy, condition, sellingType, page, size);
        }
    }
    
    /**
     * Original repository-based implementation, extracted for fallback
     */
    public Map<String, Object> getProductsByCityWithFiltersUsingRepository(
            String city, String category, String sortBy, String condition, 
            String sellingType, int page, int size) {
        
        // Get products from repository using exact city name
        List<Product> products = productRepository.findByCity(city);
        System.out.println("Repository found " + products.size() + " products for city: " + city);
        
        // Apply filters
        if (category != null && !category.isEmpty()) {
            products = products.stream()
                .filter(p -> category.equals(p.getCategory()))
                .collect(Collectors.toList());
            System.out.println("After category filtering: " + products.size() + " products");
        }
        
        if (condition != null && !condition.isEmpty()) {
            products = products.stream()
                .filter(p -> condition.equals(p.getProductage()))
                .collect(Collectors.toList());
            System.out.println("After condition filtering: " + products.size() + " products");
        }
        
        if (sellingType != null && !sellingType.isEmpty()) {
            products = products.stream()
                .filter(p -> sellingType.equals(p.getSellingtype()))
                .collect(Collectors.toList());
            System.out.println("After selling type filtering: " + products.size() + " products");
        }
        
        // Apply sorting
        applySorting(products, sortBy);
        
        // Apply pagination
        int totalItems = products.size();
        int startIndex = Math.min(page * size, totalItems);
        int endIndex = Math.min(startIndex + size, totalItems);
        List<Product> paginatedProducts = 
            (startIndex < endIndex) ? products.subList(startIndex, endIndex) : new ArrayList<>();
        
        Map<String, Object> response = new HashMap<>();
        response.put("products", paginatedProducts);
        response.put("totalItems", totalItems);
        response.put("currentPage", page);
        response.put("totalPages", (int) Math.ceil((double) totalItems / size));
        
        return response;
    }

    public List<Map<String, String>> getNearbyUniversities(String zipcode, int radiusInMiles) {
        // This would typically integrate with a geocoding service or database
        // For now, return a mock list of nearby universities
        List<Map<String, String>> universities = new ArrayList<>();
        Map<String, String> university1 = new HashMap<>();
        university1.put("name", "San Jose State University");
        university1.put("distance", "0.5 miles");
        universities.add(university1);
        
        Map<String, String> university2 = new HashMap<>();
        university2.put("name", "Santa Clara University");
        university2.put("distance", "2.3 miles");
        universities.add(university2);
        
        return universities;
    }

    public List<Product> getFeaturedProducts(String university, String city, int limit) {
        // Get all products for the university and city
        List<Product> products = productRepository.findByUniversity(university);
        
        // Filter by city
        products = products.stream()
            .filter(p -> city.equals(p.getCity()))
            .collect(Collectors.toList());
            
        // Sort by number of views/interests (this would be implemented when we add view tracking)
        // For now, we'll sort by posting date to show recent products
        products.sort(Comparator.comparing(Product::getPostingdate).reversed());
        
        // Limit the number of products returned
        return products.stream()
            .limit(limit)
            .collect(Collectors.toList());
    }

    public List<Product> getNewArrivals(String university, int limit) {
        try {
            System.out.println("getNewArrivals called for university: " + university + ", limit: " + limit);
            
            // Check if we should use the search table implementation
            boolean useSearchTable = featureToggleConfig.useSearchTableForNewArrivals();
            System.out.println("Feature toggle for search table enabled for new arrivals: " + useSearchTable);
            
            if (useSearchTable) {
                System.out.println("Using search table implementation for new arrivals from: " + university);
                return getNewArrivalsUsingSearchTable(university, limit);
            } else {
                System.out.println("Using repository implementation for new arrivals from: " + university);
                return getNewArrivalsUsingRepository(university, limit);
            }
        } catch (Exception e) {
            System.err.println("Error in getNewArrivals: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
    
    /**
     * Gets new arrivals using the search table for a university
     * This implementation is more efficient for large datasets
     */
    private List<Product> getNewArrivalsUsingSearchTable(String university, int limit) {
        try {
            System.out.println("getNewArrivalsUsingSearchTable called for university: " + university);
            
            // Use the search service's cost-efficient method for new arrivals
            List<Product> products = searchService.getNewArrivalsForUniversity(university, limit);
            
            if (!products.isEmpty()) {
                System.out.println("Successfully retrieved " + products.size() + " new arrivals using search table");
                return products;
            }
            
            // Fallback to repository if search table returns no results
            System.out.println("No results from search table, falling back to repository");
            products = productRepository.findByUniversity(university);
            System.out.println("Found " + products.size() + " products for university: " + university + " via repository");
            
            if (products.isEmpty()) {
                System.out.println("No products found, returning empty list");
                return new ArrayList<>();
            }
            
            // Filter only available products
            System.out.println("Filtering for available products");
            products = products.stream()
                .filter(product -> "available".equals(product.getStatus()))
                .collect(Collectors.toList());
            
            System.out.println("After filtering, " + products.size() + " available products");
            
            // Sort by posting date (newest first)
            System.out.println("Sorting products by posting date");
            products.sort(Comparator.comparing(Product::getPostingdate).reversed());
            
            // Take only the requested number of products
            if (products.size() > limit) {
                System.out.println("Limiting to " + limit + " products");
                return products.subList(0, limit);
            }
            
            System.out.println("Returning " + products.size() + " products");
            return products;
        } catch (Exception e) {
            System.err.println("Error in getNewArrivalsUsingSearchTable: " + e.getMessage());
            e.printStackTrace();
            
            // Fallback to repository method
            System.out.println("Falling back to repository method for new arrivals");
            return getNewArrivalsUsingRepository(university, limit);
        }
    }
    
    /**
     * Original repository-based implementation for new arrivals
     */
    private List<Product> getNewArrivalsUsingRepository(String university, int limit) {
        try {
            List<Product> products;
            
            if (university != null && !university.isEmpty()) {
                System.out.println("Finding products for university: " + university);
                List<Product> dbResults = productRepository.findByUniversity(university);
                // Convert to ArrayList before sorting (DynamoDB PaginatedList doesn't support sort)
                products = new ArrayList<>(dbResults);
                System.out.println("Found " + products.size() + " products for university: " + university);
            } else {
                System.out.println("Finding all products");
                List<Product> dbResults = productRepository.findAll();
                // Convert to ArrayList before sorting
                products = new ArrayList<>(dbResults);
                System.out.println("Found " + products.size() + " products total");
            }
            
            if (products.isEmpty()) {
                System.out.println("No products found, returning empty list");
                return new ArrayList<>();
            }
            
            // Sort by posting date (newest first)
            System.out.println("Sorting products by posting date");
            products.sort(Comparator.comparing(Product::getPostingdate).reversed());
            
            // Filter only available products
            System.out.println("Filtering for available products");
            products = products.stream()
                .filter(product -> "available".equals(product.getStatus()))
                .collect(Collectors.toList());
            
            System.out.println("After filtering, " + products.size() + " available products");
            
            // Take only the requested number of products
            if (products.size() > limit) {
                System.out.println("Limiting to " + limit + " products");
                return products.subList(0, limit);
            }
            
            System.out.println("Returning " + products.size() + " products");
            return products;
        } catch (Exception e) {
            System.err.println("Error processing products in repository method: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
    
    /**
     * Efficiently retrieves all products in a user's wishlist in a single batch operation.
     * This is much more cost-effective than retrieving products one by one.
     *
     * @param productIds List of product IDs from the user's wishlist
     * @return List of Products that are in the user's wishlist
     */
    public List<Product> getWishlistProducts(List<String> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return new ArrayList<>();
        }
        
        // Use the batch load functionality for maximum efficiency
        return productRepository.batchLoad(productIds);
    }
    
    /**
     * Search for products by keyword across name and description
     * with pagination support
     * 
     * @param keyword The search term
     * @param page Page number (zero-based)
     * @param size Number of items per page
     * @return Map containing the search results and pagination info
     */
    public Map<String, Object> searchProductsByKeyword(String keyword, int page, int size) {
        List<Product> matchingProducts = productRepository.searchByKeyword(keyword);
        
        // Filter out products that are not available
        matchingProducts = matchingProducts.stream()
            .filter(product -> "available".equals(product.getStatus()))
            .collect(Collectors.toList());
        
        // Apply pagination
        int totalItems = matchingProducts.size();
        int startIndex = Math.min(page * size, totalItems);
        int endIndex = Math.min(startIndex + size, totalItems);
        
        List<Product> paginatedProducts = 
            startIndex < endIndex ? matchingProducts.subList(startIndex, endIndex) : new ArrayList<>();
        
        Map<String, Object> response = new HashMap<>();
        response.put("products", paginatedProducts);
        response.put("totalItems", totalItems);
        response.put("currentPage", page);
        response.put("totalPages", (int) Math.ceil((double) totalItems / size));
        
        return response;
    }

    // Method to directly get products by city from repository
    public List<Product> getProductsByCity(String city) {
        return productRepository.findByCity(city);
    }

    /**
     * Helper method to apply sorting to a list of products
     */
    private void applySorting(List<Product> products, String sortBy) {
        if (sortBy != null && !sortBy.isEmpty()) {
            switch (sortBy) {
                case "price_low_high":
                    products.sort((p1, p2) -> {
                        double price1 = Double.parseDouble(p1.getPrice());
                        double price2 = Double.parseDouble(p2.getPrice());
                        return Double.compare(price1, price2);
                    });
                    break;
                case "price_high_low":
                    products.sort((p1, p2) -> {
                        double price1 = Double.parseDouble(p1.getPrice());
                        double price2 = Double.parseDouble(p2.getPrice());
                        return Double.compare(price2, price1);
                    });
                    break;
                case "newest":
                    products.sort(Comparator.comparing(Product::getPostingdate).reversed());
                    break;
                case "popularity":
                    // Implement popularity-based sorting if needed
                    break;
            }
        }
    }
} 