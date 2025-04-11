package com.example.demo.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

/**
 * Configuration for feature toggles
 * Used to control gradual rollout of new implementations
 */
@Component
public class FeatureToggleConfig {

    @Value("${feature.search_table.enabled:false}")
    private boolean searchTableEnabled;
    
    @Value("${feature.search_table.university_filter_percentage:0}")
    private int universityFilterPercentage;
    
    @Value("${feature.search_table.city_filter_percentage:0}")
    private int cityFilterPercentage;
    
    @Value("${feature.search_table.category_filter_percentage:0}")
    private int categoryFilterPercentage;
    
    @Value("${feature.search_table.featured_percentage:0}")
    private int featuredPercentage;
    
    @Value("${feature.search_table.new_arrivals_percentage:0}")
    private int newArrivalsPercentage;
    
    /**
     * Check if search table should be used for university filtering
     */
    public boolean useSearchTableForUniversity() {
        if (!searchTableEnabled) {
            return false;
        }
        
        if (universityFilterPercentage >= 100) {
            return true;
        }
        
        return Math.random() * 100 < universityFilterPercentage;
    }
    
    /**
     * Check if search table should be used for city filtering
     */
    public boolean useSearchTableForCity() {
        if (!searchTableEnabled) {
            return false;
        }
        
        if (cityFilterPercentage >= 100) {
            return true;
        }
        
        return Math.random() * 100 < cityFilterPercentage;
    }
    
    /**
     * Check if search table should be used for category filtering
     */
    public boolean useSearchTableForCategory() {
        if (!searchTableEnabled) {
            return false;
        }
        
        if (categoryFilterPercentage >= 100) {
            return true;
        }
        
        return Math.random() * 100 < categoryFilterPercentage;
    }
    
    /**
     * Check if search table should be used for featured products
     */
    public boolean useSearchTableForFeatured() {
        if (!searchTableEnabled) {
            return false;
        }
        
        if (featuredPercentage >= 100) {
            return true;
        }
        
        return Math.random() * 100 < featuredPercentage;
    }
    
    /**
     * Check if search table should be used for new arrivals
     */
    public boolean useSearchTableForNewArrivals() {
        if (!searchTableEnabled) {
            return false;
        }
        
        if (newArrivalsPercentage >= 100) {
            return true;
        }
        
        return Math.random() * 100 < newArrivalsPercentage;
    }
} 