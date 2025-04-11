# Student Marketplace API - Frontend Integration Guide

This guide explains how to integrate your React Native mobile app with the Student Marketplace API.

## API Base URL

```
http://your-api-domain:8080
```

## Environment Setup

### AWS Credentials Configuration

1. The application requires AWS credentials to interact with S3 and DynamoDB.
2. Create a copy of the application.properties.template file:
   ```
   cp src/main/resources/application.properties.template src/main/resources/application.properties
   ```
3. Edit the application.properties file with your AWS credentials:
   ```
   aws.s3.access-key=YOUR_ACCESS_KEY_HERE
   aws.s3.secret-key=YOUR_SECRET_KEY_HERE
   aws.dynamodb.access-key=YOUR_ACCESS_KEY_HERE
   aws.dynamodb.secret-key=YOUR_SECRET_KEY_HERE
   ```
4. The application.properties file is ignored by git to prevent credential leakage.

### Required DynamoDB Tables

The application requires the following DynamoDB tables to be set up:
- `products`: For storing product listings
- `userdetails`: For storing user details
- `userwishlist`: For storing wishlist items

## Authentication

Currently, the API doesn't require authentication tokens. Email addresses are used to identify users.

## Available Endpoints

| Method | Endpoint | Description | Pagination |
|--------|----------|-------------|------------|
| POST | `/api/files/product-images` | Upload multiple product images (max 5) | No |
| POST | `/api/products/with-image-filenames` | Create product with pre-uploaded images | No |
| POST | `/api/products/with-images` | Create product with images in one request | No |
| POST | `/api/products` | Create a basic product without images | No |
| GET | `/api/products/{id}` | Get product by ID | No |
| GET | `/api/products/user/{email}` | Get products by user email | No |
| GET | `/api/products/category/{category}` | Get products by category | No |
| GET | `/api/products/category/{category}/paginated` | Get products by category with pagination | **Yes** |
| GET | `/api/products/university/{university}` | Get products by university with filters | **Yes** |
| GET | `/api/products/city/{city}` | Get products by city with filters | **Yes** |
| GET | `/api/products/nearby-universities/{zipcode}` | Get nearby universities based on zipcode | No |
| GET | `/api/products/featured/{university}/{city}` | Get featured products for university and city | No |
| GET | `/api/products/new-arrivals/{university}` | Get newest products for university | No |
| GET | `/api/products/search` | Search products by keyword | **Yes** |
| PUT | `/api/products/{id}` | Update a product | No |
| PATCH | `/api/products/{id}/status` | Update product status | No |
| POST | `/api/products/{id}/images` | Add an image to a product | No |
| DELETE | `/api/products/{id}/images` | Remove an image from a product | No |
| DELETE | `/api/products/{id}` | Delete a product | No |
| GET | `/api/wishlist/{email}` | Get wishlist items for a user | No |
| GET | `/api/wishlist/{email}/products` | Get full product details for a user's wishlist | No |
| POST | `/api/wishlist/{email}` | Add product to wishlist | No |
| DELETE | `/api/wishlist/{email}/{productId}` | Remove product from wishlist | No |
| GET | `/api/wishlist/{email}/check/{productId}` | Check if product is in user's wishlist | No |

## Paginated API Endpoints

### 1. Get Products by University with Pagination

Retrieves products for a specific university with various filtering options and pagination.

**Endpoint**: `GET /api/products/university/{university}`

**URL Parameters**:
- `university`: University name (e.g., "SUNY Buffalo")

**Query Parameters**:
- `category` (optional): Filter by product category (e.g., "textbooks", "furniture")
- `sortBy` (optional): Sort products by "price_low_high", "price_high_low", "newest", or "popularity"
- `condition` (optional): Filter by product condition (e.g., "like-new", "good", "fair")
- `sellingType` (optional): Filter by selling type (e.g., "sell", "giveaway")
- `page` (optional): Page number (zero-based, default: 0)
- `size` (optional): Items per page (default: 20)

**Example Request**:
```javascript
// Fetch first page of textbooks from SUNY Buffalo, 10 items per page, sorted by newest first
fetch('http://your-api-domain:8080/api/products/university/SUNY%20Buffalo?category=textbooks&sortBy=newest&page=0&size=10')
  .then(response => response.json())
  .then(data => {
    console.log(`Showing ${data.products.length} of ${data.totalItems} total products`);
    console.log(`Page ${data.currentPage + 1} of ${data.totalPages}`);
    
    // Display products
    data.products.forEach(product => {
      console.log(`${product.name} - $${product.price}`);
    });
    
    // Check if there are more pages
    const hasNextPage = data.currentPage < data.totalPages - 1;
    if (hasNextPage) {
      // Show "Load More" button or implement infinite scrolling
    }
  });
```

