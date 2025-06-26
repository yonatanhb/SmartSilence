package com.yonatanh_tald_evem.smartsilence.activities;

import android.Manifest;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
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
import com.yonatanh_tald_evem.smartsilence.utils.TimeUtils;
import com.yonatanh_tald_evem.smartsilence.views.WeekDaysView;

import java.util.List;

public class HomeActivity extends AppCompatActivity {

    private TextView ringerStatusTextView;
    private TextView nextRuleTextView;
    private ImageView ringerStatusIcon;
    private AudioManager audioManager;
    private RuleDatabaseHelper dbHelper;
    private boolean ringerModeReceiverRegistered = false;
    private boolean locationPermissionRequested = false;

    private int nextRuleId = -1;

    // ל־Android 10 ומעלה – ACCESS_BACKGROUND_LOCATION נדרש בנפרד
    private final boolean needBackgroundLocation =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q;

    private final BroadcastReceiver ringerModeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (AudioManager.RINGER_MODE_CHANGED_ACTION.equals(intent.getAction())) {
                displayCurrentRingerMode();
            }
        }
    };

    // בקשת הרשאות מיקום
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // הגדרת Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.primary_color));

        // אתחול רכיבי ממשק
        ringerStatusTextView = findViewById(R.id.ringerStatusTextView);
        nextRuleTextView     = findViewById(R.id.nextRuleTextView);
        ringerStatusIcon     = findViewById(R.id.ringerStatusIcon);

        // אתחול שירותים ובסיס נתונים
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        dbHelper     = new RuleDatabaseHelper(this);
//        dbHelper.deleteAllRules();
//        dbHelper.insertTestTimeRule();
//        dbHelper.insertLocationRule(
//                "בית ספר",
//                "תיכון רוטברג",
//                31.987654,        // קו רוחב
//                34.765432,        // קו אורך
//                100,              // רדיוס במטרים
//                true              // פעיל
//        );

        setupButtonListeners();

        displayCurrentRingerMode();
        displayNextScheduledRule();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.home_menu, menu);
        return true;
    }

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

    private void showAboutDialog() {
        // איסוף מידע על האפליקציה והמכשיר
        String appName = getString(R.string.app_name);
        String packageName = getPackageName();

        // פרטי מערכת ההפעלה
        String osVersion = "Android " + Build.VERSION.RELEASE + " API " +Build.VERSION.SDK_INT;

        // תאריך הגשה
        String submissionDate = "29/06/2025";
        String developers = "יונתן חבה, טל דניאל, איב בן ישעיה";

        // בניית הודעת האודות
        String aboutMessage = String.format(
                "שם האפליקציה: %s\n" +
                        "מזהה האפליקציה: %s\n" +
                        "מערכת הפעלה: %s\n\n" +
                        "פותח על ידי: %s\n" +
                        "תאריך הגשה: %s",
                appName,
                packageName,
                osVersion,
                developers,
                submissionDate
        );

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("אודות האפליקציה")
                .setMessage(aboutMessage)
                .setPositiveButton("סגור", null)
                .create();

        // הגדרת כיוון RTL לכל החלונית
        Window window = dialog.getWindow();
        if (window != null) {
            window.getDecorView().setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        }

        dialog.show();
    }

    private void showExitDialog() {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("יציאה מהאפליקציה")
                .setMessage("האם אתה בטוח שברצונך לצאת מהאפליקציה?")
                .setPositiveButton("יציאה", (d, w) -> finishAffinity())
                .setNegativeButton("ביטול", null)
                .create();
        Window window = dialog.getWindow();
        if (window != null) {
            window.getDecorView().setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        }

        dialog.show();
    }

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
                // דיאלוג – תציג רק אם אין הרשאה
                showLocationPermissionDialog();
            }
            return;
        }

        // ברגע שיש הרשאות — אפס את הדגל!
        locationPermissionRequested = false;

        startAllSmartSilenceServices();

        if (!ringerModeReceiverRegistered) {
            registerReceiver(ringerModeReceiver, new IntentFilter(AudioManager.RINGER_MODE_CHANGED_ACTION));
            ringerModeReceiverRegistered = true;
        }

        displayCurrentRingerMode();
        displayNextScheduledRule();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (ringerModeReceiverRegistered) {
            unregisterReceiver(ringerModeReceiver);
            ringerModeReceiverRegistered = false;
        }
    }

    private boolean hasLocationPermissions() {
        boolean fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        boolean background = !needBackgroundLocation ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED;
        return fine && background;
    }

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

    private void startAllSmartSilenceServices() {
        // Start both services safely (לא תיפתח פעמיים, גם אם תנסה להפעיל שוב)
        ContextCompat.startForegroundService(this, new Intent(this, LocationMonitorService.class));
        startService(new Intent(this, TimeSchedulerService.class));
    }

    private void showPermissionDialog() {
        new AlertDialog.Builder(this)
                .setTitle("הרשאת 'נא לא להפריע'")
                .setMessage("כדי שהאפליקציה תוכל להעביר את הטלפון למצב שקט, יש לאפשר גישה להגדרות 'נא לא להפריע'.")
                .setPositiveButton("לאפשר", (d, w) -> {
                    startActivity(new Intent(
                            android.provider.Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS
                    ));
                })
                .setNegativeButton("בטל", null)
                .show();
    }

    private void showLocationPermissionDialog() {
        new AlertDialog.Builder(this)
                .setTitle("דרושה הרשאת מיקום ברקע")
                .setMessage("כדי שהאפליקציה תוכל לעבור אוטומטית למצב שקט גם כשאינה פתוחה, יש להיכנס להגדרות האפליקציה ולאפשר 'גישה למיקום תמיד'.")
                .setPositiveButton("פתח הגדרות", (d, w) -> {
                    Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    intent.setData(android.net.Uri.parse("package:" + getPackageName()));
                    startActivity(intent);
                })
                .setNegativeButton("ביטול", null)
                .show();
    }

    private void displayCurrentRingerMode() {
        int mode = audioManager.getRingerMode();
        String status;
        int iconRes, colorRes;

        switch (mode) {
            case AudioManager.RINGER_MODE_SILENT:
                status = "מצב שקט";
                iconRes = R.drawable.ic_volume_off;
                colorRes = R.color.ringer_silent;
                break;
            case AudioManager.RINGER_MODE_VIBRATE:
                status = "רטט בלבד";
                iconRes = R.drawable.ic_vibrate;
                colorRes = R.color.ringer_vibrate;
                break;
            case AudioManager.RINGER_MODE_NORMAL:
            default:
                status = "מצב רגיל";
                iconRes = R.drawable.ic_volume_up;
                colorRes = R.color.ringer_normal;
                break;
        }

        ringerStatusTextView.setText(status);
        ringerStatusIcon.setImageResource(iconRes);
        int tintColor = ContextCompat.getColor(this, colorRes);
        ringerStatusIcon.setColorFilter(tintColor);
    }

    private void displayNextScheduledRule() {
        List<RuleModel> timeRules = dbHelper.getActiveTimeRules();
        RuleModel nextRule = TimeUtils.findNextTimeRule(timeRules);

        MaterialButton editRuleButton = findViewById(R.id.editRuleButton);
        WeekDaysView weekDaysView     = findViewById(R.id.weekDaysView);

        if (nextRule != null) {
            nextRuleTextView.setText(
                    String.format("%s - %s", nextRule.getTimeStart(), nextRule.getTimeEnd())
            );
            editRuleButton.setVisibility(View.VISIBLE);

            weekDaysView.setSelectable(false);
            weekDaysView.setDaysMask(nextRule.getDaysMask());

            // שמור את מזהה החוק הקרוב
            nextRuleId = nextRule.getId();

        } else {
            nextRuleTextView.setText("אין כלל זמן מתוזמן בקרוב");
            editRuleButton.setVisibility(View.GONE);
            weekDaysView.setVisibility(View.GONE);

            nextRuleId = -1;
        }
    }
}