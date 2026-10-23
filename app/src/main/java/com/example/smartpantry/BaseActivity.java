package com.example.smartpantry;

import android.app.*;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.*;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

/** Shares bottom navigation and moves database work off the main UI thread for every screen. */
public abstract class BaseActivity extends Activity {
  protected DatabaseHelper database() {
    return ((PantryApp) getApplication()).database;
  }

  protected void page(int layout, String title, String subtitle) {
    setContentView(layout);
    ((TextView) findViewById(R.id.page_title)).setText(title);
    ((TextView) findViewById(R.id.page_subtitle)).setText(subtitle);
    link(R.id.nav_pantry, PantryActivity.class);
    link(R.id.nav_suggested, SuggestedRecipesActivity.class);
    link(R.id.nav_recipes, RecipesActivity.class);
    link(R.id.nav_settings, SettingsActivity.class);
  }

  private void link(int id, Class<?> target) {
    findViewById(id)
        .setOnClickListener(
            v -> {
              if (getClass() != target)
                startActivity(
                    new Intent(this, target)
                        .addFlags(
                            Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP));
            });
  }

  protected <T> void background(Callable<T> task, Consumer<T> success) {
    ((PantryApp) getApplication())
        .databaseQueue.execute(
            () -> {
              try {
                T value = task.call();
                runOnUiThread(
                    () -> {
                      if (!isFinishing() && !isDestroyed()) success.accept(value);
                    });
              } catch (Exception ex) {
                Log.e("SmartPantry", "Database operation failed", ex);
                runOnUiThread(
                    () -> {
                      if (!isFinishing() && !isDestroyed())
                        new AlertDialog.Builder(this)
                            .setTitle("Couldn't complete that action")
                            .setMessage("Your changes could not be completed. " + ex.getMessage())
                            .setPositiveButton("OK", (d, w) -> onOperationFailed())
                            .show();
                    });
              }
            });
  }

  protected void onOperationFailed() {}

  protected void toast(String text) {
    Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
  }

  @Override
  protected void onCreate(Bundle state) {
    super.onCreate(state);
    Log.d("SmartPantryLifecycle", getClass().getSimpleName() + " onCreate");
  }

  @Override
  protected void onResume() {
    super.onResume();
    Log.d("SmartPantryLifecycle", getClass().getSimpleName() + " onResume");
  }

  @Override
  protected void onPause() {
    Log.d("SmartPantryLifecycle", getClass().getSimpleName() + " onPause");
    super.onPause();
  }
}
