package com.group8.rbs.validation;

/**
 * Result class to carry validation results with clear error messages.
 */
public class ValidationResult {
    private final boolean valid;
    private final String errorMessage;
    
    private ValidationResult(boolean valid, String errorMessage) {
        this.valid = valid;
        this.errorMessage = errorMessage;
    }
    
    public static ValidationResult success() {
        return new ValidationResult(true, null);
    }
    
    public static ValidationResult failure(String errorMessage) {
        return new ValidationResult(false, errorMessage);
    }
    
    public boolean isValid() {
        return valid;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
}