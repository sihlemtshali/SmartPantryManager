package com.example.smartpantry.core;

import java.text.Normalizer;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class NameNormalizer {
  private static final Map<String, String> ALIASES = new HashMap<>();

  static {
    alias("tomato", "tomatoes", "tomatos");
    alias("potato", "potatoes", "potatos");
    alias("onion", "onions");
    alias("egg", "eggs");
    alias("banana", "bananas");
    alias("carrot", "carrots");
    alias("apple", "apples");
    alias("mushroom", "mushrooms");
    alias("oats", "oat", "rolled oats");
    alias("olive oil", "olive oils");
    alias("bell pepper", "bell peppers", "capsicum", "capsicums");
    alias("chickpeas", "chickpea");
    alias("canned chickpeas", "canned chickpea", "tinned chickpeas");
    alias("canned beans", "canned bean", "tinned beans");
    alias("yogurt", "yoghurt");
    alias("lemon juice", "lemonjuice");
  }

  private static void alias(String canonical, String... names) {
    ALIASES.put(canonical, canonical);
    for (String n : names) ALIASES.put(n, canonical);
  }

  public static String normalize(String name) {
    if (name == null) return "";
    String cleaned =
        Normalizer.normalize(name, Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "")
            .toLowerCase(Locale.ROOT)
            .replace('-', ' ')
            .trim()
            .replaceAll("\\s+", " ");
    // A curated alias list avoids treating unrelated foods as interchangeable.
    // For example rice and cooked rice MUST remain different ingredients.
    return ALIASES.getOrDefault(cleaned, cleaned);
  }

  private NameNormalizer() {}
}
