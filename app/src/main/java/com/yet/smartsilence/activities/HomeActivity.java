package com.yet.smartsilence.activities;

import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.yet.smartsilence.R;
import com.yet.smartsilence.database.RuleDatabaseHelper;
import com.yet.smartsilence.database.models.RuleModel;
import com.yet.smartsilence.services.TimeSchedulerService;
import com.yet.smartsilence.utils.TimeUtils;
import com.yet.smartsilence.views.WeekDaysView;

import java.util.List;

public class HomeActivity extends AppCompatActivity {

    private TextView ringerStatusTextView;
    private TextView nextRuleTextView;
    private ImageView ringerStatusIcon;
    private AudioManager audioManager;
    private RuleDatabaseHelper dbHelper;

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
        dbHelper.insertTestTimeRule();

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
            // Intent intent = new Intent(this, SettingsActivity.class);
            // startActivity(intent);
        });
    }

    private final BroadcastReceiver ringerModeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (AudioManager.RINGER_MODE_CHANGED_ACTION.equals(intent.getAction())) {
                displayCurrentRingerMode();
            }
        }
    };

    @Override
    protected void onResume() {
        super.onResume();

        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (!nm.isNotificationPolicyAccessGranted()) {
            showPermissionDialog();
        } else {
            startService(new Intent(this, TimeSchedulerService.class));
        }

        registerReceiver(ringerModeReceiver,
                new IntentFilter(AudioManager.RINGER_MODE_CHANGED_ACTION));

        displayCurrentRingerMode();
        displayNextScheduledRule();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(ringerModeReceiver);
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


    private void showPermissionDialog() {
        new AlertDialog.Builder(this)
                .setTitle("הרשאת 'נא לא להפריע'")
                .setMessage("כדי שהאפליקציה תוכל להעביר את הטלפון למצב שקט, יש לאפשר גישה להגדרות 'נא לא להפריע'.")
                .setPositiveButton("לאפשר", (d,w) -> {
                    startActivity(new Intent(
                            android.provider.Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS
                    ));
                })
                .setNegativeButton("בטל", null)
                .show();
    }
}