**Response**:
```json
{
  "products": [
    {
      "id": "123abc",
      "name": "Calculus Textbook",
      "category": "textbooks",
      "price": "45.00",
      "description": "Calculus textbook, barely used",
      "email": "student@university.edu",
      "sellerName": "Jane Smith",
      "university": "SUNY Buffalo",
      "city": "Buffalo",
      "zipcode": "14260",
      "primaryImage": "https://your-bucket.s3.amazonaws.com/image1.jpg",
      "additionalImages": [],
      "postingdate": "2023-09-15T14:30:00",
      "productage": "like-new",
      "sellingtype": "sell",
      "status": "available"
    },
    // More products...
  ],
  "totalItems": 53,
  "currentPage": 0,
  "totalPages": 6
}
```

### 2. Get Products by City with Pagination

Retrieves products for a specific city with various filtering options and pagination.

**Endpoint**: `GET /api/products/city/{city}`

**URL Parameters**:
- `city`: City name (e.g., "Buffalo")

**Query Parameters**:
- `university` (optional): Filter by specific university
- `category` (optional): Filter by product category
- `sortBy` (optional): Sort products by "price_low_high", "price_high_low", "newest", or "popularity"
- `condition` (optional): Filter by product condition
- `sellingType` (optional): Filter by selling type
- `page` (optional): Page number (zero-based, default: 0)
- `size` (optional): Items per page (default: 20)

**Example Request**:
```javascript
// Function to fetch products from a city with pagination
async function fetchProductsByCity(city, filters = {}, page = 0) {
  // Build query parameters
  const queryParams = new URLSearchParams();
  
  if (filters.university) queryParams.append('university', filters.university);
  if (filters.category) queryParams.append('category', filters.category);
  if (filters.sortBy) queryParams.append('sortBy', filters.sortBy);
  if (filters.condition) queryParams.append('condition', filters.condition);
  if (filters.sellingType) queryParams.append('sellingType', filters.sellingType);
  
  // Add pagination parameters
  queryParams.append('page', page);
  queryParams.append('size', filters.size || 20);
  
  const response = await fetch(
    `http://your-api-domain:8080/api/products/city/${encodeURIComponent(city)}?${queryParams}`
  );
  
  if (!response.ok) {
    throw new Error('Failed to fetch products');
  }
  
  return await response.json();
}

// Example usage with React component for infinite scrolling
function ProductList() {
  const [products, setProducts] = useState([]);
  const [page, setPage] = useState(0);
  const [hasMore, setHasMore] = useState(true);
  const [loading, setLoading] = useState(false);
  
  const loadMoreProducts = async () => {
    if (loading || !hasMore) return;
    
    setLoading(true);
    try {
      const result = await fetchProductsByCity('Buffalo', { category: 'electronics' }, page);
      
      // Append new products to existing list
      setProducts(prev => [...prev, ...result.products]);
      
      // Update pagination state
      setPage(page + 1);
      setHasMore(page + 1 < result.totalPages);
    } catch (error) {
      console.error('Error loading products:', error);
    } finally {
      setLoading(false);
    }
  };
  
  // Initial load
  useEffect(() => {
    loadMoreProducts();
  }, []);
  
  return (
    <div>
      {/* Product list */}
      <div className="product-grid">
        {products.map(product => (
          <ProductCard key={product.id} product={product} />
        ))}
      </div>
      
      {/* Load more button */}
      {hasMore && (
        <button 
          disabled={loading} 
          onClick={loadMoreProducts}
        >
          {loading ? 'Loading...' : 'Load More'}
        </button>
      )}
    </div>
  );
}
```

**Response**: Same format as the university endpoint.

### 3. Search Products by Keyword with Pagination

Search for products using keywords with filtering options and pagination.

**Endpoint**: `GET /api/products/search`

**Query Parameters**:
- `keyword` (required): Search term to match against product names and descriptions
- `university` (optional): Filter by university
- `city` (optional): Filter by city
- `category` (optional): Filter by category
- `page` (optional): Page number (zero-based, default: 0)
- `size` (optional): Items per page (default: 20, max: 50)

**Note**: Either `university` or `city` parameter must be provided.

**Example Request**:
```javascript
// Function to search products with pagination
async function searchProducts(keyword, filters, page = 0) {
  const queryParams = new URLSearchParams();
  queryParams.append('keyword', keyword);
  
  // Add location filters (at least one is required)
  if (filters.university) queryParams.append('university', filters.university);
  if (filters.city) queryParams.append('city', filters.city);
  
  // Add optional filters
  if (filters.category) queryParams.append('category', filters.category);
  
  // Add pagination parameters
  queryParams.append('page', page);
  queryParams.append('size', filters.size || 20);
  
  const response = await fetch(
    `http://your-api-domain:8080/api/products/search?${queryParams}`
  );
  
  if (!response.ok) {
    const errorData = await response.json();
    throw new Error(errorData.error || 'Failed to search products');
  }
  
  return await response.json();
}

