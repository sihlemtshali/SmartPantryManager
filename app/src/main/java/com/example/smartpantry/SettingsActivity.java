package com.example.smartpantry;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.*;
import com.example.smartpantry.core.UnitConverter;
import java.util.Arrays;

/** Stores the in-app expiry highlight and preferred unit using private SharedPreferences. */
public final class SettingsActivity extends BaseActivity {
  @Override
  protected void onCreate(Bundle state) {
    super.onCreate(state);
    page(R.layout.activity_settings, "Your preferences", "Small choices, stored on this device.");
    SharedPreferences preferences = getSharedPreferences("settings", MODE_PRIVATE);
    Switch alerts = findViewById(R.id.expiry_alerts);
    alerts.setChecked(preferences.getBoolean("expiry_alerts", true));
    alerts.setOnCheckedChangeListener(
        (button, checked) -> preferences.edit().putBoolean("expiry_alerts", checked).apply());
    Spinner units = findViewById(R.id.default_unit);
    ArrayAdapter<String> adapter =
        new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, UnitConverter.UNITS);
    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    units.setAdapter(adapter);
    units.setSelection(
        Arrays.asList(UnitConverter.UNITS).indexOf(preferences.getString("default_unit", "g")));
    findViewById(R.id.save_settings)
        .setOnClickListener(
            v -> {
              preferences
                  .edit()
                  .putString("default_unit", (String) units.getSelectedItem())
                  .apply();
              toast("Preferences saved");
            });
  }
}
