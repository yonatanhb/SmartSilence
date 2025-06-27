package com.yonatanh_tald_evem.smartsilence.activities;

import android.Manifest;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.yonatanh_tald_evem.smartsilence.R;
import com.yonatanh_tald_evem.smartsilence.database.RuleDatabaseHelper;
import com.yonatanh_tald_evem.smartsilence.database.models.RuleModel;
import com.yonatanh_tald_evem.smartsilence.services.TimeSchedulerService;
import com.yonatanh_tald_evem.smartsilence.services.LocationMonitorService;
import com.yonatanh_tald_evem.smartsilence.views.WeekDaysView;

import java.util.List;

public class HomeActivity extends AppCompatActivity {
    // UI components
    private TextView ringerStatusTextView;
    private TextView activeRulesTextView;
    private TextView nextRuleTextView;
    private ImageView ringerStatusIcon;

    // System and database helpers
    private AudioManager audioManager;
    private RuleDatabaseHelper dbHelper;

    // State flags
    private boolean ringerModeReceiverRegistered = false;
    private boolean locationPermissionRequested = false;

    // Used to store ID of the next scheduled rule
    private int nextRuleId = -1;

    // Background location permission is required
    private final boolean needBackgroundLocation =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q;

    private TextView activeRulesCountTextView;
    private LinearLayout activeRulesContainer;
    private LinearLayout noActiveRulesLayout;

