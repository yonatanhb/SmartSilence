package com.yonatanh_tald_eveb.smartsilence.activities;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.CompoundButton;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import com.yonatanh_tald_eveb.smartsilence.R;

public class SettingsActivity extends AppCompatActivity {
    private static final String TAG = "SettingsActivity"; // Tag used for logging
    private static final String PREFS_NAME = "settings_prefs"; // SharedPreferences file name

    // Keys for preferences
    private static final String KEY_PUSH_ENABLED = "push_enabled";
    private static final String KEY_APP_ACTIVE = "app_active";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Initialize switches from layout
        SwitchCompat pushSwitch = findViewById(R.id.switchPush);
        SwitchCompat appActiveSwitch = findViewById(R.id.switchAppActive);

        // Access SharedPreferences for reading/saving settings
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        // Load saved preferences or use default values
        boolean isPushEnabled = prefs.getBoolean(KEY_PUSH_ENABLED, true);
        boolean isAppActive = prefs.getBoolean(KEY_APP_ACTIVE, true);

        // Set the switch states based on saved values
        pushSwitch.setChecked(isPushEnabled);
        appActiveSwitch.setChecked(isAppActive);

        // Listener for push notification switch changes
        pushSwitch.setOnCheckedChangeListener((CompoundButton buttonView, boolean isChecked) -> {
            prefs.edit().putBoolean(KEY_PUSH_ENABLED, isChecked).apply();
            Log.d(TAG, getString(isChecked ? R.string.log_push_enabled : R.string.log_push_disabled));
        });

        // Listener for app active status switch changes
        appActiveSwitch.setOnCheckedChangeListener((CompoundButton buttonView, boolean isChecked) -> {
            prefs.edit().putBoolean(KEY_APP_ACTIVE, isChecked).apply();
            Log.d(TAG, getString(R.string.log_app_active, String.valueOf(isChecked)));
        });
    }
}
