package com.example.smartpantry;

import android.content.Context;
import android.view.*;
import android.widget.*;
import com.example.smartpantry.core.Recipe;
import java.util.*;

public final class RecipeAdapter extends BaseAdapter {
  private final Context context;
  private final List<Recipe> recipes;

  public RecipeAdapter(Context c, List<Recipe> r) {
    context = c;
    recipes = r;
  }

  @Override
  public int getCount() {
    return recipes.size();
  }

  @Override
  public Recipe getItem(int p) {
    return recipes.get(p);
  }

  @Override
  public long getItemId(int p) {
    return getItem(p).id;
  }

  @Override
  public View getView(int position, View row, ViewGroup parent) {
    if (row == null) row = LayoutInflater.from(context).inflate(R.layout.row_recipe, parent, false);
    Recipe r = getItem(position);
    ((TextView) row.findViewById(R.id.row_name)).setText(r.name);
    ((TextView) row.findViewById(R.id.row_amount))
        .setText(
            r.minutes
                + " min · "
                + r.servings
                + " serving(s) · "
                + r.ingredients.size()
                + " ingredients");
    ((TextView) row.findViewById(R.id.row_expiry)).setText(r.description);
    return row;
  }
}
