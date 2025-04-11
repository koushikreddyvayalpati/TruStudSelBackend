package com.example.demo.service;

import com.example.demo.model.Product;
import com.example.demo.model.ProductSearchEntry;
import com.example.demo.repository.ProductRepository;
import com.example.demo.repository.ProductSearchRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ProductSearchService {
    private final ProductSearchRepository searchRepository;
    private final ProductRepository productRepository;
    
    // Common words to exclude from indexing
    private static final Set<String> COMMON_WORDS = new HashSet<>(Arrays.asList(
        "the", "and", "for", "with", "this", "that", "is", "in", "to", "of", "a", "an"
    ));
    
    // Common terms to use for searching - provides fallback options
    private static final String[] COMMON_SEARCH_TERMS = {
        "product", "the", "new", "good", "like", "with", "item", "university", "for"
    };
    
    @Autowired
    public ProductSearchService(ProductSearchRepository searchRepository, ProductRepository productRepository) {
        this.searchRepository = searchRepository;
        this.productRepository = productRepository;
    }
    
    /**
     * Index a product for searching
     */
    public void indexProduct(Product product) {
        System.out.println("[INDEXING] Starting index for product ID: " + product.getId() + 
                           ", Name: " + product.getName() + ", University: " + product.getUniversity());
                           
        // Skip indexing if product is not available
        if (!"available".equals(product.getStatus())) {
            System.out.println("[INDEXING] Product status is not available ('" + product.getStatus() + "'). Skipping indexing and deleting existing entries.");
            // We still need to delete if the product becomes unavailable
            searchRepository.deleteEntriesForProduct(product.getId()); 
            return;
        }
        
        // Extract text for indexing (name + description)
        String text = (product.getName() + " " + product.getDescription()).toLowerCase();
        
        // Normalize text (remove accents, etc.)
        text = normalizeText(text);
        
        // Extract individual words, excluding common words
        Set<String> keywords = Arrays.stream(text.split("\\W+"))
            .filter(word -> word.length() >= 3)  // Only index words with 3+ characters
            .filter(word -> !COMMON_WORDS.contains(word))
            .collect(Collectors.toSet());
        System.out.println("[INDEXING] Extracted keywords: " + keywords);
        
        // Use a Map with searchKey as the key to avoid duplicates
        Map<String, ProductSearchEntry> entriesMap = new HashMap<>();
        
        // Add entries for common search terms to improve reliability
        Set<String> enhancedKeywords = new HashSet<>(keywords);
        enhancedKeywords.addAll(Arrays.asList(COMMON_SEARCH_TERMS));
        System.out.println("[INDEXING] Enhanced keywords (with common terms): " + enhancedKeywords);
        
        // Create entries for all keywords
        createSearchEntries(product, enhancedKeywords, entriesMap);
        
        // Add special university-only entries
        addSpecialLocationEntries(product, entriesMap);
        
        // Convert map values to a list for batch saving
        List<ProductSearchEntry> entries = new ArrayList<>(entriesMap.values());
        
        System.out.println("[INDEXING] Total entries to save/upsert for product " + product.getId() + ": " + entries.size());
        // Save all entries in batch (this will upsert)
        searchRepository.batchSaveEntries(entries, product.getId());
        System.out.println("[INDEXING] Successfully saved/upserted " + entries.size() + " search entries for product " + product.getId());
    }
    
    /**
     * Helper method to create search entries for all keywords
     */
    private void createSearchEntries(Product product, Set<String> keywords, Map<String, ProductSearchEntry> entriesMap) {
        for (String keyword : keywords) {
            // Basic keyword entry
            String basicKey = keyword;
            System.out.println("[INDEXING] Adding basic key: '" + basicKey + "'");
            entriesMap.put(basicKey, createSearchEntry(basicKey, product));
            
            // University-specific entry
            if (product.getUniversity() != null && !product.getUniversity().isEmpty()) {
                // Original case
                String uniKey = "UNI:" + product.getUniversity() + ":" + keyword;
                System.out.println("[INDEXING] Adding university key: '" + uniKey + "'");
                entriesMap.put(uniKey, createSearchEntry(uniKey, product));
                
                // Lowercase university
                String uniKeyLower = "UNI:" + product.getUniversity().toLowerCase() + ":" + keyword;
                System.out.println("[INDEXING] Adding lowercase university key: '" + uniKeyLower + "'");
                entriesMap.put(uniKeyLower, createSearchEntry(uniKeyLower, product));
                
                if (product.getCategory() != null && !product.getCategory().isEmpty()) {
                    // Original case
                    String uniCatKey = "UNICAT:" + product.getUniversity() + ":" + product.getCategory() + ":" + keyword;
                    System.out.println("[INDEXING] Adding university+category key: '" + uniCatKey + "'");
                    entriesMap.put(uniCatKey, createSearchEntry(uniCatKey, product));
                    
                    // Lowercase
                    String uniCatKeyLower = "UNICAT:" + product.getUniversity().toLowerCase() + ":" + product.getCategory().toLowerCase() + ":" + keyword;
                    System.out.println("[INDEXING] Adding lowercase university+category key: '" + uniCatKeyLower + "'");
                    entriesMap.put(uniCatKeyLower, createSearchEntry(uniCatKeyLower, product));
                }
            }
            
            // City-specific entry
            if (product.getCity() != null && !product.getCity().isEmpty()) {
                // Original case
                String cityKey = "CITY:" + product.getCity() + ":" + keyword;
                System.out.println("[INDEXING] Adding city key: '" + cityKey + "'");
                entriesMap.put(cityKey, createSearchEntry(cityKey, product));
                
                // Lowercase city
                String cityKeyLower = "CITY:" + product.getCity().toLowerCase() + ":" + keyword;
                System.out.println("[INDEXING] Adding lowercase city key: '" + cityKeyLower + "'");
                entriesMap.put(cityKeyLower, createSearchEntry(cityKeyLower, product));
            }
            
            // Category-specific entry
            if (product.getCategory() != null && !product.getCategory().isEmpty()) {
                // Original case
                String catKey = "CAT:" + product.getCategory() + ":" + keyword;
                System.out.println("[INDEXING] Adding category key: '" + catKey + "'");
                entriesMap.put(catKey, createSearchEntry(catKey, product));
                
                // Lowercase
                String catKeyLower = "CAT:" + product.getCategory().toLowerCase() + ":" + keyword;
                System.out.println("[INDEXING] Adding lowercase category key: '" + catKeyLower + "'");
                entriesMap.put(catKeyLower, createSearchEntry(catKeyLower, product));
            }
        }
    }
    
    /**
     * Helper method to add special location-only entries
     */
    private void addSpecialLocationEntries(Product product, Map<String, ProductSearchEntry> entriesMap) {
        // Add special university-only entries
        if (product.getUniversity() != null && !product.getUniversity().isEmpty()) {
            // Original case
            String uniOnlyKey = "UNI:" + product.getUniversity();
            System.out.println("[INDEXING] Adding university-only key: '" + uniOnlyKey + "'");
            entriesMap.put(uniOnlyKey, createSearchEntry(uniOnlyKey, product));
            
            // Lowercase
            String uniOnlyKeyLower = "UNI:" + product.getUniversity().toLowerCase();
            System.out.println("[INDEXING] Adding lowercase university-only key: '" + uniOnlyKeyLower + "'");
            entriesMap.put(uniOnlyKeyLower, createSearchEntry(uniOnlyKeyLower, product));
            
            if (product.getCategory() != null && !product.getCategory().isEmpty()) {
                // Original case
                String uniCatOnlyKey = "UNICAT:" + product.getUniversity() + ":" + product.getCategory();
                System.out.println("[INDEXING] Adding university+category-only key: '" + uniCatOnlyKey + "'");
                entriesMap.put(uniCatOnlyKey, createSearchEntry(uniCatOnlyKey, product));
                
                // Lowercase
                String uniCatOnlyKeyLower = "UNICAT:" + product.getUniversity().toLowerCase() + ":" + product.getCategory().toLowerCase();
                System.out.println("[INDEXING] Adding lowercase university+category-only key: '" + uniCatOnlyKeyLower + "'");
                entriesMap.put(uniCatOnlyKeyLower, createSearchEntry(uniCatOnlyKeyLower, product));
            }
        }
        
        // Add special city-only entries
        if (product.getCity() != null && !product.getCity().isEmpty()) {
            // Original case
            String cityOnlyKey = "CITY:" + product.getCity();
            System.out.println("[INDEXING] Adding city-only key: '" + cityOnlyKey + "'");
            entriesMap.put(cityOnlyKey, createSearchEntry(cityOnlyKey, product));
            
            // Lowercase
            String cityOnlyKeyLower = "CITY:" + product.getCity().toLowerCase();
            System.out.println("[INDEXING] Adding lowercase city-only key: '" + cityOnlyKeyLower + "'");
            entriesMap.put(cityOnlyKeyLower, createSearchEntry(cityOnlyKeyLower, product));
        }
    }
    
    /**
     * Helper method to create a search entry
     */
    private ProductSearchEntry createSearchEntry(String searchKey, Product product) {
        ProductSearchEntry entry = new ProductSearchEntry();
        entry.setSearchKey(searchKey);
        entry.setProductId(product.getId());
        entry.setProductName(product.getName());
        entry.setCategory(product.getCategory());
        entry.setStatus(product.getStatus());
        entry.setPrice(product.getPrice());
        entry.setUniversity(product.getUniversity());
        entry.setCity(product.getCity());
        entry.setImageUrl(product.getPrimaryImage());
        entry.setPostingDate(product.getPostingdate());
        
        // Set TTL for 90 days from now
        entry.setTtl(Instant.now().plus(90, ChronoUnit.DAYS).getEpochSecond());
        
        return entry;
    }
    
    /**
     * Search for products by keyword with various filters
     */
    public Map<String, Object> searchProducts(
            String keyword, 
            String university, 
            String city,
            String category,
            int page, 
            int size) {
        
        // Validate inputs
        if (keyword == null || (keyword.trim().length() < 3 && !keyword.trim().equals("*"))) {
            throw new IllegalArgumentException("Search term must be at least 3 characters");
        }
        
        // Normalize and prepare the search key
        keyword = normalizeText(keyword).trim();
        String searchKey;
        
        // Special handling for the "*" wildcard
        if (keyword.equals("*")) {
            return handleWildcardSearch(university, city, category, page, size);
        }
        
        // Normal search processing for non-wildcard queries
        // Determine the search key based on filters
        if (university != null && !university.isEmpty()) {
            searchKey = "UNI:" + university + ":" + keyword;
        } else if (city != null && !city.isEmpty()) {
            searchKey = "CITY:" + city + ":" + keyword;
        } else if (category != null && !category.isEmpty()) {
            searchKey = "CAT:" + category + ":" + keyword;
        } else {
            // Direct keyword search
            searchKey = keyword;
        }
        
        return performSearch(searchKey, keyword, university, city, category, page, size);
    }

    /**
     * Handle wildcard (*) search queries
     */
    private Map<String, Object> handleWildcardSearch(String university, String city, String category, int page, int size) {
        System.out.println("Processing wildcard (*) query with: university=" + university + 
                          ", city=" + city + ", category=" + category);
        
        // Try using search table with a valid search key first
        if (university != null && !university.isEmpty()) {
            try {
                System.out.println("Trying to use search table with wildcard university query");
                
                // Use helper method to try different keys
                List<ProductSearchEntry> searchResults = new ArrayList<>();
                
                // Try with different common terms until we find results
                for (String term : COMMON_SEARCH_TERMS) {
                    if (searchResults.isEmpty()) {
                        String searchKey = "UNI:" + university + ":" + term;
                        searchResults = searchRepository.findBySearchKey(searchKey, 100);
                        
                        if (searchResults.isEmpty()) {
                            searchKey = "UNI:" + university.toLowerCase() + ":" + term;
                            searchResults = searchRepository.findBySearchKey(searchKey, 100);
                        }
                        
                        if (!searchResults.isEmpty()) {
                            break;
                        }
                    }
                }
                
                if (!searchResults.isEmpty()) {
                    return processSearchResults(searchResults, category, page, size);
                } else {
                    System.out.println("No results from search table for university wildcard, falling back to repository");
                }
            } catch (Exception e) {
                System.out.println("Error using search table with wildcard: " + e.getMessage());
                // Continue to repository fallback
            }
        }
        
        // Fallback to repository methods
        return handleWildcardWithRepository(university, city, category, page, size);
    }
    
    /**
     * Process search results to filter and paginate
     */
    private Map<String, Object> processSearchResults(List<ProductSearchEntry> searchResults, String category, int page, int size) {
        System.out.println("Found " + searchResults.size() + " entries in search table");
        
        // Extract unique product IDs
        Set<String> productIds = searchResults.stream()
            .map(ProductSearchEntry::getProductId)
            .collect(Collectors.toSet());
        
        // Batch load the full products
        List<Product> matchingProducts = productRepository.batchLoad(new ArrayList<>(productIds));
        System.out.println("Loaded " + matchingProducts.size() + " products from search table");
        
        // Apply category filtering if needed
        if (category != null && !category.isEmpty()) {
            matchingProducts = matchingProducts.stream()
                .filter(p -> category.equals(p.getCategory()))
                .collect(Collectors.toList());
            System.out.println("After category filtering: " + matchingProducts.size() + " products");
        }
        
        // Filter only available products
        matchingProducts = matchingProducts.stream()
            .filter(p -> "available".equals(p.getStatus()))
            .collect(Collectors.toList());
        System.out.println("After status filtering: " + matchingProducts.size() + " available products");
        
        // Sort by posting date (newest first)
        matchingProducts.sort(Comparator.comparing(Product::getPostingdate).reversed());
        
        return paginateResults(matchingProducts, page, size);
    }
    
    /**
     * Fallback to repository for wildcard searches
     */
    private Map<String, Object> handleWildcardWithRepository(String university, String city, String category, int page, int size) {
        // Fallback to repository methods
        List<Product> matchingProducts;
        if (university != null && !university.isEmpty()) {
            matchingProducts = productRepository.findByUniversity(university);
        } else if (city != null && !city.isEmpty()) {
            matchingProducts = productRepository.findByCity(city);
        } else {
            // Without any location filter, get all products
            matchingProducts = productRepository.findAll();
        }
        
        System.out.println("Found " + matchingProducts.size() + " initial products for location");
        
        // Apply category filtering if needed
        if (category != null && !category.isEmpty()) {
            matchingProducts = matchingProducts.stream()
                .filter(p -> category.equals(p.getCategory()))
                .collect(Collectors.toList());
            System.out.println("After category filtering: " + matchingProducts.size() + " products");
        }
        
        // Filter only available products
        matchingProducts = matchingProducts.stream()
            .filter(p -> "available".equals(p.getStatus()))
            .collect(Collectors.toList());
        System.out.println("After status filtering: " + matchingProducts.size() + " available products");
        
        // Sort by posting date (newest first)
        matchingProducts.sort(Comparator.comparing(Product::getPostingdate).reversed());
        
        return paginateResults(matchingProducts, page, size);
    }
    
    /**
     * Perform a search with specific search key and fallbacks
     */
    private Map<String, Object> performSearch(String searchKey, String keyword, String university, String city, String category, int page, int size) {
        // Limit the maximum query size
        int queryLimit = Math.min(size * 5, 100);
        
        // Query the search table
        List<ProductSearchEntry> searchResults = searchRepository.findBySearchKey(searchKey, queryLimit);
        
        // If no results from specific location search, fall back to general keyword search
        if (searchResults.isEmpty() && !searchKey.equals(keyword)) {
            searchResults = searchRepository.findBySearchKey(keyword, queryLimit);
        }
        
        // Extract unique product IDs
        Set<String> productIds = searchResults.stream()
            .map(ProductSearchEntry::getProductId)
            .collect(Collectors.toSet());
        
        // Batch load the full products
        List<Product> matchedProducts = productRepository.batchLoad(new ArrayList<>(productIds));
        
        // Apply additional filtering if needed
        if (category != null && !category.isEmpty() && !searchKey.startsWith("CAT:")) {
            matchedProducts = matchedProducts.stream()
                .filter(p -> category.equals(p.getCategory()))
                .collect(Collectors.toList());
        }
        
        // Sort by posting date (newest first)
        matchedProducts.sort(Comparator.comparing(Product::getPostingdate).reversed());
        
        return paginateResults(matchedProducts, page, size);
    }
    
    /**
     * Helper method to paginate results
     */
    private Map<String, Object> paginateResults(List<Product> products, int page, int size) {
        // Apply pagination
        int totalItems = products.size();
        int startIndex = Math.min(page * size, totalItems);
        int endIndex = Math.min(startIndex + size, totalItems);
        
        List<Product> paginatedProducts = 
            startIndex < endIndex ? products.subList(startIndex, endIndex) : new ArrayList<>();
        
        // Build the response
        Map<String, Object> response = new HashMap<>();
        response.put("products", paginatedProducts);
        response.put("totalItems", totalItems);
        response.put("currentPage", page);
        response.put("totalPages", (int) Math.ceil((double) totalItems / size));
        
        return response;
    }
    
    /**
     * Re-index all existing products
     */
    public void reindexAllProducts() {
        List<Product> allProducts = productRepository.findAll();
        
        for (Product product : allProducts) {
            try {
                indexProduct(product);
            } catch (Exception e) {
                // Log error but continue with next product
                System.err.println("Error indexing product " + product.getId() + ": " + e.getMessage());
            }
        }
    }
    
    /**
     * Helper method to normalize text for search
     */
    private String normalizeText(String text) {
        if (text == null) {
            return "";
        }
        
        // Remove accents and normalize Unicode
        text = Normalizer.normalize(text, Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "");
        
        // Convert to lowercase
        text = text.toLowerCase();
        
        // Replace special characters with spaces
        text = text.replaceAll("[^a-z0-9\\s]", " ");
        
        // Replace multiple spaces with a single space
        text = text.replaceAll("\\s+", " ").trim();
        
        return text;
    }
    
    /**
     * Delete all search entries for a specific product
     * 
     * @param productId The ID of the product to remove from the search index
     */
    public void deleteEntriesForProduct(String productId) {
        if (productId == null || productId.isEmpty()) {
            return;
        }
        
        try {
            // Use the repository to delete all search entries for this product
            searchRepository.deleteEntriesForProduct(productId);
        } catch (Exception e) {
            // Log error but don't throw it
            System.err.println("Error deleting search entries for product " + productId + ": " + e.getMessage());
        }
    }
    
    /**
     * Find products by university with common search strategy
     */
    public Map<String, Object> getProductsByUniversity(String university, String category, int page, int size) {
        System.out.println("Searching for products in university: " + university + " using search table");
        
        try {
            List<ProductSearchEntry> searchResults = findEntriesByUniversity(university);
            
            if (!searchResults.isEmpty()) {
                return processUniversitySearchResults(searchResults, university, category, page, size);
            } else {
                System.out.println("No results found in search table for university: " + university);
                return new HashMap<>();
            }
        } catch (Exception e) {
            System.err.println("Error in getProductsByUniversity: " + e.getMessage());
            e.printStackTrace();
            return new HashMap<>();
        }
    }
    
    /**
     * Helper method to find search entries by university
     */
    private List<ProductSearchEntry> findEntriesByUniversity(String university) {
        List<ProductSearchEntry> searchResults = new ArrayList<>();
        
        // Try multiple common keywords that are likely to be in product names/descriptions
        for (String term : COMMON_SEARCH_TERMS) {
            if (searchResults.isEmpty()) {
                // Original case
                String searchKey = "UNI:" + university + ":" + term;
                System.out.println("Trying search key: " + searchKey);
                searchResults = searchRepository.findBySearchKey(searchKey, 250);
                
                if (searchResults.isEmpty()) {
                    // Lowercase university
                    searchKey = "UNI:" + university.toLowerCase() + ":" + term;
                    System.out.println("Trying lowercase search key: " + searchKey);
                    searchResults = searchRepository.findBySearchKey(searchKey, 250);
                }
                
                if (!searchResults.isEmpty()) {
                    System.out.println("Found results with term: " + term);
                    break;
                }
            }
        }
        
        if (searchResults.isEmpty()) {
            // Last resort - try to match just the university as an exact key
            // This is possible if we've created a special entry during indexing
            String searchKey = "UNI:" + university;
            System.out.println("Trying exact university key: " + searchKey);
            searchResults = searchRepository.findBySearchKey(searchKey, 250);
            
            if (searchResults.isEmpty()) {
                // Try lowercase
                searchKey = "UNI:" + university.toLowerCase();
                System.out.println("Trying exact lowercase university key: " + searchKey);
                searchResults = searchRepository.findBySearchKey(searchKey, 250);
            }
        }
        
        return searchResults;
    }
    
    /**
     * Process search results for university queries
     */
    private Map<String, Object> processUniversitySearchResults(List<ProductSearchEntry> searchResults, String university, String category, int page, int size) {
        System.out.println("Found " + searchResults.size() + " search entries for university: " + university);
        
        // Extract unique product IDs
        Set<String> productIds = searchResults.stream()
            .map(ProductSearchEntry::getProductId)
            .collect(Collectors.toSet());
        
        // Batch load the full products
        List<Product> products = productRepository.batchLoad(new ArrayList<>(productIds));
        System.out.println("Loaded " + products.size() + " unique products from search table");
        
        // Filter by category if needed
        if (category != null && !category.isEmpty()) {
            products = products.stream()
                .filter(p -> category.equals(p.getCategory()))
                .collect(Collectors.toList());
            System.out.println("After category filtering: " + products.size() + " products");
        }
        
        // Filter only available products
        products = products.stream()
            .filter(p -> "available".equals(p.getStatus()))
            .collect(Collectors.toList());
        System.out.println("After status filtering: " + products.size() + " available products");
        
        // Sort by posting date (newest first)
        products.sort(Comparator.comparing(Product::getPostingdate).reversed());
        
        return paginateResults(products, page, size);
    }
    
    /**
     * Get new arrivals for a university using search table
     */
    public List<Product> getNewArrivalsForUniversity(String university, int limit) {
        System.out.println("Getting new arrivals for university: " + university + " using search table");
        
        try {
            List<ProductSearchEntry> searchResults = findEntriesByUniversity(university);
            
            if (!searchResults.isEmpty()) {
                return processNewArrivalsResults(searchResults, university, limit);
            } else {
                System.out.println("No results found in search table for university: " + university);
                return new ArrayList<>();
            }
        } catch (Exception e) {
            System.err.println("Error in getNewArrivalsForUniversity: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
    
    /**
     * Process search results for new arrivals
     */
    private List<Product> processNewArrivalsResults(List<ProductSearchEntry> searchResults, String university, int limit) {
        System.out.println("Found " + searchResults.size() + " search entries for university: " + university);
        
        // Extract unique product IDs
        Set<String> productIds = searchResults.stream()
            .map(ProductSearchEntry::getProductId)
            .collect(Collectors.toSet());
        
        // Batch load the full products
        List<Product> products = productRepository.batchLoad(new ArrayList<>(productIds));
        System.out.println("Loaded " + products.size() + " unique products from search table");
        
        // Filter only available products
        products = products.stream()
            .filter(p -> "available".equals(p.getStatus()))
            .collect(Collectors.toList());
        System.out.println("After status filtering: " + products.size() + " available products");
        
        // Sort by posting date (newest first)
        products.sort(Comparator.comparing(Product::getPostingdate).reversed());
        
        // Apply limit
        if (products.size() > limit) {
            products = products.subList(0, limit);
        }
        
        return products;
    }
} 