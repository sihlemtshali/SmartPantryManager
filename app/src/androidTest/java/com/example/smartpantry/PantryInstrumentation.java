package com.example.smartpantry;

import android.app.*;
import android.content.*;
import android.graphics.Bitmap;
import android.os.*;
import android.view.View;
import android.widget.*;
import com.example.smartpantry.core.*;
import java.io.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

/**
 * Device checks without third-party Android test libraries. Default run uses a separate disposable
 * SQLite database and never clears the pantry. -e evidence true additionally exercises UI on a
 * FRESH emulator and adds/removes test data.
 */
public final class PantryInstrumentation extends Instrumentation {
  private int count;
  private boolean evidence;

  @Override
  public void onCreate(Bundle args) {
    super.onCreate(args);
    evidence = args != null && "true".equals(args.getString("evidence"));
    start();
  }

  private void check(boolean ok, String label) {
    if (!ok) throw new AssertionError(label);
    count++;
    android.util.Log.i("PantryChecks", "PASS " + label);
    Bundle progress = new Bundle();
    progress.putString("stream", "PASS " + label + "\n");
    sendStatus(0, progress);
  }

  @Override
  public void onStart() {
    Bundle results = new Bundle();
    String testDb = "instrumentation_checks.db";
    try {
      Context context = getTargetContext();
      context.deleteDatabase(testDb);
      DatabaseHelper db = new DatabaseHelper(context, testDb);
      check(db.recipes().size() == 20, "Twenty recipes seeded in SQLite");
      check(db.pantry().isEmpty(), "New pantry initially empty");
      long id = db.save(0, "Tomatoes", new BigDecimal("2"), "each", null, "test-create");
      check(db.ingredient(id) != null, "Create then read pantry row");
      check(db.ingredient(id).name.equals("Tomatoes"), "Name persisted");
      db.save(id, "Tomatoes", new BigDecimal("3"), "each", LocalDate.of(2030, 1, 1), "test-create");
      check(
          db.ingredient(id).quantity.compareTo(new BigDecimal("3")) == 0,
          "Update quantity persisted");
      check(db.ingredient(id).expiry.equals(LocalDate.of(2030, 1, 1)), "Optional expiry persisted");
      long again = db.save(0, "Tomatoes", new BigDecimal("4"), "each", null, "test-create");
      check(
          again == id && db.pantry().size() == 1,
          "Stable form key prevents repeated-create duplicates");
      db.close();
      db = new DatabaseHelper(context, testDb);
      check(
          db.pantry().size() == 1 && db.ingredient(id).quantity.intValue() == 4,
          "Pantry survives closing and reopening SQLite");
      check(db.recipes().size() == 20, "Reopening does not duplicate seeded recipes");
      db.delete(id);
      check(db.pantry().isEmpty(), "Delete persisted");
      Recipe skillet = null;
      for (Recipe recipe : db.recipes())
        if (recipe.name.equals("Tomato egg skillet")) skillet = recipe;
      check(
          skillet != null && skillet.ingredients.size() == 5,
          "Five-ingredient demonstration recipe seeded");
      List<Ingredient> items = new ArrayList<>();
      items.add(item("eggs", "2", "each"));
      items.add(item("tomatoes", "2", "each"));
      items.add(item("onion", "1", "each"));
      items.add(item("olive oil", "1", "tbsp"));
      check(
          !RecipeMatcher.canMake(skillet, items, LocalDate.now()),
          "Real seeded recipe rejects 4 of 5");
      items.add(item("salt", "1", "g"));
      check(
          RecipeMatcher.canMake(skillet, items, LocalDate.now()),
          "Real seeded recipe accepts all five");
      db.close();
      context.deleteDatabase(testDb);
      if (evidence) captureEvidence();
      results.putString("stream", count + " on-device checks passed.\n");
      results.putInt("checks_passed", count);
      finish(Activity.RESULT_OK, results);
    } catch (Throwable ex) {
      results.putString("stream", "FAIL: " + ex + "\n" + android.util.Log.getStackTraceString(ex));
      finish(Activity.RESULT_CANCELED, results);
    } finally {
      getTargetContext().deleteDatabase(testDb);
    }
  }

  private Ingredient item(String name, String quantity, String unit) {
    return new Ingredient(0, name, new BigDecimal(quantity), unit, null);
  }

