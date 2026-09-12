package com.Propertmanagement.validation;

import java.math.BigDecimal;
import java.math.RoundingMode;

// * Field-level validation shared by the CRUD screens.
public final class ValidationUtils {

    // DECIMAL(10,2)
    private static final BigDecimal MAX_MONEY = new BigDecimal("99999999.99");

    // Static-only utility class
    private ValidationUtils() {
    }

    // Used for Name/Address/Unit-number style fields
    public static ParsedField<String> requireText(String rawValue, String fieldName, int maxLength) {
        String trimmed = rawValue == null ? "" : rawValue.trim();
        if (trimmed.isEmpty()) {
            return ParsedField.error(fieldName + " is required.");
        }
        if (trimmed.length() > maxLength) {
            return ParsedField.error(fieldName + " must be " + maxLength
                    + " characters or fewer (currently " + trimmed.length() + ").");
        }
        return ParsedField.ok(trimmed);
    }

    public static ParsedField<Integer> requirePositiveInt(String rawValue, String fieldName) {
        String trimmed = rawValue == null ? "" : rawValue.trim();
        if (trimmed.isEmpty()) {
            return ParsedField.error(fieldName + " is required.");
        }
        int parsed;
        try {
            parsed = Integer.parseInt(trimmed);
        } catch (NumberFormatException e) {
            return ParsedField.error(fieldName + " must be a whole number.");
        }
        if (parsed <= 0) {
            return ParsedField.error(fieldName + " must be greater than zero.");
        }
        return ParsedField.ok(parsed);
    }

    // Plain integer, no sign restriction
    public static ParsedField<Integer> requireInt(String rawValue, String fieldName) {
        String trimmed = rawValue == null ? "" : rawValue.trim();
        if (trimmed.isEmpty()) {
            return ParsedField.error(fieldName + " is required.");
        }
        try {
            return ParsedField.ok(Integer.parseInt(trimmed));
        } catch (NumberFormatException e) {
            return ParsedField.error(fieldName + " must be a whole number (e.g. 1, 0, or -1 for a basement).");
        }
    }

    // Used for rent amount parses as BigDecimal (not double) to avoid float rounding on currency, and enforces the DECIMAL(10,2) column limits
    public static ParsedField<BigDecimal> requireMoney(String rawValue, String fieldName) {
        String trimmed = rawValue == null ? "" : rawValue.trim();
        if (trimmed.isEmpty()) {
            return ParsedField.error(fieldName + " is required.");
        }
        BigDecimal parsed;
        try {
            parsed = new BigDecimal(trimmed);
        } catch (NumberFormatException e) {
            return ParsedField.error(fieldName + " must be a valid amount (e.g. 1250.00).");
        }
        if (parsed.compareTo(BigDecimal.ZERO) < 0) {
            return ParsedField.error(fieldName + " cannot be negative.");
        }
        // Normalize to exactly 2 decimal places (rounding half-up) before the max-value check, so e.g. "100.005" is judged as "100.01"
        parsed = parsed.setScale(2, RoundingMode.HALF_UP);
        if (parsed.compareTo(MAX_MONEY) > 0) {
            return ParsedField.error(fieldName + " is too large (max " + MAX_MONEY + ").");
        }
        return ParsedField.ok(parsed);
    }
}