package com.example.smartpantry.core;

import java.math.BigDecimal;
import java.time.LocalDate;

public final class Ingredient {
  public final long id;
  public final String name, unit;
  public final BigDecimal quantity;
  public final LocalDate expiry;

  public Ingredient(long id, String name, BigDecimal quantity, String unit, LocalDate expiry) {
    this.id = id;
    this.name = name;
    this.quantity = quantity;
    this.unit = unit;
    this.expiry = expiry;
  }

  public boolean expired(LocalDate today) {
    return expiry != null && expiry.isBefore(today);
  }

  public String amount() {
    return quantity.stripTrailingZeros().toPlainString() + " " + unit;
  }
}
