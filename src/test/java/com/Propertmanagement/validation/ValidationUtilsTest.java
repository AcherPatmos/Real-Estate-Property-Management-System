package com.Propertmanagement.validation;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

// * Unit tests for the field-level validation rules.
// * These are pure functions with no database dependency; runs from a fresh git clone
class ValidationUtilsTest {

    @Test
    void requireText_nullInput_isRejectedAsRequired() {
        ParsedField<String> result = ValidationUtils.requireText(null, "Name", 10);

        assertFalse(result.isValid());
        assertEquals("Name is required.", result.getError());
    }

    @Test
    void requireText_emptyString_isRejectedAsRequired() {
        ParsedField<String> result = ValidationUtils.requireText("", "Name", 10);

        assertFalse(result.isValid());
        assertEquals("Name is required.", result.getError());
    }

    @Test
    void requireText_whitespaceOnly_isRejectedAsRequired() {
        ParsedField<String> result = ValidationUtils.requireText("     ", "Name", 10);

        assertFalse(result.isValid());
        assertEquals("Name is required.", result.getError());
    }

    @Test
    void requireText_surroundingWhitespace_isTrimmedFromTheAcceptedValue() {
        ParsedField<String> result = ValidationUtils.requireText("  Block A  ", "Name", 10);

        assertTrue(result.isValid());
        assertEquals("Block A", result.getValue());
    }

    @Test
    void requireText_exactlyAtMaxLength_isAccepted() {
        // BOUNDARY: length == maxLength must pass.
        ParsedField<String> result = ValidationUtils.requireText("abcde", "Name", 5);

        assertTrue(result.isValid());
        assertEquals("abcde", result.getValue());
    }

    @Test
    void requireText_onePastMaxLength_isRejected() {
        // BOUNDARY: length == maxLength + 1 must fail.
        ParsedField<String> result = ValidationUtils.requireText("abcdef", "Name", 5);

        assertFalse(result.isValid());
        assertTrue(result.getError().contains("5 characters or fewer"));
    }

    @Test
    void requireText_lengthIsMeasuredAfterTrimming_notBefore() {
        // "abcde" padded to 9 chars is within the limit once trimmed.
        ParsedField<String> result = ValidationUtils.requireText("  abcde  ", "Name", 5);

        assertTrue(result.isValid());
        assertEquals("abcde", result.getValue());
    }


    @Test
    void requirePositiveInt_emptyInput_isRejectedAsRequired() {
        ParsedField<Integer> result = ValidationUtils.requirePositiveInt("  ", "Floor count");

        assertFalse(result.isValid());
        assertEquals("Floor count is required.", result.getError());
    }

    @Test
    void requirePositiveInt_nonNumericInput_isRejected() {
        ParsedField<Integer> result = ValidationUtils.requirePositiveInt("twelve", "Floor count");

        assertFalse(result.isValid());
        assertEquals("Floor count must be a whole number.", result.getError());
    }

    @Test
    void requirePositiveInt_decimalInput_isRejectedBecauseItIsNotAWholeNumber() {
        ParsedField<Integer> result = ValidationUtils.requirePositiveInt("3.5", "Floor count");

        assertFalse(result.isValid());
        assertEquals("Floor count must be a whole number.", result.getError());
    }

    @Test
    void requirePositiveInt_zero_isRejected() {
        // BOUNDARY: zero is the first invalid value going down.
        ParsedField<Integer> result = ValidationUtils.requirePositiveInt("0", "Floor count");

        assertFalse(result.isValid());
        assertEquals("Floor count must be greater than zero.", result.getError());
    }

    @Test
    void requirePositiveInt_one_isAccepted() {
        // BOUNDARY: one is the first valid value going up.
        ParsedField<Integer> result = ValidationUtils.requirePositiveInt("1", "Floor count");

        assertTrue(result.isValid());
        assertEquals(1, result.getValue());
    }

    @Test
    void requirePositiveInt_negative_isRejected() {
        ParsedField<Integer> result = ValidationUtils.requirePositiveInt("-4", "Floor count");

        assertFalse(result.isValid());
        assertEquals("Floor count must be greater than zero.", result.getError());
    }

    // ---------------------------------------------------------------
    // requireInt  (signed: basements are legitimate negative floors)
    // ---------------------------------------------------------------

    @Test
    void requireInt_emptyInput_isRejectedAsRequired() {
        ParsedField<Integer> result = ValidationUtils.requireInt("", "Floor number");

        assertFalse(result.isValid());
        assertEquals("Floor number is required.", result.getError());
    }