// Example usage with state management for pagination
// In a React component
const [searchResults, setSearchResults] = useState([]);
const [pagination, setPagination] = useState({
  currentPage: 0,
  totalPages: 0,
  totalItems: 0
});

const performSearch = async (query, page = 0) => {
  setLoading(true);
  try {
    const results = await searchProducts(query, {
      university: 'SUNY Buffalo',
      size: 10
    }, page);
    
    if (page === 0) {
      // New search, replace results
      setSearchResults(results.products);
    } else {
      // Loading more, append results
      setSearchResults(prev => [...prev, ...results.products]);
    }
    
    setPagination({
      currentPage: results.currentPage,
      totalPages: results.totalPages,
      totalItems: results.totalItems
    });
  } catch (error) {
    console.error('Search error:', error);
    setError(error.message);
  } finally {
    setLoading(false);
  }
};
```

**Response**:
```json
{
  "products": [
    {
      "id": "abc123",
      "name": "Computer Science Textbook",
      "category": "textbooks",
      "price": "35.00",
      "description": "Introduction to Computer Science, great condition!",
      "primaryImage": "https://your-bucket.s3.amazonaws.com/image2.jpg",
      "university": "SUNY Buffalo",
      "city": "Buffalo",
      // Other product fields...
    },
    // More products...
  ],
  "totalItems": 28,
  "currentPage": 0,
  "totalPages": 3
}
```

### 4. Get Products by Category with Pagination

Retrieves products in a specific category with pagination and optional sorting.

**Endpoint**: `GET /api/products/category/{category}/paginated`

**URL Parameters**:
- `category`: Product category (e.g., "textbooks", "furniture", "electronics", etc.)

**Query Parameters**:
- `sortBy` (optional): Sort products by "price_low_high", "price_high_low", or "newest"
- `page` (optional): Page number (zero-based, default: 0)
- `size` (optional): Items per page (default: 20)

**Example Request**:
```javascript
// Function to fetch products by category with pagination
async function getProductsByCategory(category, page = 0, size = 20, sortBy = 'newest') {
  try {
    const queryParams = new URLSearchParams({
      page,
      size,
      sortBy
    });
    
    const response = await fetch(
      `http://your-api-domain:8080/api/products/category/${encodeURIComponent(category)}/paginated?${queryParams}`
    );
    
    if (!response.ok) {
      throw new Error('Failed to fetch products');
    }
    
    return await response.json();
  } catch (error) {
    console.error(`Error fetching ${category} products:`, error);
    throw error;
  }
}

