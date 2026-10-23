package com.example.smartpantry;

import android.content.Intent;
import android.os.Bundle;
import android.widget.*;
import com.example.smartpantry.core.*;
import java.time.LocalDate;
import java.util.*;

/**
 * Loads stored recipes and optionally applies the strict matcher before binding a custom ListView
 * adapter.
 */
public abstract class RecipeListActivity extends BaseActivity {
  protected abstract boolean strictOnly();

  @Override
  protected void onCreate(Bundle state) {
    super.onCreate(state);
    page(
        R.layout.activity_recipes,
        strictOnly() ? "Cook right now" : "Recipe collection",
        strictOnly()
            ? "Every ingredient. Enough of each. No shopping needed."
            : "20 recipes stored on your device. This collection is not a list of matches.");
  }

  @Override
  protected void onResume() {
    super.onResume();
    background(
        () -> {
          List<Recipe> recipes = database().recipes();
          if (!strictOnly()) return recipes;
          List<Ingredient> pantry = database().pantry();
          List<Recipe> matched = new ArrayList<>();
          for (Recipe recipe : recipes)
            if (RecipeMatcher.canMake(recipe, pantry, LocalDate.now())) matched.add(recipe);
          return matched;
        },
        recipes -> {
          ((TextView) findViewById(R.id.summary))
              .setText(
                  recipes.size() + (strictOnly() ? " strict match(es)" : " recipes to explore"));
          ((TextView) findViewById(R.id.empty_text))
              .setText(
                  strictOnly()
                      ? "No recipes match your pantry yet. Add more ingredients or check your"
                            + " quantities and units."
                      : "The recipe collection could not be loaded.");
          ListView list = findViewById(R.id.recipe_list);
          RecipeAdapter adapter = new RecipeAdapter(this, recipes);
          list.setAdapter(adapter);
          list.setEmptyView(findViewById(R.id.empty_text));
          list.setOnItemClickListener(
              (parent, view, position, id) ->
                  startActivity(
                      new Intent(this, RecipeDetailActivity.class)
                          .putExtra(RecipeDetailActivity.EXTRA_ID, adapter.getItem(position).id)));
        });
  }
}
