package com.example.smartpantry.core;

import java.util.ArrayList;
import java.util.List;

public final class Recipe {
  public final long id;
  public final String name, description, method;
  public final int minutes, servings;
  public final List<Ingredient> ingredients;

  public Recipe(
      long id,
      String name,
      String description,
      int minutes,
      int servings,
      String method,
      List<Ingredient> ingredients) {
    this.id = id;
    this.name = name;
    this.description = description;
    this.minutes = minutes;
    this.servings = servings;
    this.method = method;
    this.ingredients = new ArrayList<>(ingredients);
  }
}
