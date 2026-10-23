package com.example.smartpantry;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import com.example.smartpantry.core.*;
import java.io.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;
import org.json.*;

/**
 * Owns the on-device SQLite schema, recipe seeding and pantry CRUD. Call its methods on the
 * application database queue.
 */
public final class DatabaseHelper extends SQLiteOpenHelper {
  private final Context context;

  public DatabaseHelper(Context context) {
    this(context, "smart_pantry.db");
  }

  DatabaseHelper(Context context, String databaseName) {
    super(context, databaseName, null, 1);
    this.context = context.getApplicationContext();
  }

  @Override
  public void onConfigure(SQLiteDatabase db) {
    db.setForeignKeyConstraintsEnabled(true);
  }

  @Override
  public void onCreate(SQLiteDatabase db) {
    // SQLiteOpenHelper wraps database creation + first-run seeding in a transaction.
    db.execSQL(
        "CREATE TABLE pantry_items (_id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL,"
            + " normalized_name TEXT NOT NULL, quantity TEXT NOT NULL, unit TEXT NOT NULL, expiry"
            + " TEXT, draft_key TEXT NOT NULL UNIQUE)");
    db.execSQL("CREATE INDEX pantry_name ON pantry_items(normalized_name)");
    db.execSQL(
        "CREATE TABLE recipes (_id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL UNIQUE,"
            + " description TEXT NOT NULL, minutes INTEGER NOT NULL, servings INTEGER NOT NULL,"
            + " method TEXT NOT NULL)");
    db.execSQL(
        "CREATE TABLE recipe_ingredients (_id INTEGER PRIMARY KEY AUTOINCREMENT, recipe_id INTEGER"
            + " NOT NULL REFERENCES recipes(_id) ON DELETE CASCADE, name TEXT NOT NULL, quantity"
            + " TEXT NOT NULL, unit TEXT NOT NULL, sort_order INTEGER NOT NULL)");
    db.execSQL("CREATE INDEX recipe_children ON recipe_ingredients(recipe_id,sort_order)");
    seedRecipes(db);
  }