// Example usage in a React component
function CategoryProductList({ category }) {
  const [products, setProducts] = useState([]);
  const [pagination, setPagination] = useState({
    currentPage: 0,
    totalPages: 0,
    totalItems: 0
  });
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  
  const loadPage = async (pageNumber) => {
    setLoading(true);
    try {
      const result = await getProductsByCategory(category, pageNumber, 10, 'price_low_high');
      setProducts(result.products);
      setPagination({
        currentPage: result.currentPage,
        totalPages: result.totalPages,
        totalItems: result.totalItems
      });
      setError(null);
    } catch (error) {
      setError('Failed to load products. Please try again.');
    } finally {
      setLoading(false);
    }
  };
  
  // Load first page when component mounts or category changes
  useEffect(() => {
    loadPage(0);
  }, [category]);
  
  if (loading && pagination.currentPage === 0) {
    return <ActivityIndicator size="large" color="#0000ff" />;
  }
  
  if (error) {
    return (
      <View>
        <Text>{error}</Text>
        <Button title="Retry" onPress={() => loadPage(pagination.currentPage)} />
      </View>
    );
  }
  
  return (
    <View>
      <Text style={styles.title}>{category} Products</Text>
      <Text style={styles.subtitle}>
        Showing {products.length} of {pagination.totalItems} products
      </Text>
      
      <FlatList
        data={products}
        keyExtractor={(item) => item.id}
        renderItem={({ item }) => <ProductCard product={item} />}
        contentContainerStyle={styles.productList}
      />
      
      <View style={styles.pagination}>
        <Button
          title="Previous"
          disabled={pagination.currentPage === 0}
          onPress={() => loadPage(pagination.currentPage - 1)}
        />
        <Text>Page {pagination.currentPage + 1} of {pagination.totalPages}</Text>
        <Button
          title="Next"
          disabled={pagination.currentPage >= pagination.totalPages - 1}
          onPress={() => loadPage(pagination.currentPage + 1)}
        />
      </View>
    </View>
  );
}
```

**Response**:
```json
{
  "products": [
    {
      "id": "456def",
      "name": "Gaming Laptop",
      "category": "electronics",
      "price": "899.99",
      "description": "Gaming laptop, 16GB RAM, 512GB SSD",
      "email": "seller@example.com",
      "sellerName": "John Doe",
      "university": "SUNY Buffalo",
      "city": "Buffalo",
      "zipcode": "14260",
      "primaryImage": "https://your-bucket.s3.amazonaws.com/laptop.jpg",
      "additionalImages": ["https://your-bucket.s3.amazonaws.com/laptop_side.jpg"],
      "postingdate": "2023-09-10T10:15:00",
      "productage": "good",
      "sellingtype": "sell",
      "status": "available"
    },
    // More products...
  ],
  "totalItems": 45,
  "currentPage": 0,
  "totalPages": 5
}
```

## Implementing Pagination in Your App

### Example: Pagination Component

```javascript
import React from 'react';
import { View, Button, Text } from 'react-native';

const Pagination = ({ currentPage, totalPages, onPageChange }) => {
  return (
    <View style={styles.paginationContainer}>
      <Button 
        title="Previous"
        disabled={currentPage === 0}
        onPress={() => onPageChange(currentPage - 1)}
      />
      
      <Text style={styles.pageInfo}>
        Page {currentPage + 1} of {totalPages}
      </Text>
      
      <Button 
        title="Next"
        disabled={currentPage >= totalPages - 1}
        onPress={() => onPageChange(currentPage + 1)}
      />
    </View>
  );
};
```

### Example: Infinite Scrolling Implementation

```javascript
import React, { useState, useEffect } from 'react';
import { FlatList, ActivityIndicator } from 'react-native';

