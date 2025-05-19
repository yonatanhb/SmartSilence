package com.yet.smartsilence.activities;

import android.Manifest;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.yet.smartsilence.R;
import com.yet.smartsilence.database.RuleDatabaseHelper;
import com.yet.smartsilence.database.models.RuleModel;
import com.yet.smartsilence.services.TimeSchedulerService;
import com.yet.smartsilence.services.LocationMonitorService;
import com.yet.smartsilence.utils.TimeUtils;
import com.yet.smartsilence.views.WeekDaysView;

import java.util.List;

public class HomeActivity extends AppCompatActivity {

    private TextView ringerStatusTextView;
    private TextView nextRuleTextView;
    private ImageView ringerStatusIcon;
    private AudioManager audioManager;
    private RuleDatabaseHelper dbHelper;
    private boolean ringerModeReceiverRegistered = false;
    private boolean locationPermissionRequested = false;

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

        // אתחול רכיבי ממשק
        ringerStatusTextView = findViewById(R.id.ringerStatusTextView);
        nextRuleTextView     = findViewById(R.id.nextRuleTextView);
        ringerStatusIcon     = findViewById(R.id.ringerStatusIcon);

        // אתחול שירותים ובסיס נתונים
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        dbHelper     = new RuleDatabaseHelper(this);
        dbHelper.deleteAllRules();
        //dbHelper.insertTestTimeRule();
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

    private void setupButtonListeners() {
        MaterialButton addRuleButton = findViewById(R.id.addRuleButton);
        addRuleButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, RulesActivity.class);
            startActivity(intent);
        });

        MaterialButton editRuleButton = findViewById(R.id.editRuleButton);
        editRuleButton.setOnClickListener(v -> {
            // Intent intent = new Intent(this, EditRuleActivity.class);
            // startActivity(intent);
        });

        FloatingActionButton settingsButton = findViewById(R.id.settingsButton);
        settingsButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
        });
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

        } else {
            nextRuleTextView.setText("אין כלל זמן מתוזמן בקרוב");
            editRuleButton.setVisibility(View.GONE);
            weekDaysView.setVisibility(View.GONE);
        }
    }
}
