package com.Propertmanagement.validation;

public final class ParsedField<T> {

    private final T value;
    private final String error;

    // Private constructor forces callers through ok()/error()
    private ParsedField(T value, String error) {
        this.value = value;
        this.error = error;
    }

    // Wraps a successfully parsed value; error is implicitly null
    public static <T> ParsedField<T> ok(T value) {
        return new ParsedField<>(value, null);
    }

    // Wraps a validation failure however callers must check isValid() before trusting getValue()
    public static <T> ParsedField<T> error(String message) {
        return new ParsedField<>(null, message);
    }

    // True when this is an ok() result; false when it's an error() result
    public boolean isValid() {
        return error == null;
    }

    // Only meaningful when isValid() is true
    public T getValue() {
        return value;
    }

    // Only meaningful when isValid() is false
    public String getError() {
        return error;
    }
}