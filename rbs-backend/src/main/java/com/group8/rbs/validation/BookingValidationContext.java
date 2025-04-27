package com.group8.rbs.validation;

import com.group8.rbs.entities.Facility;

/**
 * ThreadLocal context to pass data between validators.
 */
public class BookingValidationContext {
    private static final ThreadLocal<Facility> facilityContext = new ThreadLocal<>();
    
    public static void setFacility(Facility facility) {
        facilityContext.set(facility);
    }
    
    public static Facility getFacility() {
        return facilityContext.get();
    }
    
    public static void clear() {
        facilityContext.remove();
    }
}