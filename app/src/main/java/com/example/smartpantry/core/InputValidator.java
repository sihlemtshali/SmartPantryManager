package com.example.smartpantry.core;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

public final class InputValidator {
  public static String name(String raw) {
    String value = raw == null ? "" : raw.trim().replaceAll("\\s+", " ");
    if (value.length() < 2 || value.length() > 60 || !value.matches(".*\\p{L}.*"))
      throw new IllegalArgumentException(
          "Enter an ingredient name of 2-60 characters containing letters.");
    return value;
  }

  public static BigDecimal quantity(String raw) {
    String value = raw == null ? "" : raw.trim();
    // Do not guess whether a comma means a decimal separator or thousands.
    if (!value.matches("[0-9]+(\\.[0-9]{1,6})?"))
      throw new IllegalArgumentException(
          "Enter a number using a decimal point, for example 0.5 (up to 6 decimal places).");
    BigDecimal q = new BigDecimal(value);
    if (q.signum() <= 0 || q.compareTo(new BigDecimal("1000000")) > 0)
      throw new IllegalArgumentException(
          "Quantity must be greater than 0 and no more than 1,000,000.");
    return q;
  }

  public static LocalDate expiry(String raw) {
    if (raw == null || raw.trim().isEmpty()) return null;
    String value = raw.trim();
    if (!value.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}"))
      throw new IllegalArgumentException("Use YYYY-MM-DD for expiry, or leave it empty.");
    try {
      return LocalDate.parse(value);
    } catch (DateTimeParseException ex) {
      throw new IllegalArgumentException("Enter a real calendar date, for example 2027-01-31.");
    }
  }

  private InputValidator() {}
}
