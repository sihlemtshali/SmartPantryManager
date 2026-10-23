package com.example.smartpantry.core;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class UnitConverter {
  public static final String[] UNITS = {
    "g", "kg", "mg", "ml", "l", "tsp", "tbsp", "cup", "each", "slice", "clove"
  };
  private static final Map<String, Unit> UNITS_BY_NAME = new HashMap<>();

  public static final class Unit {
    public final String name, dimension;
    public final BigDecimal factor;

    Unit(String name, String dimension, String factor) {
      this.name = name;
      this.dimension = dimension;
      this.factor = new BigDecimal(factor);
    }
  }

  static {
    add("g", "mass", "1", "gram", "grams");
    add("kg", "mass", "1000", "kilogram", "kilograms");
    add("mg", "mass", "0.001", "milligram", "milligrams");
    add("ml", "volume", "1", "millilitre", "millilitres", "milliliter", "milliliters");
    add("l", "volume", "1000", "litre", "litres", "liter", "liters");
    add("tsp", "volume", "5", "teaspoon", "teaspoons");
    add("tbsp", "volume", "15", "tablespoon", "tablespoons");
    // This project explicitly uses a 250 ml metric cup, never an unspecified cup.
    add("cup", "volume", "250", "cups");
    add("each", "count", "1", "piece", "pieces", "pc", "pcs", "unit", "units");
    add("slice", "slice", "1", "slices");
    add("clove", "clove", "1", "cloves");
  }

  private static void add(String name, String dimension, String factor, String... aliases) {
    Unit u = new Unit(name, dimension, factor);
    UNITS_BY_NAME.put(name, u);
    for (String a : aliases) UNITS_BY_NAME.put(a, u);
  }

  public static Unit unit(String value) {
    Unit u = UNITS_BY_NAME.get(value == null ? "" : value.trim().toLowerCase(Locale.ROOT));
    if (u == null) throw new IllegalArgumentException("Unsupported unit: " + value);
    return u;
  }

  public static BigDecimal base(BigDecimal quantity, String unit) {
    return quantity.multiply(unit(unit).factor);
  }

  private UnitConverter() {}
}