    private BroadcastReceiver activeRulesChangedReceiver;
    private boolean activeRulesChangedReceiverRegistered = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // Set up toolbar and status bar color
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.primary_color));

        // Initialize UI components
        activeRulesCountTextView = findViewById(R.id.activeRulesCountTextView);
        activeRulesContainer = findViewById(R.id.activeRulesContainer);
        noActiveRulesLayout = findViewById(R.id.noActiveRulesLayout);
        ringerStatusTextView = findViewById(R.id.ringerStatusTextView);
        nextRuleTextView     = findViewById(R.id.nextRuleTextView);
        ringerStatusIcon     = findViewById(R.id.ringerStatusIcon);

        // Initialize audio manager and database helper
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        dbHelper     = new RuleDatabaseHelper(this);
        setupButtonListeners();
        displayCurrentRingerMode();
        displayNextScheduledRule();
    }

    // Resume lifecycle: check permissions, start services, register broadcast
    @Override
    protected void onResume() {
        super.onResume();

        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (!nm.isNotificationPolicyAccessGranted()) {
            showPermissionDialog();
            return;
        }

        if (!hasLocationPermissions()) {
            if (!locationPermissionRequested) {
                requestLocationPermissions();
                locationPermissionRequested = true;
            } else {
                showLocationPermissionDialog();
            }
            return;
        }

        locationPermissionRequested = false;

        startAllSmartSilenceServices();

        if (!ringerModeReceiverRegistered) {
            registerReceiver(ringerModeReceiver, new IntentFilter(AudioManager.RINGER_MODE_CHANGED_ACTION));
            ringerModeReceiverRegistered = true;
        }

        // --- קליטה לשידור שינוי חוקים פעילים ---
        if (!activeRulesChangedReceiverRegistered) {
            IntentFilter filter = new IntentFilter("com.yonatanh_tald_evem.smartsilence.ACTION_ACTIVE_RULES_CHANGED");
            activeRulesChangedReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    displayCurrentlyActiveRules();
                }
            };
            registerReceiver(activeRulesChangedReceiver, filter);
            activeRulesChangedReceiverRegistered = true;
        }

        displayCurrentRingerMode();
        displayNextScheduledRule();
        displayCurrentlyActiveRules();
    }


    // Pause lifecycle: unregister broadcast receiver
    @Override
    protected void onPause() {
        super.onPause();
        if (ringerModeReceiverRegistered) {
            unregisterReceiver(ringerModeReceiver);
            ringerModeReceiverRegistered = false;
        }
        // ביטול הרשמה ל־activeRulesChangedReceiver
        if (activeRulesChangedReceiverRegistered && activeRulesChangedReceiver != null) {
            unregisterReceiver(activeRulesChangedReceiver);
            activeRulesChangedReceiverRegistered = false;
        }
    }


    // Inflate menu (About, Settings, Exit)
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.home_menu, menu);
        return true;
    }

    // Handle menu item selection
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();

        if (itemId == R.id.action_about) {
            showAboutDialog();
            return true;
        } else if (itemId == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        } else if (itemId == R.id.action_exit) {
            showExitDialog();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    // Check if all necessary location permissions are granted
    private boolean hasLocationPermissions() {
        boolean fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        boolean background = !needBackgroundLocation ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED;
        return fine && background;
    }

    // Request foreground and (if needed) background location permissions
    private void requestLocationPermissions() {
        if (needBackgroundLocation) {
            locationPermissionLauncher.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
            });
        } else {
            locationPermissionLauncher.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION
            });
        }
    }

    // Handles location permission requests
    private final ActivityResultLauncher<String[]> locationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                boolean fineGranted = Boolean.TRUE.equals(result.get(Manifest.permission.ACCESS_FINE_LOCATION));
                boolean backgroundGranted = !needBackgroundLocation ||
                        Boolean.TRUE.equals(result.get(Manifest.permission.ACCESS_BACKGROUND_LOCATION));

                if (fineGranted && backgroundGranted) {
                    locationPermissionRequested = false;
                    startAllSmartSilenceServices();
                } else {
                    showLocationPermissionDialog();
                }
            });

    // Broadcast receiver to listen for changes in ringer mode (silent/vibrate/normal)
    private final BroadcastReceiver ringerModeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (AudioManager.RINGER_MODE_CHANGED_ACTION.equals(intent.getAction())) {
                displayCurrentRingerMode();
                displayNextScheduledRule();
                displayCurrentlyActiveRules();
            }
        }
    };

    // Start both time and location-based monitoring services
    private void startAllSmartSilenceServices() {
        ContextCompat.startForegroundService(this, new Intent(this, LocationMonitorService.class));
        startService(new Intent(this, TimeSchedulerService.class));
    }

    // Set up button listeners for adding or editing rules
    private void setupButtonListeners() {
        MaterialButton addRuleButton = findViewById(R.id.addRuleButton);
        addRuleButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, RulesActivity.class);
            startActivity(intent);
        });

        MaterialButton editRuleButton = findViewById(R.id.editRuleButton);
        editRuleButton.setOnClickListener(v -> {
            if (nextRuleId != -1) {
                Intent intent = new Intent(this, AddEditRuleActivity.class);
                intent.putExtra("ruleId", nextRuleId);
                startActivity(intent);
            }
        });
    }

    // Display current ringer mode and update UI
    private void displayCurrentRingerMode() {
        int mode = audioManager.getRingerMode();
        String status;
        int iconRes, colorRes;

        switch (mode) {
            case AudioManager.RINGER_MODE_SILENT:
                status = getString(R.string.ringer_mode_silent);
                iconRes = R.drawable.ic_volume_off;
                colorRes = R.color.ringer_silent;
                break;
            case AudioManager.RINGER_MODE_VIBRATE:
                status = getString(R.string.ringer_mode_vibrate);
                iconRes = R.drawable.ic_vibrate;
                colorRes = R.color.ringer_vibrate;
                break;
            case AudioManager.RINGER_MODE_NORMAL:
            default:
                status = getString(R.string.ringer_mode_normal);
                iconRes = R.drawable.ic_volume_up;
                colorRes = R.color.ringer_normal;
                break;
        }

        ringerStatusTextView.setText(status);
        ringerStatusIcon.setImageResource(iconRes);
        int tintColor = ContextCompat.getColor(this, colorRes);
        ringerStatusIcon.setColorFilter(tintColor);
    }

    // Display list of currently active rules (time or location-based)
    private void displayCurrentlyActiveRules() {
        List<RuleModel> activeRules = dbHelper.getCurrentlyActiveRules();

        activeRulesCountTextView.setText(String.valueOf(activeRules.size()));
        activeRulesContainer.removeAllViews();

        if (activeRules.isEmpty()) {
            // הצגת הודעת "אין חוקים פעילים"
            noActiveRulesLayout.setVisibility(View.VISIBLE);
            // activeRulesContainer.addView(noActiveRulesLayout); // למחוק שורה זו
        } else {
            noActiveRulesLayout.setVisibility(View.GONE);

            for (RuleModel rule : activeRules) {
                View ruleItemView = createActiveRuleItemView(rule);
                activeRulesContainer.addView(ruleItemView);
            }
        }
    }


    private View createActiveRuleItemView(RuleModel rule) {
        // קונטיינר חיצוני (שורה)
        LinearLayout ruleContainer = new LinearLayout(this);
        LinearLayout.LayoutParams containerParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        containerParams.setMargins(0, 0, 0, dpToPx(12));
        ruleContainer.setLayoutParams(containerParams);
        ruleContainer.setOrientation(LinearLayout.HORIZONTAL);
        ruleContainer.setGravity(Gravity.CENTER_VERTICAL);
        ruleContainer.setBackground(ContextCompat.getDrawable(this, R.drawable.bg_active_rule_item));
        ruleContainer.setPadding(dpToPx(16), dpToPx(12), dpToPx(16), dpToPx(12));

        // *** אייקון סוג החוק - בצד שמאל ***
        ImageView ruleIcon = new ImageView(this);
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dpToPx(32), dpToPx(32));
        iconParams.setMarginEnd(dpToPx(12));
        ruleIcon.setLayoutParams(iconParams);

        if ("time".equals(rule.getType())) {
            ruleIcon.setImageResource(R.drawable.ic_schedule);
            ruleIcon.setColorFilter(ContextCompat.getColor(this, R.color.time_rule_color));
        } else if ("location".equals(rule.getType())) {
            ruleIcon.setImageResource(R.drawable.ic_location_on);
            ruleIcon.setColorFilter(ContextCompat.getColor(this, R.color.location_rule_color));
        }

        // *** קונטיינר לטקסטים (שם למעלה, פרטים מתחת) ***
        LinearLayout textContainer = new LinearLayout(this);
        LinearLayout.LayoutParams textContainerParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f
        );
        textContainer.setLayoutParams(textContainerParams);
        textContainer.setOrientation(LinearLayout.VERTICAL);
        textContainer.setGravity(Gravity.END); // יישור לימין

        // שם החוק (למעלה)
        TextView ruleNameText = new TextView(this);
        ruleNameText.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        String ruleName = rule.getRuleName() != null && !rule.getRuleName().isEmpty()
                ? rule.getRuleName()
                : getString(rule.getType().equals("time") ? R.string.unnamed_time_rule : R.string.unnamed_location_rule);
        ruleNameText.setText(ruleName);
        ruleNameText.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        ruleNameText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        ruleNameText.setTypeface(null, Typeface.BOLD);
        ruleNameText.setGravity(Gravity.END);

        // פרטי החוק (מתחת לשם)
        TextView ruleDetailsText = new TextView(this);
        LinearLayout.LayoutParams detailsParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        detailsParams.setMargins(0, dpToPx(4), 0, 0);
        ruleDetailsText.setLayoutParams(detailsParams);

        String ruleDetails;
        if ("time".equals(rule.getType())) {
            ruleDetails = getString(R.string.time_range_format, rule.getTimeStart(), rule.getTimeEnd());
        } else {
            String locationName = rule.getLocationName() != null ?
                    rule.getLocationName() : getString(R.string.location_not_set);
            ruleDetails = getString(R.string.location_format, locationName);
        }

        ruleDetailsText.setText(ruleDetails);
        ruleDetailsText.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        ruleDetailsText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        ruleDetailsText.setGravity(Gravity.END);

        // הוספת הטקסטים לקונטיינר
        textContainer.addView(ruleNameText);
        textContainer.addView(ruleDetailsText);

        // סדר: קודם קונטיינר טקסט, אחריו אייקון בצד שמאל
        ruleContainer.addView(textContainer);
        ruleContainer.addView(ruleIcon);

        // אנימציה
        ruleContainer.setAlpha(0f);
        ruleContainer.animate()
                .alpha(1f)
                .setDuration(300)
                .setStartDelay(100)
                .start();

        return ruleContainer;
    }


    // פונקציית עזר להמרת dp לפיקסלים
    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    // Display the next scheduled time-based rule
    private void displayNextScheduledRule() {
        RuleModel nextRule = dbHelper.getNextScheduledTimeRule();
        MaterialButton editRuleButton = findViewById(R.id.editRuleButton);
        WeekDaysView weekDaysView = findViewById(R.id.weekDaysView);

        if (nextRule != null) {
            nextRuleTextView.setText(
                    getString(R.string.next_rule_format, nextRule.getTimeStart(), nextRule.getTimeEnd())
            );
            editRuleButton.setVisibility(View.VISIBLE);
            weekDaysView.setVisibility(View.VISIBLE);
            weekDaysView.setSelectable(false);
            weekDaysView.setDaysMask(nextRule.getDaysMask());
            nextRuleId = nextRule.getId();
        } else {
            nextRuleTextView.setText(R.string.no_next_rule);
            editRuleButton.setVisibility(View.GONE);
            weekDaysView.setVisibility(View.GONE);
            nextRuleId = -1;
        }
    }

    // Show app and developer info
    private void showAboutDialog() {
        String appName = getString(R.string.app_name);
        String packageName = getPackageName();
        String osVersion = getString(R.string.android_version_format, Build.VERSION.RELEASE, Build.VERSION.SDK_INT);
        String submissionDate = getString(R.string.submission_date_num);
        String developers = getString(R.string.developers);

        String aboutMessage = getString(
                R.string.about_dialog_message,
                appName,
                packageName,
                osVersion,
                developers,
                submissionDate
        );

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.about_dialog_title)
                .setMessage(aboutMessage)
                .setPositiveButton(R.string.about_dialog_close, null)
                .create();

        Window window = dialog.getWindow();
        if (window != null) {
            window.getDecorView().setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        }

        dialog.show();
    }

    // Show confirmation dialog before exiting app
    private void showExitDialog() {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.exit_dialog_title)
                .setMessage(R.string.exit_dialog_message)
                .setPositiveButton(R.string.exit_dialog_confirm, (d, w) -> finishAffinity())
                .setNegativeButton(R.string.exit_dialog_cancel, null)
                .create();
        Window window = dialog.getWindow();
        if (window != null) {
            window.getDecorView().setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        }

        dialog.show();
    }

    // Show dialog to request "Do Not Disturb" permission
    private void showPermissionDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.dnd_permission_title)
                .setMessage(R.string.dnd_permission_message)
                .setPositiveButton(R.string.permission_allow, (d, w) -> startActivity(
                        new Intent(android.provider.Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)))
                .setNegativeButton(R.string.permission_cancel, null)
                .show();
    }

    // Show dialog guiding user to grant background location access
    private void showLocationPermissionDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.location_permission_title)
                .setMessage(R.string.location_permission_message)
                .setPositiveButton(R.string.open_settings, (d, w) -> {
                    Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    intent.setData(android.net.Uri.parse(R.string.package_word + getPackageName()));
                    startActivity(intent);
                })
                .setNegativeButton(R.string.exit_dialog_cancel, null)
                .show();
    }
}