    @Test
    void requireInt_nonNumericInput_isRejected() {
        ParsedField<Integer> result = ValidationUtils.requireInt("ground", "Floor number");

        assertFalse(result.isValid());
        assertTrue(result.getError().startsWith("Floor number must be a whole number"));
    }

    @Test
    void requireInt_zero_isAcceptedAsTheGroundFloor() {
        ParsedField<Integer> result = ValidationUtils.requireInt("0", "Floor number");

        assertTrue(result.isValid());
        assertEquals(0, result.getValue());
    }

    @Test
    void requireInt_negative_isAcceptedAsABasementLevel() {
        // This is the rule that distinguishes requireInt from requirePositiveInt.
        ParsedField<Integer> result = ValidationUtils.requireInt("-2", "Floor number");

        assertTrue(result.isValid());
        assertEquals(-2, result.getValue());
    }

    @Test
    void requireMoney_emptyInput_isRejectedAsRequired() {
        ParsedField<BigDecimal> result = ValidationUtils.requireMoney("", "Rent");

        assertFalse(result.isValid());
        assertEquals("Rent is required.", result.getError());
    }

    @Test
    void requireMoney_nonNumericInput_isRejected() {
        ParsedField<BigDecimal> result = ValidationUtils.requireMoney("1,250.00", "Rent");

        assertFalse(result.isValid());
        assertTrue(result.getError().startsWith("Rent must be a valid amount"));
    }

    @Test
    void requireMoney_negativeAmount_isRejected() {
        ParsedField<BigDecimal> result = ValidationUtils.requireMoney("-0.01", "Rent");

        assertFalse(result.isValid());
        assertEquals("Rent cannot be negative.", result.getError());
    }

    @Test
    void requireMoney_zero_isAccepted() {
        // BOUNDARY: zero sits exactly on the lower limit and must pass.
        ParsedField<BigDecimal> result = ValidationUtils.requireMoney("0", "Rent");

        assertTrue(result.isValid());
        assertEquals(new BigDecimal("0.00"), result.getValue());
    }

    @Test
    void requireMoney_acceptedValue_isNormalisedToTwoDecimalPlaces() {
        // The DECIMAL(10,2) column stores two places, so "450" becomes "450.00".
        ParsedField<BigDecimal> result = ValidationUtils.requireMoney("450", "Rent");

        assertTrue(result.isValid());
        assertEquals(new BigDecimal("450.00"), result.getValue());
    }

    @Test
    void requireMoney_thirdDecimalPlace_isRoundedHalfUp() {
        ParsedField<BigDecimal> result = ValidationUtils.requireMoney("100.005", "Rent");

        assertTrue(result.isValid());
        assertEquals(new BigDecimal("100.01"), result.getValue());
    }

    @Test
    void requireMoney_thirdDecimalPlaceBelowHalf_isRoundedDown() {
        ParsedField<BigDecimal> result = ValidationUtils.requireMoney("100.004", "Rent");

        assertTrue(result.isValid());
        assertEquals(new BigDecimal("100.00"), result.getValue());
    }

    @Test
    void requireMoney_exactlyAtTheColumnMaximum_isAccepted() {
        // BOUNDARY: 99999999.99 is the largest value DECIMAL(10,2) holds.
        ParsedField<BigDecimal> result = ValidationUtils.requireMoney("99999999.99", "Rent");

        assertTrue(result.isValid());
        assertEquals(new BigDecimal("99999999.99"), result.getValue());
    }

    @Test
    void requireMoney_onePastTheColumnMaximum_isRejected() {
        // BOUNDARY: one cent past the limit must fail.
        ParsedField<BigDecimal> result = ValidationUtils.requireMoney("100000000.00", "Rent");

        assertFalse(result.isValid());
        assertTrue(result.getError().contains("too large"));
    }

    @Test
    void requireMoney_roundingIsAppliedBeforeTheMaximumCheck() {
        // "99999999.995" rounds UP to 100000000.00, which is over the limit.
        // This asserts the order of operations inside requireMoney: the value
        // is normalised first, then range-checked. Reversing those two steps
        // would let this value through and the INSERT would fail at the DB.
        ParsedField<BigDecimal> result = ValidationUtils.requireMoney("99999999.995", "Rent");

        assertFalse(result.isValid());
        assertTrue(result.getError().contains("too large"));
    }
}