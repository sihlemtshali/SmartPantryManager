package com.example.smartpantry;

import android.os.Bundle;
import android.widget.TextView;
import com.example.smartpantry.core.*;
import java.time.LocalDate;
import java.util.*;

/** Reads a recipe ID from its Intent and shows current requirements, instructions and readiness. */
public final class RecipeDetailActivity extends BaseActivity {
  public static final String EXTRA_ID = "recipe_id";

  private static final class Detail {
    final Recipe recipe;
    final boolean matched;

    Detail(Recipe r, boolean m) {
      recipe = r;
      matched = m;
    }
  }

  @Override
  protected void onCreate(Bundle state) {
    super.onCreate(state);
    page(
        R.layout.activity_detail,
        "Recipe details",
        "Check the full amounts and preparation steps.");
  }

  @Override
  protected void onResume() {
    super.onResume();
    long id = getIntent().getLongExtra(EXTRA_ID, -1);
    background(
        () -> {
          Recipe r = database().recipe(id);
          return new Detail(
              r, r != null && RecipeMatcher.canMake(r, database().pantry(), LocalDate.now()));
        },
        detail -> {
          Recipe recipe = detail.recipe;
          if (recipe == null) {
            toast("Recipe not found.");
            finish();
            return;
          }
          ((TextView) findViewById(R.id.recipe_name)).setText(recipe.name);
          ((TextView) findViewById(R.id.recipe_meta))
              .setText(recipe.minutes + " minutes · " + recipe.servings + " serving(s)");
          ((TextView) findViewById(R.id.match_status))
              .setText(
                  detail.matched
                      ? "READY TO COOK · all quantities available"
                      : "COLLECTION VIEW · not currently a pantry match");
          StringBuilder ingredients = new StringBuilder();
          for (Ingredient item : recipe.ingredients)
            ingredients
                .append("• ")
                .append(item.name)
                .append(" — ")
                .append(item.amount())
                .append("\n\n");
          ((TextView) findViewById(R.id.recipe_ingredients)).setText(ingredients.toString().trim());
          ((TextView) findViewById(R.id.recipe_method)).setText(recipe.method);
        });
  }
}
