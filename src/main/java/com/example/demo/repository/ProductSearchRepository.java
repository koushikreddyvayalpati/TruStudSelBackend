package com.example.demo.repository;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression;
import com.amazonaws.services.dynamodbv2.datamodeling.PaginatedScanList;
import com.amazonaws.services.dynamodbv2.datamodeling.PaginatedQueryList;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.WriteRequest;
import com.amazonaws.services.dynamodbv2.model.ResourceNotFoundException;
import com.example.demo.model.ProductSearchEntry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public class ProductSearchRepository {
    private final DynamoDBMapper dynamoDBMapper;
    
    @Autowired
    public ProductSearchRepository(DynamoDBMapper dynamoDBMapper) {
        this.dynamoDBMapper = dynamoDBMapper;
    }
    
    /**
     * Save a single search entry
     */
    public void save(ProductSearchEntry entry) {
        dynamoDBMapper.save(entry);
    }
    
    /**
     * Save multiple search entries in batch with improved error detection
     */
    public void batchSave(List<ProductSearchEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            return;
        }
        
        System.out.println("[BATCH_SAVE] Attempting to save " + entries.size() + " entries");
        
        // DynamoDB can only process 25 items per batch write
        List<List<ProductSearchEntry>> batches = new ArrayList<>();
        for (int i = 0; i < entries.size(); i += 25) {
            batches.add(entries.subList(i, Math.min(i + 25, entries.size())));
        }
        
        int batchNumber = 0;
        int failedEntries = 0;
        
        for (List<ProductSearchEntry> batch : batches) {
            batchNumber++;
            System.out.println("[BATCH_SAVE] Processing batch " + batchNumber + " of " + batches.size() + " with " + batch.size() + " entries");
            
            try {
                // Save the batch
                List<DynamoDBMapper.FailedBatch> failedBatches = dynamoDBMapper.batchSave(batch);
                
                // Check for failures (unprocessed items)
                if (failedBatches != null && !failedBatches.isEmpty()) {
                    for (DynamoDBMapper.FailedBatch failedBatch : failedBatches) {
                        failedEntries += failedBatch.getUnprocessedItems().size();
                        System.err.println("[BATCH_SAVE] Failed to save " + failedBatch.getUnprocessedItems().size() + 
                                         " items. Error: " + failedBatch.getException().getMessage());
                        
                        // Print the first few unprocessed items for debugging
                        int count = 0;
                        for (Map.Entry<String, List<WriteRequest>> tableEntry : failedBatch.getUnprocessedItems().entrySet()) {
                            String tableName = tableEntry.getKey();
                            for (WriteRequest request : tableEntry.getValue()) {
                                if (count++ < 3) { // Limit to first 3 items for brevity
                                    System.err.println("[BATCH_SAVE] Unprocessed item in table " + tableName + ": " + 
                                                    (request.getPutRequest() != null ? 
                                                     request.getPutRequest().getItem() : "DeleteRequest"));
                                }
                            }
                            if (count >= 3) break;
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("[BATCH_SAVE] Error in batch " + batchNumber + ": " + e.getMessage());
                e.printStackTrace();
                failedEntries += batch.size(); // Assume all failed in this batch
            }
        }
        
        if (failedEntries > 0) {
            System.err.println("[BATCH_SAVE] WARNING: Failed to save " + failedEntries + " out of " + entries.size() + " entries");
        } else {
            System.out.println("[BATCH_SAVE] Successfully saved all " + entries.size() + " entries");
        }
    }
    
    /**
     * Query search entries by searchKey
     */
    public List<ProductSearchEntry> findBySearchKey(String searchKey, int limit) {
        System.out.println("[SEARCH_REPO] Querying for searchKey: '" + searchKey + "' with limit: " + limit);
        Map<String, AttributeValue> eav = new HashMap<>();
        eav.put(":searchKey", new AttributeValue().withS(searchKey));
        
        DynamoDBQueryExpression<ProductSearchEntry> queryExpression = new DynamoDBQueryExpression<ProductSearchEntry>()
            .withKeyConditionExpression("searchKey = :searchKey")
            .withExpressionAttributeValues(eav)
            .withLimit(limit);
        
        try {
            List<ProductSearchEntry> results = dynamoDBMapper.query(ProductSearchEntry.class, queryExpression);
            System.out.println("[SEARCH_REPO] Found " + (results == null ? 0 : results.size()) + " entries for searchKey: '" + searchKey + "'");
            return results == null ? new ArrayList<>() : results;
        } catch (Exception e) {
            System.err.println("[SEARCH_REPO] Error querying DynamoDB for searchKey: '" + searchKey + "'. Error: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>(); // Return empty list on error
        }
    }
    
    /**
     * Delete all search entries for a specific product
     * This implementation avoids using filterExpression on primary key attributes
     */
    public void deleteEntriesForProduct(String productId) {
        if (productId == null || productId.isEmpty()) {
            System.out.println("[DELETE_ENTRIES] Skipping delete operation: productId is null or empty");
            return;
        }
        
        System.out.println("[DELETE_ENTRIES] Starting deletion for product: " + productId);
        int totalEntriesDeleted = 0;
        int failedBatchCount = 0;
        
        try {
            // Use pagination to handle large datasets
            Map<String, AttributeValue> lastEvaluatedKey = null;
            List<ProductSearchEntry> entriesToDelete = new ArrayList<>();
            boolean hasMoreResults = true;
            
            do {
                // Configure scan with pagination
                DynamoDBScanExpression scanExpression = new DynamoDBScanExpression()
                    .withProjectionExpression("searchKey, productId");
                
                if (lastEvaluatedKey != null) {
                    scanExpression.setExclusiveStartKey(lastEvaluatedKey);
                }
                
                // Perform scan for this page
                PaginatedScanList<ProductSearchEntry> scanResult = dynamoDBMapper.scan(ProductSearchEntry.class, scanExpression);
                
                // Properly handle the pagination
                // HasNextPage() and getLastLowLevelResult() aren't available in these list types
                scanResult.loadAllResults(); // This will load all results for the current scan
                hasMoreResults = !scanResult.isEmpty(); // If we got results, check if we need to paginate
                
                // Force the list to be consumed to get the last evaluated key
                if (hasMoreResults) {
                    scanResult.forEach(item -> {}); // Consume the list to ensure lastEvaluatedKey is updated
                    lastEvaluatedKey = scanExpression.getExclusiveStartKey();
                }
                
                // Filter locally to find entries with matching productId
                List<ProductSearchEntry> matchingEntries = new ArrayList<>();
                for (ProductSearchEntry entry : scanResult) {
                    if (productId.equals(entry.getProductId())) {
                        matchingEntries.add(entry);
                    }
                }
                
                entriesToDelete.addAll(matchingEntries);
                
                // If we've collected enough entries, process a batch delete
                if (entriesToDelete.size() >= 25 || !hasMoreResults) {
                    totalEntriesDeleted += processBatchDelete(entriesToDelete, productId, failedBatchCount);
                    entriesToDelete.clear(); // Clear for next batch
                }
                
                // No more pagination if we can't find more results
                if (scanResult.isEmpty()) {
                    hasMoreResults = false;
                }
                
            } while (hasMoreResults);
            
            System.out.println("[DELETE_ENTRIES] Successfully deleted " + totalEntriesDeleted + 
                             " entries for product " + productId);
            
        } catch (Exception e) {
            System.err.println("[DELETE_ENTRIES] Error scanning for entries for product " + 
                             productId + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Helper method to process a batch of deletions
     * Returns the number of successfully deleted entries
     */
    private int processBatchDelete(List<ProductSearchEntry> entries, String productId, int failedBatchCount) {
        if (entries == null || entries.isEmpty()) {
            return 0;
        }
        
        // DynamoDB can only process 25 items per batch delete
        List<List<ProductSearchEntry>> batches = new ArrayList<>();
        for (int i = 0; i < entries.size(); i += 25) {
            batches.add(new ArrayList<>(entries.subList(i, Math.min(i + 25, entries.size()))));
        }
        
        int batchNumber = 0;
        int successfullyDeleted = 0;
        
        for (List<ProductSearchEntry> batch : batches) {
            batchNumber++;
            try {
                System.out.println("[DELETE_ENTRIES] Processing delete batch " + batchNumber + 
                                 " of " + batches.size() + " with " + batch.size() + 
                                 " entries for product " + productId);
                
                // Perform the batch delete
                List<DynamoDBMapper.FailedBatch> failedBatches = dynamoDBMapper.batchDelete(batch);
                
                // Check for failures
                if (failedBatches != null && !failedBatches.isEmpty()) {
                    failedBatchCount++;
                    for (DynamoDBMapper.FailedBatch failedBatch : failedBatches) {
                        int failedCount = failedBatch.getUnprocessedItems().values().stream()
                            .mapToInt(List::size).sum();
                            
                        System.err.println("[DELETE_ENTRIES] Failed to delete " + failedCount + 
                                         " items in batch " + batchNumber + ". Error: " + 
                                         failedBatch.getException().getMessage());
                        
                        // Successfully deleted = batch size - failed count
                        successfullyDeleted += (batch.size() - failedCount);
                    }
                } else {
                    // All items in batch were successfully deleted
                    successfullyDeleted += batch.size();
                }
            } catch (Exception e) {
                failedBatchCount++;
                System.err.println("[DELETE_ENTRIES] Error in delete batch " + batchNumber + 
                                 " for product " + productId + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        return successfullyDeleted;
    }
    
    /**
     * Delete all search entries for a specific product using GSI
     * This implementation uses the GSI on productId which is more efficient
     */
    public void deleteEntriesForProductUsingGSI(String productId) {
        if (productId == null || productId.isEmpty()) {
            System.out.println("[DELETE_ENTRIES_GSI] Skipping delete operation: productId is null or empty");
            return;
        }
        
        System.out.println("[DELETE_ENTRIES_GSI] Starting deletion for product: " + productId);
        int totalEntriesDeleted = 0;
        
        try {
            // Use the GSI to query for entries with a specific productId
            DynamoDBQueryExpression<ProductSearchEntry> queryExpression = new DynamoDBQueryExpression<ProductSearchEntry>()
                .withIndexName("productId-index")
                .withConsistentRead(false)  // Consistent reads not supported on GSI
                .withKeyConditionExpression("productId = :productId")
                .withExpressionAttributeValues(Map.of(":productId", new AttributeValue().withS(productId)));
            
            // Use pagination to handle large result sets
            Map<String, AttributeValue> lastEvaluatedKey = null;
            List<ProductSearchEntry> entriesToDelete = new ArrayList<>();
            boolean hasMoreResults = true;
            
            do {
                // Set pagination for the current query
                if (lastEvaluatedKey != null) {
                    queryExpression.setExclusiveStartKey(lastEvaluatedKey);
                }
                
                // Perform the query for this page
                PaginatedQueryList<ProductSearchEntry> queryResult = dynamoDBMapper.query(ProductSearchEntry.class, queryExpression);
                
                // Properly handle the pagination
                queryResult.loadAllResults(); // This will load all results for the current query
                hasMoreResults = !queryResult.isEmpty(); // If we got results, check if we need to paginate
                
                // Force the list to be consumed to get the last evaluated key
                if (hasMoreResults) {
                    queryResult.forEach(item -> {}); // Consume the list to ensure lastEvaluatedKey is updated
                    lastEvaluatedKey = queryExpression.getExclusiveStartKey();
                }
                
                // Add results to our collection
                for (ProductSearchEntry entry : queryResult) {
                    entriesToDelete.add(entry);
                }
                
                // If we've collected enough entries, process a batch delete
                if (entriesToDelete.size() >= 25 || !hasMoreResults) {
                    totalEntriesDeleted += processBatchDelete(entriesToDelete, productId, 0);
                    entriesToDelete.clear(); // Clear for next batch
                }
                
                // No more pagination if we can't find more results
                if (queryResult.isEmpty()) {
                    hasMoreResults = false;
                }
                
            } while (hasMoreResults);
            
            System.out.println("[DELETE_ENTRIES_GSI] Successfully deleted " + totalEntriesDeleted + 
                             " entries for product " + productId);
            
        } catch (ResourceNotFoundException e) {
            System.out.println("[DELETE_ENTRIES_GSI] GSI not found for productId. The index may not be active yet: " + e.getMessage());
            // Fall back to standard method if GSI is not available
            System.out.println("[DELETE_ENTRIES_GSI] Falling back to scan-based deletion.");
            deleteEntriesForProduct(productId);
        } catch (Exception e) {
            System.err.println("[DELETE_ENTRIES_GSI] Error deleting entries for product " + 
                             productId + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Query search entries by searchKey prefix
     * More efficient for retrieving all entries for a university or city
     */
    public List<ProductSearchEntry> findBySearchKeyPrefix(String searchKeyPrefix, int limit) {
        Map<String, AttributeValue> eav = new HashMap<>();
        eav.put(":prefix", new AttributeValue().withS(searchKeyPrefix));
        
        // The begins_with function must be used with the partition key
        // For DynamoDB, the key condition expression must be on the partition key
        DynamoDBQueryExpression<ProductSearchEntry> queryExpression = new DynamoDBQueryExpression<ProductSearchEntry>()
            .withKeyConditionExpression("searchKey = :prefix or begins_with(searchKey, :prefix)")
            .withExpressionAttributeValues(eav)
            .withLimit(limit);
        
        try {
            return dynamoDBMapper.query(ProductSearchEntry.class, queryExpression);
        } catch (Exception e) {
            // If the begins_with expression fails, fall back to exact match only
            System.err.println("Error using begins_with in query: " + e.getMessage());
            
            // Try with exact match only
            queryExpression = new DynamoDBQueryExpression<ProductSearchEntry>()
                .withKeyConditionExpression("searchKey = :prefix")
                .withExpressionAttributeValues(eav)
                .withLimit(limit);
                
            return dynamoDBMapper.query(ProductSearchEntry.class, queryExpression);
        }
    }
    
    /**
     * Batch save a list of search entries with retry logic
     * More efficient than saving entries one by one
     */
    public void batchSaveEntries(List<ProductSearchEntry> entries, String productId) {
        if (entries == null || entries.isEmpty()) {
            System.out.println("[BATCH_SAVE] No entries to save for product: " + productId);
            return;
        }

        System.out.println("[BATCH_SAVE] Saving " + entries.size() + " entries for product: " + productId);
        
        // Ensure we don't have duplicate search keys
        Map<String, ProductSearchEntry> uniqueEntries = new HashMap<>();
        for (ProductSearchEntry entry : entries) {
            uniqueEntries.put(entry.getSearchKey(), entry);
        }
        
        // Convert back to list with only unique entries
        List<ProductSearchEntry> dedupedEntries = new ArrayList<>(uniqueEntries.values());
        System.out.println("[BATCH_SAVE] After deduplication: " + dedupedEntries.size() + " unique entries (removed " + 
                         (entries.size() - dedupedEntries.size()) + " duplicates)");
        
        // Process in batches of 25 (DynamoDB limit for batch operations)
        int batchSize = 25;
        int successCount = 0;
        
        for (int i = 0; i < dedupedEntries.size(); i += batchSize) {
            int toIndex = Math.min(i + batchSize, dedupedEntries.size());
            List<ProductSearchEntry> batch = dedupedEntries.subList(i, toIndex);
            
            try {
                List<DynamoDBMapper.FailedBatch> failedBatches = dynamoDBMapper.batchSave(batch);
                
                if (failedBatches.isEmpty()) {
                    successCount += batch.size();
                } else {
                    System.err.println("[BATCH_SAVE] Failed to save some entries: " + 
                        failedBatches.size() + " failed batches for product " + productId);
                    
                    // Retry individual saves for failed items
                    for (DynamoDBMapper.FailedBatch failedBatch : failedBatches) {
                        System.err.println("[BATCH_SAVE] Failure reason: " + failedBatch.getException().getMessage());
                        
                        // Try to salvage what we can by saving one by one
                        for (ProductSearchEntry entry : batch) {
                            try {
                                dynamoDBMapper.save(entry);
                                successCount++;
                                System.out.println("[BATCH_SAVE] Successfully saved individual entry with key: " + entry.getSearchKey());
                            } catch (Exception ex) {
                                System.err.println("[BATCH_SAVE] Failed to save individual entry with key: " + 
                                                entry.getSearchKey() + ", Error: " + ex.getMessage());
                            }
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("[BATCH_SAVE] Error during batch save for product " + 
                    productId + ": " + e.getMessage());
                
                // Try to save entries individually
                for (ProductSearchEntry entry : batch) {
                    try {
                        dynamoDBMapper.save(entry);
                        successCount++;
                    } catch (Exception ex) {
                        System.err.println("[BATCH_SAVE] Failed to save individual entry: " + ex.getMessage());
                    }
                }
            }
        }
        
        System.out.println("[BATCH_SAVE] Successfully saved " + successCount + "/" + 
            dedupedEntries.size() + " entries for product " + productId);
    }
} 