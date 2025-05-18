package com.yet.smartsilence.activities;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log; // <-- הוספת ייבוא Log
import android.widget.CompoundButton;
import android.widget.Switch;
import androidx.appcompat.app.AppCompatActivity;
import com.yet.smartsilence.R;

public class SettingsActivity extends AppCompatActivity {

    private static final String TAG = "SettingsActivity"; // Tag ללוגים
    private static final String PREFS_NAME = "settings_prefs";
    private static final String KEY_PUSH_ENABLED = "push_enabled";
    private static final String KEY_APP_ACTIVE = "app_active";

    private Switch pushSwitch;
    private Switch appActiveSwitch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        pushSwitch = findViewById(R.id.switchPush);
        appActiveSwitch = findViewById(R.id.switchAppActive);

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        // טעינת ערכים מהעדפות
        boolean isPushEnabled = prefs.getBoolean(KEY_PUSH_ENABLED, true);
        boolean isAppActive = prefs.getBoolean(KEY_APP_ACTIVE, true);

        pushSwitch.setChecked(isPushEnabled);
        appActiveSwitch.setChecked(isAppActive);

        // שמירת ערכים כאשר יש שינוי + לוג
        pushSwitch.setOnCheckedChangeListener((CompoundButton buttonView, boolean isChecked) -> {
            prefs.edit().putBoolean(KEY_PUSH_ENABLED, isChecked).apply();
            Log.d(TAG, "Push notifications " + (isChecked ? "enabled" : "disabled"));
        });

        appActiveSwitch.setOnCheckedChangeListener((CompoundButton buttonView, boolean isChecked) -> {
            prefs.edit().putBoolean(KEY_APP_ACTIVE, isChecked).apply();
            Log.d(TAG, "App active: " + isChecked);
        });
    }
}