  private Activity open(Class<?> type) {
    return startActivitySync(
        new Intent(getTargetContext(), type).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
  }

  private Activity openEdit(long id) {
    return startActivitySync(
        new Intent(getTargetContext(), EditIngredientActivity.class)
            .putExtra(EditIngredientActivity.EXTRA_ID, id)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
  }

  private void waitFor(java.util.function.BooleanSupplier condition) throws Exception {
    for (int i = 0; i < 100; i++) {
      waitForIdleSync();
      final boolean[] ready = {false};
      runOnMainSync(() -> ready[0] = condition.getAsBoolean());
      if (ready[0]) return;
      SystemClock.sleep(100);
    }
    throw new AssertionError("Timed out waiting for screen state");
  }

  private void screenshot(String name) throws Exception {
    waitForIdleSync();
    dismissEmulatorSystemDialog();
    SystemClock.sleep(300);
    Bitmap bitmap = getUiAutomation().takeScreenshot();
    if (bitmap == null) throw new AssertionError("No screenshot");
    File directory = new File(getTargetContext().getExternalFilesDir(null), "evidence");
    if (!directory.exists() && !directory.mkdirs())
      throw new IOException("Cannot create evidence directory");
    try (FileOutputStream out = new FileOutputStream(new File(directory, name + ".png"))) {
      bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
    }
    bitmap.recycle();
  }

  private void dismissEmulatorSystemDialog() {
    // Slow software emulators may show a System UI startup ANR. Dismiss only
    // that OS dialog through its real Wait button; never mask an app error.
    android.view.accessibility.AccessibilityNodeInfo root =
        getUiAutomation().getRootInActiveWindow();
    if (root != null && !root.findAccessibilityNodeInfosByText("System UI").isEmpty()) {
      for (android.view.accessibility.AccessibilityNodeInfo node :
          root.findAccessibilityNodeInfosByViewId("android:id/aerr_wait"))
        node.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK);
      SystemClock.sleep(500);
      waitForIdleSync();
    }
  }

  private void waitForScreen(String title) throws Exception {
    for (int i = 0; i < 100; i++) {
      dismissEmulatorSystemDialog();
      android.view.accessibility.AccessibilityNodeInfo root =
          getUiAutomation().getRootInActiveWindow();
      if (root != null && !root.findAccessibilityNodeInfosByText(title).isEmpty()) return;
      SystemClock.sleep(100);
    }
    throw new AssertionError("Expected visible screen: " + title);
  }

  private void captureEvidence() throws Exception {
    android.accessibilityservice.AccessibilityServiceInfo info = getUiAutomation().getServiceInfo();
    info.flags |= android.accessibilityservice.AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS;
    getUiAutomation().setServiceInfo(info);
    DatabaseHelper db = ((PantryApp) getTargetContext().getApplicationContext()).database;
    if (!db.pantry().isEmpty())
      throw new IllegalStateException(
          "Evidence run requires an empty pantry on a fresh emulator. It will not erase existing"
              + " data.");
    dismissEmulatorSystemDialog();
    Activity pantry = open(PantryActivity.class);
    waitFor(() -> ((ListView) pantry.findViewById(R.id.pantry_list)).getAdapter() != null);
    screenshot("01_empty_pantry");
    Activity edit = open(EditIngredientActivity.class);
    screenshot("02_add_ingredient");
    runOnMainSync(() -> edit.findViewById(R.id.save).performClick());
    screenshot("03_validation_error");
    runOnMainSync(
        () -> {
          ((EditText) edit.findViewById(R.id.ingredient_name)).setText("eggs");
          ((EditText) edit.findViewById(R.id.quantity)).setText("2");
          ((Spinner) edit.findViewById(R.id.unit))
              .setSelection(Arrays.asList(UnitConverter.UNITS).indexOf("each"));
          edit.findViewById(R.id.save).performClick();
        });
    waitFor(() -> edit.isFinishing() || edit.isDestroyed());
    waitFor(() -> ((ListView) pantry.findViewById(R.id.pantry_list)).getAdapter().getCount() == 1);
    screenshot("04_ingredient_created");
    long eggId = db.pantry().get(0).id;
    Activity change = openEdit(eggId);
    waitFor(
        () ->
            !((EditText) change.findViewById(R.id.ingredient_name)).getText().toString().isEmpty());
    screenshot("05_edit_ingredient");
    runOnMainSync(
        () -> {
          ((EditText) change.findViewById(R.id.quantity)).setText("1");
          change.findViewById(R.id.save).performClick();
        });
    waitFor(() -> change.isFinishing() || change.isDestroyed());
    check(db.ingredient(eggId).quantity.intValue() == 1, "UI edit saved quantity");
    waitForScreen("Your pantry");
    screenshot("06_ingredient_updated");
    db.save(eggId, "eggs", new BigDecimal("2"), "each", null, "unused");
    db.save(0, "tomatoes", new BigDecimal("2"), "each", null, "evidence-tomato");
    db.save(0, "onion", BigDecimal.ONE, "each", null, "evidence-onion");
    db.save(0, "olive oil", BigDecimal.ONE, "tbsp", null, "evidence-oil");
    Activity incomplete = open(SuggestedRecipesActivity.class);
    waitFor(() -> ((ListView) incomplete.findViewById(R.id.recipe_list)).getAdapter() != null);
    check(
        ((ListView) incomplete.findViewById(R.id.recipe_list)).getAdapter().getCount() == 0,
        "UI suggestions exclude incomplete recipe");
    screenshot("07_no_matches_four_of_five");
    long saltId = db.save(0, "salt", BigDecimal.ONE, "g", null, "evidence-salt");
    runOnMainSync(incomplete::finish);
    final Activity matches = open(SuggestedRecipesActivity.class);
    waitFor(
        () ->
            ((ListView) matches.findViewById(R.id.recipe_list)).getAdapter() != null
                && ((ListView) matches.findViewById(R.id.recipe_list)).getAdapter().getCount() > 0);
    screenshot("08_strict_match_all_five");
    Recipe skillet = null;
    for (Recipe recipe : db.recipes())
      if (recipe.name.equals("Tomato egg skillet")) skillet = recipe;
    Activity detail =
        startActivitySync(
            new Intent(getTargetContext(), RecipeDetailActivity.class)
                .putExtra(RecipeDetailActivity.EXTRA_ID, skillet.id)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    waitFor(
        () -> !((TextView) detail.findViewById(R.id.recipe_name)).getText().toString().isEmpty());
    screenshot("09_recipe_details");
    runOnMainSync(
        () ->
            ((android.widget.ScrollView)
                    ((View) detail.findViewById(R.id.page_title).getParent())
                        .getParent()
                        .getParent())
                .fullScroll(View.FOCUS_DOWN));
    screenshot("10_recipe_method");
    Activity collection = open(RecipesActivity.class);
    waitFor(() -> ((ListView) collection.findViewById(R.id.recipe_list)).getAdapter() != null);
    screenshot("11_recipe_collection");
    Activity settings = open(SettingsActivity.class);
    screenshot("12_settings");
    Activity delete = openEdit(saltId);
    waitFor(
        () ->
            !((EditText) delete.findViewById(R.id.ingredient_name)).getText().toString().isEmpty());
    runOnMainSync(() -> delete.findViewById(R.id.delete).performClick());
    screenshot("13_delete_confirmation");
    // Press the positive dialog button through a real tap supplied by UI automation.
    android.view.accessibility.AccessibilityNodeInfo root =
        getUiAutomation().getRootInActiveWindow();
    boolean clicked = false;
    for (android.view.accessibility.AccessibilityNodeInfo node :
        root.findAccessibilityNodeInfosByViewId("android:id/button1"))
      if (node.isClickable()) {
        node.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK);
        clicked = true;
        break;
      }
    check(clicked, "Delete confirmation action found");
    waitFor(() -> delete.isFinishing() || delete.isDestroyed());
    check(db.ingredient(saltId) == null, "UI delete removed pantry row");
    Activity afterDelete = open(SuggestedRecipesActivity.class);
    waitFor(() -> ((ListView) afterDelete.findViewById(R.id.recipe_list)).getAdapter() != null);
    screenshot("14_match_removed_after_delete");
    db.save(0, "salt", BigDecimal.ONE, "g", null, "evidence-salt-final");
    // Reuse the existing launcher instead of startActivitySync, which waits for
    // a NEW Activity and can block when Android only brings the root task forward.
    getTargetContext()
        .startActivity(
            new Intent(getTargetContext(), PantryActivity.class)
                .addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | Intent.FLAG_ACTIVITY_SINGLE_TOP));
    waitForScreen("Your pantry");
    waitFor(
        () ->
            ((ListView) pantry.findViewById(R.id.pantry_list)).getAdapter() != null
                && ((ListView) pantry.findViewById(R.id.pantry_list)).getAdapter().getCount() == 5);
    screenshot("15_final_pantry");
  }
}
