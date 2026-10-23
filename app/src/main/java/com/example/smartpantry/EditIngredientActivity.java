package com.example.smartpantry;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import com.example.smartpantry.core.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

/**
 * One form serves both Create and Update. All input is checked before SQLite writes, and Delete
 * requires confirmation.
 */
public final class EditIngredientActivity extends BaseActivity {
  public static final String EXTRA_ID = "ingredient_id";
  private long id;
  private String draftKey;
  private EditText name, quantity, expiry;
  private Spinner unit;
  private Button save, delete;

  @Override
  protected void onCreate(Bundle state) {
    super.onCreate(state);
    id = getIntent().getLongExtra(EXTRA_ID, 0);
    draftKey = state == null ? UUID.randomUUID().toString() : state.getString("draft_key");
    page(
        R.layout.activity_edit,
        id > 0 ? "Edit ingredient" : "Add ingredient",
        "Amounts matter. Use the unit you measured.");
    name = findViewById(R.id.ingredient_name);
    quantity = findViewById(R.id.quantity);
    expiry = findViewById(R.id.expiry);
    unit = findViewById(R.id.unit);
    save = findViewById(R.id.save);
    delete = findViewById(R.id.delete);
    ArrayAdapter<String> adapter =
        new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, UnitConverter.UNITS);
    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    unit.setAdapter(adapter);
    String preferred =
        getSharedPreferences("settings", MODE_PRIVATE).getString("default_unit", "g");
    unit.setSelection(Arrays.asList(UnitConverter.UNITS).indexOf(preferred));
    delete.setVisibility(id > 0 ? View.VISIBLE : View.GONE);
    if (id > 0 && state == null) {
      save.setEnabled(false);
      delete.setEnabled(false);
      background(
          () -> database().ingredient(id),
          item -> {
            if (item == null) {
              toast("Ingredient no longer exists.");
              finish();
              return;
            }
            name.setText(item.name);
            quantity.setText(item.quantity.toPlainString());
            expiry.setText(item.expiry == null ? "" : item.expiry.toString());
            unit.setSelection(Arrays.asList(UnitConverter.UNITS).indexOf(item.unit));
            save.setEnabled(true);
            delete.setEnabled(true);
          });
    }
    save.setOnClickListener(v -> save());
    delete.setOnClickListener(
        v ->
            new AlertDialog.Builder(this)
                .setTitle("Delete this ingredient?")
                .setMessage(
                    "This removes this pantry lot. Recipe suggestions will be recalculated.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton(
                    "Delete",
                    (d, w) -> {
                      save.setEnabled(false);
                      delete.setEnabled(false);
                      background(
                          () -> {
                            database().delete(id);
                            return true;
                          },
                          ok -> {
                            toast("Ingredient deleted");
                            finish();
                          });
                    })
                .show());
  }

  private void save() {
    name.setError(null);
    quantity.setError(null);
    expiry.setError(null);
    final String n;
    final BigDecimal q;
    final LocalDate e;
    try {
      n = InputValidator.name(name.getText().toString());
    } catch (IllegalArgumentException ex) {
      name.setError(ex.getMessage());
      name.requestFocus();
      return;
    }
    try {
      q = InputValidator.quantity(quantity.getText().toString());
    } catch (IllegalArgumentException ex) {
      quantity.setError(ex.getMessage());
      quantity.requestFocus();
      return;
    }
    try {
      e = InputValidator.expiry(expiry.getText().toString());
    } catch (IllegalArgumentException ex) {
      expiry.setError(ex.getMessage());
      expiry.requestFocus();
      return;
    }
    String u = (String) unit.getSelectedItem();
    save.setEnabled(false);
    delete.setEnabled(false);
    background(
        () -> database().save(id, n, q, u, e, draftKey),
        newId -> {
          toast(id > 0 ? "Ingredient updated" : "Ingredient added");
          finish();
        });
  }

  @Override
  protected void onSaveInstanceState(Bundle out) {
    out.putString("draft_key", draftKey);
    super.onSaveInstanceState(out);
  }

  @Override
  protected void onOperationFailed() {
    save.setEnabled(true);
    delete.setEnabled(true);
  }
}
