package com.example.demo.controller;

import com.example.demo.model.Product;
import com.example.demo.service.ProductService;
import com.example.demo.service.S3Service;
import com.example.demo.service.ProductSearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;
    private final S3Service s3Service;
    private final ProductSearchService searchService;

    @Autowired
    public ProductController(ProductService productService, S3Service s3Service, ProductSearchService searchService) {
        this.productService = productService;
        this.s3Service = s3Service;
        this.searchService = searchService;
    }

    // Get products by university with optional filters
    @GetMapping("/university/{university}")
    public ResponseEntity<Map<String, Object>> getProductsByUniversity(
            @PathVariable String university,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String condition,
            @RequestParam(required = false) String sellingType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            Map<String, Object> response = productService.getProductsByUniversityWithFilters(
                university, category, sortBy, condition, sellingType, page, size);
                
            // If no products found through search table, fallback to direct repository
            if (response.containsKey("products") && ((List<?>)response.get("products")).isEmpty() 
                && (int)response.get("totalItems") == 0) {
                System.out.println("No products found via search table, falling back to repository for university: " + university);
                
                // Fallback to direct repository access
                List<Product> directProducts = productService.getProductsByUniversity(university);
                
                if (!directProducts.isEmpty()) {
                    System.out.println("Found " + directProducts.size() + " products directly from repository");
                    Map<String, Object> fallbackResponse = productService.getProductsByUniversityWithFiltersUsingRepository(
                        university, category, sortBy, condition, sellingType, page, size);
                    return ResponseEntity.ok(fallbackResponse);
                }
            }
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    // Get products by city with university filtering
    @GetMapping("/city/{city}")
    public ResponseEntity<Map<String, Object>> getProductsByCity(
            @PathVariable String city,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String condition,
            @RequestParam(required = false) String sellingType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            Map<String, Object> response = productService.getProductsByCityWithFilters(
                city, category, sortBy, condition, sellingType, page, size);
            
            // If no products found through search table, fallback to direct repository
            if (response.containsKey("products") && ((List<?>)response.get("products")).isEmpty() 
                && (int)response.get("totalItems") == 0) {
                System.out.println("No products found via search table, falling back to repository for city: " + city);
                
                // Fallback to direct repository access
                List<Product> directProducts = productService.getProductsByCity(city);
                
                if (!directProducts.isEmpty()) {
                    System.out.println("Found " + directProducts.size() + " products directly from repository");
                    Map<String, Object> fallbackResponse = productService.getProductsByCityWithFiltersUsingRepository(
                        city, category, sortBy, condition, sellingType, page, size);
                    return ResponseEntity.ok(fallbackResponse);
                }
            }
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    // Get nearby universities based on zipcode
    @GetMapping("/nearby-universities/{zipcode}")
    public ResponseEntity<List<Map<String, String>>> getNearbyUniversities(
            @PathVariable String zipcode,
            @RequestParam(defaultValue = "10") int radiusInMiles) {
        try {
            List<Map<String, String>> universities = productService.getNearbyUniversities(zipcode, radiusInMiles);
            return new ResponseEntity<>(universities, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // Get featured products for a university and city
    @GetMapping("/featured/{university}/{city}")
    public ResponseEntity<List<Product>> getFeaturedProducts(
            @PathVariable String university,
            @PathVariable String city,
            @RequestParam(defaultValue = "5") int limit) {
        try {
            List<Product> featuredProducts = productService.getFeaturedProducts(university, city, limit);
            return new ResponseEntity<>(featuredProducts, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // Get new arrivals for a university
    @GetMapping("/new-arrivals/{university}")
    public ResponseEntity<List<Product>> getNewArrivals(
            @PathVariable String university,
            @RequestParam(defaultValue = "5") int limit) {
        try {
            List<Product> newArrivals = productService.getNewArrivals(university, limit);
            return new ResponseEntity<>(newArrivals, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // Search products by keyword with location filter
    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> searchProducts(
            @RequestParam String keyword,
            @RequestParam(required = false) String university,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        try {
            // Validate that at least one location filter is provided
            if ((university == null || university.isEmpty()) && 
                (city == null || city.isEmpty())) {
                throw new IllegalArgumentException("Either university or city must be provided");
            }
            
            // Cap the page size for efficiency
            int actualSize = Math.min(size, 50);
            
            // Perform the search
            Map<String, Object> results = searchService.searchProducts(
                keyword, university, city, category, page, actualSize);
            
            return ResponseEntity.ok(results);
        } catch (IllegalArgumentException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "An error occurred during search: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    // created new product listing
    @PostMapping
    public ResponseEntity<Product> createProduct(@RequestBody Product product) {
        Product createdProduct = productService.createProduct(product);
        return new ResponseEntity<>(createdProduct, HttpStatus.CREATED);
    }

    // fetch product details based on product id
    @GetMapping("/{id}")
    public ResponseEntity<Product> getProductById(@PathVariable String id) {
        Product product = productService.getProductById(id);
        if (product != null) {
            return new ResponseEntity<>(product, HttpStatus.OK);
        }
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    // fetch product details based on user email
    @GetMapping("/user/{email}")
    public ResponseEntity<List<Product>> getProductsByEmail(@PathVariable String email) {
        List<Product> products = productService.getProductsByEmail(email);
        return new ResponseEntity<>(products, HttpStatus.OK);
    }

    // fetch product details based on category
    @GetMapping("/category/{category}")
    public ResponseEntity<List<Product>> getProductsByCategory(@PathVariable String category) {
        List<Product> products = productService.getProductsByCategory(category);
        return new ResponseEntity<>(products, HttpStatus.OK);
    }

    // update product details based on product id
    @PutMapping("/{id}")
    public ResponseEntity<Product> updateProduct(@PathVariable String id, @RequestBody Product product) {
        try {
            // Update product with correct method signature (id, product)
            Product updatedProduct = productService.updateProduct(id, product);
            return new ResponseEntity<>(updatedProduct, HttpStatus.OK);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
    
    @PatchMapping("/{id}/status")
    public ResponseEntity<Product> updateProductStatus(
            @PathVariable String id, 
            @RequestParam String status) {
        try {
            Product updatedProduct = productService.updateProductStatus(id, status);
            return new ResponseEntity<>(updatedProduct, HttpStatus.OK);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
    
    @PostMapping("/{id}/images")
    public ResponseEntity<Product> addProductImage(
            @PathVariable String id, 
            @RequestParam String imageUrl) {
        try {
            Product updatedProduct = productService.addProductImage(id, imageUrl);
            return new ResponseEntity<>(updatedProduct, HttpStatus.OK);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
    
    @DeleteMapping("/{id}/images")
    public ResponseEntity<Product> removeProductImage(
            @PathVariable String id, 
            @RequestParam String imageUrl) {
        try {
            Product updatedProduct = productService.removeProductImage(id, imageUrl);
            return new ResponseEntity<>(updatedProduct, HttpStatus.OK);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteProduct(@PathVariable String id) {
        productService.deleteProduct(id);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Product deleted successfully");
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
    
    /**
     * Creates a product with pre-uploaded image filenames from S3
     * For use with the mobile PostingScreen where images are uploaded first
     */
    @PostMapping("/with-image-filenames")
    public ResponseEntity<Product> createProductWithImageFilenames(
            @RequestBody Map<String, Object> payload) {
        try {
            // Extract product data
            Product product = new Product();
            
            if (payload.containsKey("name")) {
                product.setName(payload.get("name").toString());
            }
            
            if (payload.containsKey("category")) {
                product.setCategory(payload.get("category").toString());
            }
            
            if (payload.containsKey("description")) {
                product.setDescription(payload.get("description").toString());
            }
            
            if (payload.containsKey("price")) {
                product.setPrice(payload.get("price").toString());
            }
            
            if (payload.containsKey("email")) {
                product.setEmail(payload.get("email").toString());
            }
            
            if (payload.containsKey("sellerName")) {
                product.setSellerName(payload.get("sellerName").toString());
            }
            
            if (payload.containsKey("city")) {
                product.setCity(payload.get("city").toString());
            }
            
            if (payload.containsKey("zipcode")) {
                product.setZipcode(payload.get("zipcode").toString());
            }
            
            if (payload.containsKey("university")) {
                product.setUniversity(payload.get("university").toString());
            }
            
            if (payload.containsKey("productage")) {
                product.setProductage(payload.get("productage").toString());
            }
            
            if (payload.containsKey("sellingtype")) {
                product.setSellingtype(payload.get("sellingtype").toString());
            }
            
            // Handle images
            if (payload.containsKey("imageFilenames") && payload.get("imageFilenames") instanceof List) {
                @SuppressWarnings("unchecked")
                List<String> imageFilenames = (List<String>) payload.get("imageFilenames");
                
                if (!imageFilenames.isEmpty()) {
                    product.setPrimaryImage(imageFilenames.get(0));
                    if (imageFilenames.size() > 1) {
                        product.setAdditionalImages(new ArrayList<>(imageFilenames.subList(1, imageFilenames.size())));
                    }
                    product.setImages(imageFilenames); // For backward compatibility
                }
            }
            
            // Save the product
            Product savedProduct = productService.createProduct(product);
            return new ResponseEntity<>(savedProduct, HttpStatus.CREATED);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/with-images")
    public ResponseEntity<Product> createProductWithImages(
            @RequestBody Product product,
            @RequestParam("imageFiles") MultipartFile[] imageFiles) {
        try {
            // Upload images to S3
            List<String> fileNames = new ArrayList<>();
            for (MultipartFile file : imageFiles) {
                String fileName = s3Service.uploadFile(file);
                fileNames.add(fileName);
            }
            
            // Set the images in the product
            if (!fileNames.isEmpty()) {
                product.setPrimaryImage(fileNames.get(0));
                if (fileNames.size() > 1) {
                    product.setAdditionalImages(new ArrayList<>(fileNames.subList(1, fileNames.size())));
                }
                product.setImages(fileNames); // For backward compatibility
            }
            
            // Save the product
            Product savedProduct = productService.createProduct(product);
            return new ResponseEntity<>(savedProduct, HttpStatus.CREATED);
        } catch (IOException e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Admin endpoint to reindex all products
     */
    @PostMapping("/admin/reindex")
    public ResponseEntity<Map<String, String>> reindexAllProducts() {
        try {
            // Start reindexing in background
            new Thread(() -> {
                searchService.reindexAllProducts();
            }).start();
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "Reindexing initiated in the background");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to start reindexing: " + e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    // Debug endpoint to check if Buffalo products exist in repository
    @GetMapping("/debug/city/{city}")
    public ResponseEntity<Map<String, Object>> debugCityProducts(@PathVariable String city) {
        Map<String, Object> response = new HashMap<>();
        try {
            List<Product> directProducts = productService.getProductsByCity(city);
            response.put("repository_products_count", directProducts.size());
            response.put("city", city);
            if (!directProducts.isEmpty()) {
                response.put("first_product", directProducts.get(0));
            }
            
            // Also check if these products are indexed in the search table
            List<Product> allProducts = productService.getAllProducts();
            response.put("all_products_count", allProducts.size());
            
            List<Product> buffaloProducts = allProducts.stream()
                .filter(p -> city.equals(p.getCity()))
                .collect(Collectors.toList());
            response.put("filtered_city_products", buffaloProducts.size());
            
            if (!buffaloProducts.isEmpty()) {
                // Check if these products have been indexed
                response.put("first_product_details", Map.of(
                    "id", buffaloProducts.get(0).getId(),
                    "name", buffaloProducts.get(0).getName(),
                    "city", buffaloProducts.get(0).getCity(),
                    "status", buffaloProducts.get(0).getStatus()
                ));
            }
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
} 