  @Override
  public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    // Add explicit ALTER TABLE migrations for future schema versions; never erase pantry data.
    throw new IllegalStateException(
        "No migration is defined from " + oldVersion + " to " + newVersion);
  }

  private void seedRecipes(SQLiteDatabase db) {
    try (InputStream input = context.getAssets().open("recipes.json")) {
      ByteArrayOutputStream output = new ByteArrayOutputStream();
      byte[] buffer = new byte[4096];
      int n;
      while ((n = input.read(buffer)) != -1) output.write(buffer, 0, n);
      JSONArray recipes = new JSONArray(output.toString(StandardCharsets.UTF_8.name()));
      if (recipes.length() < 20) throw new IllegalStateException("Recipe seed is incomplete.");
      for (int i = 0; i < recipes.length(); i++) {
        JSONObject recipe = recipes.getJSONObject(i);
        ContentValues row = new ContentValues();
        row.put("name", recipe.getString("name"));
        row.put("description", recipe.getString("description"));
        row.put("minutes", recipe.getInt("minutes"));
        row.put("servings", recipe.getInt("servings"));
        JSONArray steps = recipe.getJSONArray("steps");
        StringBuilder method = new StringBuilder();
        for (int j = 0; j < steps.length(); j++)
          method.append(j + 1).append(". ").append(steps.getString(j)).append("\n\n");
        row.put("method", method.toString().trim());
        long recipeId = db.insertOrThrow("recipes", null, row);
        JSONArray ingredients = recipe.getJSONArray("ingredients");
        for (int j = 0; j < ingredients.length(); j++) {
          JSONObject ingredient = ingredients.getJSONObject(j);
          ContentValues item = new ContentValues();
          item.put("recipe_id", recipeId);
          item.put("name", InputValidator.name(ingredient.getString("name")));
          item.put(
              "quantity",
              InputValidator.quantity(ingredient.getString("quantity")).toPlainString());
          item.put("unit", UnitConverter.unit(ingredient.getString("unit")).name);
          item.put("sort_order", j);
          db.insertOrThrow("recipe_ingredients", null, item);
        }
      }
    } catch (IOException | JSONException ex) {
      throw new IllegalStateException("Cannot load bundled recipe data.", ex);
    }
  }

  private Ingredient pantryItem(Cursor c) {
    String expiry = c.getString(c.getColumnIndexOrThrow("expiry"));
    return new Ingredient(
        c.getLong(c.getColumnIndexOrThrow("_id")),
        c.getString(c.getColumnIndexOrThrow("name")),
        new BigDecimal(c.getString(c.getColumnIndexOrThrow("quantity"))),
        c.getString(c.getColumnIndexOrThrow("unit")),
        expiry == null ? null : LocalDate.parse(expiry));
  }

  public List<Ingredient> pantry() {
    List<Ingredient> list = new ArrayList<>();
    try (Cursor c =
        getReadableDatabase()
            .query(
                "pantry_items",
                null,
                null,
                null,
                null,
                null,
                "name COLLATE NOCASE ASC, expiry ASC")) {
      while (c.moveToNext()) list.add(pantryItem(c));
    }
    return list;
  }

  public Ingredient ingredient(long id) {
    try (Cursor c =
        getReadableDatabase()
            .query(
                "pantry_items",
                null,
                "_id=?",
                new String[] {String.valueOf(id)},
                null,
                null,
                null)) {
      return c.moveToFirst() ? pantryItem(c) : null;
    }
  }

  public long save(
      long id, String name, BigDecimal quantity, String unit, LocalDate expiry, String draftKey) {
    // Validate again at the persistence boundary, not only inside the form.
    name = InputValidator.name(name);
    quantity = InputValidator.quantity(quantity.toPlainString());
    unit = UnitConverter.unit(unit).name;
    ContentValues row = new ContentValues();
    row.put("name", name);
    row.put("normalized_name", NameNormalizer.normalize(name));
    row.put("quantity", quantity.toPlainString());
    row.put("unit", unit);
    if (expiry == null) row.putNull("expiry");
    else row.put("expiry", expiry.toString());
    SQLiteDatabase db = getWritableDatabase();
    // A stable form key makes repeated create requests after rotation idempotent.
    if (id <= 0)
      try (Cursor c =
          db.query(
              "pantry_items",
              new String[] {"_id"},
              "draft_key=?",
              new String[] {draftKey},
              null,
              null,
              null)) {
        if (c.moveToFirst()) id = c.getLong(0);
      }
    if (id > 0) {
      if (db.update("pantry_items", row, "_id=?", new String[] {String.valueOf(id)}) != 1)
        throw new IllegalStateException("This ingredient no longer exists.");
      return id;
    }
    row.put("draft_key", draftKey);
    return db.insertOrThrow("pantry_items", null, row);
  }

  public void delete(long id) {
    if (getWritableDatabase().delete("pantry_items", "_id=?", new String[] {String.valueOf(id)})
        != 1) throw new IllegalStateException("This ingredient no longer exists.");
  }

  public List<Recipe> recipes() {
    SQLiteDatabase db = getReadableDatabase();
    List<Recipe> list = new ArrayList<>();
    try (Cursor c = db.query("recipes", null, null, null, null, null, "name COLLATE NOCASE ASC")) {
      while (c.moveToNext()) {
        long id = c.getLong(c.getColumnIndexOrThrow("_id"));
        List<Ingredient> ingredients = new ArrayList<>();
        try (Cursor x =
            db.query(
                "recipe_ingredients",
                null,
                "recipe_id=?",
                new String[] {String.valueOf(id)},
                null,
                null,
                "sort_order ASC")) {
          while (x.moveToNext())
            ingredients.add(
                new Ingredient(
                    x.getLong(x.getColumnIndexOrThrow("_id")),
                    x.getString(x.getColumnIndexOrThrow("name")),
                    new BigDecimal(x.getString(x.getColumnIndexOrThrow("quantity"))),
                    x.getString(x.getColumnIndexOrThrow("unit")),
                    null));
        }
        list.add(
            new Recipe(
                id,
                c.getString(c.getColumnIndexOrThrow("name")),
                c.getString(c.getColumnIndexOrThrow("description")),
                c.getInt(c.getColumnIndexOrThrow("minutes")),
                c.getInt(c.getColumnIndexOrThrow("servings")),
                c.getString(c.getColumnIndexOrThrow("method")),
                ingredients));
      }
    }
    return list;
  }

  public Recipe recipe(long id) {
    for (Recipe r : recipes()) if (r.id == id) return r;
    return null;
  }
}
