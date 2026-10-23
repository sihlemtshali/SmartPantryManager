package com.example.smartpantry;

import android.content.Intent;
import android.os.Bundle;
import android.widget.*;

/**
 * Launch screen: refresh the persisted pantry when the Activity becomes visible and pass item IDs
 * to the form.
 */
public final class PantryActivity extends BaseActivity {
  @Override
  protected void onCreate(Bundle state) {
    super.onCreate(state);
    page(
        R.layout.activity_pantry,
        "Your pantry",
        "Keep track of what you have. Cook with what is already here.");
    findViewById(R.id.add_ingredient)
        .setOnClickListener(v -> startActivity(new Intent(this, EditIngredientActivity.class)));
  }

  @Override
  protected void onResume() {
    super.onResume();
    refresh();
  }

  private void refresh() {
    background(
        () -> database().pantry(),
        items -> {
          boolean alerts =
              getSharedPreferences("settings", MODE_PRIVATE).getBoolean("expiry_alerts", true);
          ListView list = findViewById(R.id.pantry_list);
          PantryAdapter adapter = new PantryAdapter(this, items, alerts);
          list.setAdapter(adapter);
          list.setEmptyView(findViewById(R.id.empty_text));
          ((TextView) findViewById(R.id.summary))
              .setText(
                  items.size()
                      + " pantry lot(s) · "
                      + items.stream().filter(x -> x.expired(java.time.LocalDate.now())).count()
                      + " expired");
          list.setOnItemClickListener(
              (parent, view, position, id) ->
                  startActivity(
                      new Intent(this, EditIngredientActivity.class)
                          .putExtra(
                              EditIngredientActivity.EXTRA_ID, adapter.getItem(position).id)));
        });
  }
}
