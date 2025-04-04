package com.example.demo.controller;

import com.example.demo.model.Product;
import com.example.demo.service.ProductService;
import com.example.demo.service.S3Service;
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

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;
    private final S3Service s3Service;

    @Autowired
    public ProductController(ProductService productService, S3Service s3Service) {
        this.productService = productService;
        this.s3Service = s3Service;
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
            @RequestParam(required = false) String university,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String condition,
            @RequestParam(required = false) String sellingType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            Map<String, Object> response = productService.getProductsByCityWithFilters(
                city, university, category, sortBy, condition, sellingType, page, size);
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
    public ResponseEntity<Product> updateProduct(
            @PathVariable String id, 
            @RequestBody Product product) {
        product.setId(id);
        try {
            Product updatedProduct = productService.updateProduct(product);
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
} 