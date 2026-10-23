package com.example.smartpantry.core;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class RecipeMatcher {
  private static String key(Ingredient item) {
    return NameNormalizer.normalize(item.name) + "|" + UnitConverter.unit(item.unit).dimension;
  }

  private static void add(Map<String, BigDecimal> totals, Ingredient item) {
    if (item.quantity == null
        || item.quantity.signum() <= 0
        || NameNormalizer.normalize(item.name).isEmpty())
      throw new IllegalArgumentException("Invalid ingredient quantity or name.");
    totals.merge(key(item), UnitConverter.base(item.quantity, item.unit), BigDecimal::add);
  }

  public static boolean canMake(Recipe recipe, List<Ingredient> pantry, LocalDate today) {
    // Fail closed: an empty or malformed recipe is not a cookable suggestion.
    if (recipe == null || recipe.ingredients.isEmpty()) return false;
    Map<String, BigDecimal> available = new HashMap<>(), required = new HashMap<>();
    try {
      for (Ingredient item : pantry) {
        // Expired lots cannot contribute to usable pantry quantities.
        if (!item.expired(today)) add(available, item);
      }
      // Aggregate BOTH sides so repeated recipe rows cannot reuse the same stock.
      for (Ingredient need : recipe.ingredients) add(required, need);
      for (Map.Entry<String, BigDecimal> need : required.entrySet()) {
        if (available.getOrDefault(need.getKey(), BigDecimal.ZERO).compareTo(need.getValue()) < 0)
          return false;
      }
      return true; // Every required quantity has been satisfied.
    } catch (IllegalArgumentException ex) {
      return false;
    }
  }

  private RecipeMatcher() {}
}