const InfiniteProductList = ({ fetchProducts, initialFilters }) => {
  const [products, setProducts] = useState([]);
  const [loading, setLoading] = useState(false);
  const [refreshing, setRefreshing] = useState(false);
  const [page, setPage] = useState(0);
  const [hasMore, setHasMore] = useState(true);
  const [filters, setFilters] = useState(initialFilters);

  // Initial load
  useEffect(() => {
    loadFirstPage();
  }, [filters]);

  // Load first page (refresh)
  const loadFirstPage = async () => {
    setLoading(true);
    setProducts([]);
    setPage(0);
    setHasMore(true);
    
    try {
      const result = await fetchProducts(filters, 0);
      setProducts(result.products);
      setHasMore(result.currentPage < result.totalPages - 1);
    } catch (error) {
      console.error('Error loading products:', error);
    } finally {
      setLoading(false);
    }
  };

  // Load next page (pagination)
  const loadNextPage = async () => {
    if (loading || !hasMore) return;
    
    setLoading(true);
    const nextPage = page + 1;
    
    try {
      const result = await fetchProducts(filters, nextPage);
      setProducts([...products, ...result.products]);
      setPage(nextPage);
      setHasMore(nextPage < result.totalPages - 1);
    } catch (error) {
      console.error('Error loading more products:', error);
    } finally {
      setLoading(false);
    }
  };

  // Handle refresh
  const handleRefresh = async () => {
    setRefreshing(true);
    await loadFirstPage();
    setRefreshing(false);
  };

  return (
    <FlatList
      data={products}
      keyExtractor={(item) => item.id}
      renderItem={({ item }) => <ProductCard product={item} />}
      onEndReached={loadNextPage}
      onEndReachedThreshold={0.5}
      refreshing={refreshing}
      onRefresh={handleRefresh}
      ListFooterComponent={loading && hasMore ? <ActivityIndicator size="large" /> : null}
    />
  );
};
```

## API Endpoints (Full Reference)

### 1. Upload Product Images
```javascript
// Function to upload images from the camera/gallery
const uploadProductImages = async (imageUris) => {
  const formData = new FormData();
  
  // Add each image to form data
  imageUris.forEach((uri, index) => {
    formData.append('images', {
      uri: uri,
      type: 'image/jpeg',
      name: `image${index+1}.jpg`
    });
  });

  try {
    const response = await fetch('http://your-api-domain:8080/api/files/product-images', {
      method: 'POST',
      body: formData,
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    });
    
    const result = await response.json();
    return result;
  } catch (error) {
    console.error('Error uploading images:', error);
    throw error;
  }
};
```

### 2. Create Product Listing
```javascript
// Function to create a product after images are uploaded
const createProduct = async (productDetails, imageFilenames) => {
  try {
    const response = await fetch('http://your-api-domain:8080/api/products/with-image-filenames', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        name: productDetails.name,
        category: productDetails.category,
        description: productDetails.description,
        price: productDetails.price,
        email: productDetails.userEmail,
        sellerName: productDetails.sellerName, // Seller's display name
        city: productDetails.city,
        zipcode: productDetails.zipcode,
        university: productDetails.university,
        productage: productDetails.condition, // 'like-new', 'good', 'fair', etc.
        sellingtype: productDetails.sellingType, // 'sell', 'giveaway', etc.
        imageFilenames: imageFilenames
      }),
    });
    
    const result = await response.json();
    return result;
  } catch (error) {
    console.error('Error creating product:', error);
    throw error;
  }
};
```

### 3. Get University-Specific Products
```javascript
// Product search with university and filtering
const searchProducts = async (university, filters = {}) => {
  try {
    // Build query parameters
    const queryParams = new URLSearchParams();
    
    if (filters.category) queryParams.append('category', filters.category);
    if (filters.sortBy) queryParams.append('sortBy', filters.sortBy);
    if (filters.condition) queryParams.append('condition', filters.condition);
    if (filters.sellingType) queryParams.append('sellingType', filters.sellingType);
    
    // Pagination
    queryParams.append('page', filters.page || 0);
    queryParams.append('size', filters.size || 20);
    
    const response = await fetch(
      `http://your-api-domain:8080/api/products/university/${university}?${queryParams}`
    );
    
    const result = await response.json();
    return result;
  } catch (error) {
    console.error('Error searching products:', error);
    throw error;
  }
};
```

### 4. Get City-Based Products
```javascript
// Search products by city with optional university filter
const searchProductsByCity = async (city, filters = {}) => {
  try {
    const queryParams = new URLSearchParams();
    
    if (filters.university) queryParams.append('university', filters.university);
    if (filters.category) queryParams.append('category', filters.category);
    if (filters.sortBy) queryParams.append('sortBy', filters.sortBy);
    if (filters.condition) queryParams.append('condition', filters.condition);
    if (filters.sellingType) queryParams.append('sellingType', filters.sellingType);
    
    // Pagination
    queryParams.append('page', filters.page || 0);
    queryParams.append('size', filters.size || 20);
    
    const response = await fetch(
      `http://your-api-domain:8080/api/products/city/${encodeURIComponent(city)}?${queryParams}`
    );
    
    const result = await response.json();
    return result;
  } catch (error) {
    console.error('Error searching products by city:', error);
    throw error;
  }
};
```

### 5. Get Nearby Universities
```javascript
// Fetch nearby universities based on zipcode
const fetchNearbyUniversities = async (zipcode, radiusInMiles = 10) => {
  try {
    const response = await fetch(
      `http://your-api-domain:8080/api/products/nearby-universities/${zipcode}?radiusInMiles=${radiusInMiles}`
    );
    
    const result = await response.json();
    return result;
  } catch (error) {
    console.error('Error fetching nearby universities:', error);
    throw error;
  }
};
```

### 6. Get Featured Products
```javascript
// Fetch featured products for university and city
const fetchFeaturedProducts = async (university, city, limit = 5) => {
  try {
    const response = await fetch(
      `http://your-api-domain:8080/api/products/featured/${university}/${encodeURIComponent(city)}?limit=${limit}`
    );
    
    const result = await response.json();
    return result;
  } catch (error) {
    console.error('Error fetching featured products:', error);
    throw error;
  }
};
```

### 7. Get New Arrivals
```javascript
// Fetch new arrivals for university
const fetchNewArrivals = async (university, limit = 5) => {
  try {
    const response = await fetch(
      `http://your-api-domain:8080/api/products/new-arrivals/${university}?limit=${limit}`
    );
    
    const result = await response.json();
    return result;
  } catch (error) {
    console.error('Error fetching new arrivals:', error);
    throw error;
  }
};
```

## HomeScreen Integration Example

```javascript
import React, { useEffect, useState } from 'react';
import { View, Text, FlatList, ActivityIndicator } from 'react-native';

