package com.verdissia.dto;

public  class ValidationResult {
    private final boolean valid;
    private final String errors;

    public ValidationResult(boolean valid, String errors) {
        this.valid = valid;
        this.errors = errors;
    }

    public boolean isValid() {
        return valid;
    }

    public String getErrors() {
        return errors;
    }
}