const HomeScreen = ({ userUniversity, userCity }) => {
  const [newArrivals, setNewArrivals] = useState([]);
  const [featuredProducts, setFeaturedProducts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  
  useEffect(() => {
    fetchHomeData();
  }, [userUniversity, userCity]);
  
  const fetchHomeData = async () => {
    setLoading(true);
    try {
      // Fetch both data in parallel
      const [newArrivalsData, featuredData] = await Promise.all([
        fetchNewArrivals(userUniversity),
        fetchFeaturedProducts(userUniversity, userCity)
      ]);
      
      setNewArrivals(newArrivalsData);
      setFeaturedProducts(featuredData);
      setError(null);
    } catch (error) {
      console.error('Error fetching home data:', error);
      setError('Failed to load data. Please try again.');
    } finally {
      setLoading(false);
    }
  };
  
  if (loading) {
    return <ActivityIndicator size="large" color="#0000ff" />;
  }
  
  if (error) {
    return (
      <View>
        <Text>{error}</Text>
        <Button title="Retry" onPress={fetchHomeData} />
      </View>
    );
  }
  
  return (
    <View>
      <Text style={styles.sectionTitle}>New Arrivals</Text>
      <FlatList
        horizontal
        data={newArrivals}
        keyExtractor={(item) => item.id}
        renderItem={({ item }) => (
          <ProductCard product={item} />
        )}
      />
      
      <Text style={styles.sectionTitle}>Featured Products</Text>
      <FlatList
        horizontal
        data={featuredProducts}
        keyExtractor={(item) => item.id}
        renderItem={({ item }) => (
          <ProductCard product={item} />
        )}
      />
    </View>
  );
};
```

## Wishlist Integration

The API provides endpoints to manage user wishlists, allowing users to save products they're interested in.

### 1. Get User's Wishlist Items

```javascript
const getWishlistItems = async (userEmail) => {
  try {
    const response = await fetch(
      `http://your-api-domain:8080/api/wishlist/${encodeURIComponent(userEmail)}`
    );
    
    if (!response.ok) {
      throw new Error('Failed to fetch wishlist');
    }
    
    const wishlistItems = await response.json();
    return wishlistItems;
  } catch (error) {
    console.error('Error fetching wishlist:', error);
    throw error;
  }
};
```

### 2. Get Full Product Details for Wishlist Items

```javascript
const getWishlistProducts = async (userEmail) => {
  try {
    const response = await fetch(
      `http://your-api-domain:8080/api/wishlist/${encodeURIComponent(userEmail)}/products`
    );
    
    if (!response.ok) {
      throw new Error('Failed to fetch wishlist products');
    }
    
    const products = await response.json();
    return products;
  } catch (error) {
    console.error('Error fetching wishlist products:', error);
    throw error;
  }
};
```

### 3. Add Product to Wishlist

```javascript
const addToWishlist = async (userEmail, productId) => {
  try {
    const response = await fetch(
      `http://your-api-domain:8080/api/wishlist/${encodeURIComponent(userEmail)}?productId=${productId}`,
      {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
      }
    );
    
    if (!response.ok) {
      throw new Error('Failed to add item to wishlist');
    }
    
    const result = await response.json();
    return result;
  } catch (error) {
    console.error('Error adding to wishlist:', error);
    throw error;
  }
};
```

### 4. Remove Product from Wishlist

```javascript
const removeFromWishlist = async (userEmail, productId) => {
  try {
    const response = await fetch(
      `http://your-api-domain:8080/api/wishlist/${encodeURIComponent(userEmail)}/${productId}`,
      {
        method: 'DELETE',
      }
    );
    
    if (!response.ok) {
      throw new Error('Failed to remove item from wishlist');
    }
    
    return true;
  } catch (error) {
    console.error('Error removing from wishlist:', error);
    throw error;
  }
};
```

### 5. Check if Product is in Wishlist

```javascript
const isInWishlist = async (userEmail, productId) => {
  try {
    const response = await fetch(
      `http://your-api-domain:8080/api/wishlist/${encodeURIComponent(userEmail)}/check/${productId}`
    );
    
    if (!response.ok) {
      throw new Error('Failed to check wishlist status');
    }
    
    const result = await response.json();
    return result; // Returns true if in wishlist, false otherwise
  } catch (error) {
    console.error('Error checking wishlist status:', error);
    throw error;
  }
};
```

### 6. Wishlist Screen Example

```javascript
import React, { useEffect, useState } from 'react';
import { View, Text, FlatList, Button, ActivityIndicator } from 'react-native';

const WishlistScreen = ({ userEmail }) => {
  const [wishlistProducts, setWishlistProducts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  
  const fetchWishlist = async () => {
    setLoading(true);
    try {
      const products = await getWishlistProducts(userEmail);
      setWishlistProducts(products);
      setError(null);
    } catch (error) {
      console.error('Error fetching wishlist:', error);
      setError('Failed to load wishlist. Please try again.');
    } finally {
      setLoading(false);
    }
  };
  
  useEffect(() => {
    fetchWishlist();
  }, [userEmail]);
  
  const handleRemoveFromWishlist = async (productId) => {
    try {
      await removeFromWishlist(userEmail, productId);
      // Update local state after successful removal
      setWishlistProducts(wishlistProducts.filter(product => product.id !== productId));
    } catch (error) {
      console.error('Error removing item from wishlist:', error);
    }
  };
  
  if (loading) {
    return <ActivityIndicator size="large" color="#0000ff" />;
  }
  
  if (error) {
    return (
      <View>
        <Text>{error}</Text>
        <Button title="Retry" onPress={fetchWishlist} />
      </View>
    );
  }
  
  if (wishlistProducts.length === 0) {
    return (
      <View>
        <Text>Your wishlist is empty.</Text>
        <Button title="Browse Products" onPress={() => {/* Navigate to product listing */}} />
      </View>
    );
  }
  
  return (
    <View>
      <Text style={styles.title}>My Wishlist</Text>
      <FlatList
        data={wishlistProducts}
        keyExtractor={(item) => item.id}
        renderItem={({ item }) => (
          <View style={styles.productCard}>
            <Image source={{ uri: item.primaryImage }} style={styles.productImage} />
            <View style={styles.productInfo}>
              <Text style={styles.productName}>{item.name}</Text>
              <Text style={styles.productPrice}>${item.price}</Text>
              <Button 
                title="Remove" 
                onPress={() => handleRemoveFromWishlist(item.id)} 
              />
              <Button 
                title="View Details" 
                onPress={() => {/* Navigate to product details */}} 
              />
            </View>
          </View>
        )}
      />
    </View>
  );
};
```

## PostingScreen Integration Example

```javascript
// In your PostingScreen component
const handlePostItem = async () => {
  if (!selectedImages.length) {
    setError('Please select at least one image');
    return;
  }
  
  setLoading(true);
  try {
    // Step 1: Upload images
    const imageUploadResult = await uploadProductImages(selectedImages);
    
    // Step 2: Create product with returned image filenames
    const productData = {
      name: productName,
      category: selectedCategory,
      description: description,
      price: price,
      userEmail: userEmail,
      sellerName: userName, // Add seller name
      city: userCity,
      zipcode: userZipcode,
      university: userUniversity,
      condition: productCondition,
      sellingType: sellingType
    };
    
    const createdProduct = await createProduct(productData, imageUploadResult.allFilenames);
    
    // Handle success
    navigation.navigate('SuccessScreen', { productId: createdProduct.id });
  } catch (error) {
    // Handle error
    setError('Failed to post item. Please try again.');
  } finally {
    setLoading(false);
  }
};
```

## Error Handling

```javascript
try {
  // API call
} catch (error) {
  if (!navigator.onLine) {
    // Handle offline state
    showOfflineMessage();
  } else if (error.message.includes('timeout')) {
    // Handle timeout
    showTimeoutMessage();
  } else {
    // Handle general errors
    console.error('API Error:', error);
    showErrorMessage('Something went wrong. Please try again.');
  }
}
```

## Type Definitions

```typescript
// Product interface
interface Product {
  id: string;
  name: string;
  category: string;
  description: string;
  price: string;
  email: string;
  sellerName: string;
  city: string;
  zipcode: string;
  university: string;
  primaryImage: string | null;
  additionalImages: string[];
  postingdate: string;
  productage: string;
  sellingtype: string;
  status: string;
  images: string[];
}

// Filter options interface
interface FilterOptions {
  category?: string;
  sortBy?: 'price_low_high' | 'price_high_low' | 'newest' | 'popularity';
  condition?: string;
  sellingType?: string;
  page?: number;
  size?: number;
  university?: string; // For city search
}
```

## Performance Tips

1. **Image Optimization**: Resize images before uploading (max 1024×1024px)
2. **Caching**: Cache product listings using libraries like react-query
3. **Pagination**: Always use pagination for product listing screens
4. **Load on Demand**: Load images only when they enter the viewport 
5. **Error States**: Always show meaningful error messages with retry options 

## API Reference

### Products

#### `POST /api/products`
Create a new product without images.

**Request Body:**
```json
{
  "name": "Product name",
  "category": "textbooks",
  "description": "Product description",
  "price": "29.99",
  "email": "user@example.com",
  "sellerName": "John Doe", 
  "city": "San Francisco",
  "zipcode": "94107",
  "university": "SUNY Buffalo",
  "productage": "like-new",
  "sellingtype": "sell"
}
```

**Response:** The created product object with HTTP 201

#### `GET /api/products/{id}`
Get a product by ID.

**Response:** The product object with HTTP 200, or HTTP 404 if not found

#### `GET /api/products/user/{email}`
Get all products listed by a user.

**Response:** Array of product objects with HTTP 200

#### `GET /api/products/category/{category}`
Get products by category.

**Response:** Array of product objects with HTTP 200

#### `PUT /api/products/{id}`
Update an existing product.

**Request Body:** Product object with updated fields
**Response:** Updated product object with HTTP 200, or HTTP 404 if not found

#### `PATCH /api/products/{id}/status`
Update just the status of a product.

**Parameters:**
- `status`: New status value (available, sold, etc.)

**Response:** Updated product object with HTTP 200, or HTTP 404 if not found

#### `DELETE /api/products/{id}`
Delete a product.

**Response:**
```json
{
  "message": "Product deleted successfully"
}
```

### Image Handling

#### `POST /api/files/product-images`
Upload multiple product images.

**Request:** `multipart/form-data` with images field (up to 5 files)

**Response:**
```json
{
  "primaryImage": "filename1.jpg",
  "additionalImages": ["filename2.jpg", "filename3.jpg"],
  "message": "Product images uploaded successfully",
  "allFilenames": ["filename1.jpg", "filename2.jpg", "filename3.jpg"]
}
```

#### `POST /api/products/{id}/images`
Add an image to an existing product.

**Parameters:**
- `imageUrl`: The filename of the image to add

**Response:** Updated product object with HTTP 200, or HTTP 404 if not found

#### `DELETE /api/products/{id}/images`
Remove an image from a product.

**Parameters:**
- `imageUrl`: The filename of the image to remove

**Response:** Updated product object with HTTP 200, or HTTP 404 if not found

### University and Location Features

#### `GET /api/products/university/{university}`
Get products filtered by university.

**Query Parameters:**
- `category` (optional): Filter by category
- `sortBy` (optional): Sort by price_low_high, price_high_low, newest, or popularity
- `condition` (optional): Filter by product condition
- `sellingType` (optional): Filter by selling type
- `page` (optional): Page number for pagination (default: 0)
- `size` (optional): Items per page (default: 20)

**Response:**
```json
{
  "products": [...],
  "totalItems": 100,
  "currentPage": 0,
  "totalPages": 5
}
```

#### `GET /api/products/city/{city}`
Get products filtered by city.

**Query Parameters:**
- `university` (optional): Filter by specific university
- `category` (optional): Filter by category
- `sortBy` (optional): Sort by price_low_high, price_high_low, newest, or popularity
- `condition` (optional): Filter by product condition
- `sellingType` (optional): Filter by selling type
- `page` (optional): Page number for pagination (default: 0)
- `size` (optional): Items per page (default: 20)

**Response:** Same format as university endpoint

#### `GET /api/products/nearby-universities/{zipcode}`
Get universities near a zipcode.

**Query Parameters:**
- `radiusInMiles` (optional): Search radius in miles (default: 10)

**Response:**
```json
[
  {
    "name": "San Jose State University",
    "distance": "0.5 miles"
  },
  {
    "name": "Santa Clara University",
    "distance": "2.3 miles"
  }
]
```