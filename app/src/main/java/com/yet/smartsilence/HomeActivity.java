package com.yet.smartsilence;

import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.yet.smartsilence.database.RuleDatabaseHelper;
import com.yet.smartsilence.database.models.RuleModel;
import com.yet.smartsilence.services.TimeSchedulerService;
import com.yet.smartsilence.utils.TimeUtils;

import java.util.List;

public class HomeActivity extends AppCompatActivity {

    private TextView ringerStatusTextView;
    private TextView nextRuleTextView;
    private AudioManager audioManager;
    private RuleDatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        ringerStatusTextView = findViewById(R.id.ringerStatusTextView);
        nextRuleTextView = findViewById(R.id.nextRuleTextView);

        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        dbHelper = new RuleDatabaseHelper(this);

        // הוספת חוק לבדיקה
       //dbHelper.deleteAllRules();
        //dbHelper.insertTestTimeRule();

        displayCurrentRingerMode();
        displayNextScheduledRule();
    }

    private BroadcastReceiver ringerModeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (AudioManager.RINGER_MODE_CHANGED_ACTION.equals(intent.getAction())) {
                displayCurrentRingerMode(); // עדכן תצוגה
            }
        }
    };


    @Override
    protected void onResume() {
        super.onResume();

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (!notificationManager.isNotificationPolicyAccessGranted()) {
            showPermissionDialog();
        } else {
            startService(new Intent(this, TimeSchedulerService.class));
        }

        IntentFilter filter = new IntentFilter(AudioManager.RINGER_MODE_CHANGED_ACTION);
        registerReceiver(ringerModeReceiver, filter);
    }

    protected void onPause() {
        super.onPause();
        unregisterReceiver(ringerModeReceiver);
    }

    private void displayCurrentRingerMode() {
        int mode = audioManager.getRingerMode();
        String status;
        switch (mode) {
            case AudioManager.RINGER_MODE_SILENT:
                status = "מצב שקט";
                break;
            case AudioManager.RINGER_MODE_NORMAL:
                status = "מצב רגיל";
                break;
            case AudioManager.RINGER_MODE_VIBRATE:
                status = "רטט בלבד";
                break;
            default:
                status = "לא ידוע";
        }
        ringerStatusTextView.setText("מצב הצלצול הנוכחי: " + status);
    }

    private void displayNextScheduledRule() {
        List<RuleModel> timeRules = dbHelper.getActiveTimeRules();
        RuleModel nextRule = TimeUtils.findNextTimeRule(timeRules);

        if (nextRule != null) {
            String days = nextRule.getDaysOfWeek();
            String start = nextRule.getTimeStart();
            String end = nextRule.getTimeEnd();
            nextRuleTextView.setText("החוק הבא:\n" + start + " - " + end + " ב-" + days);
        } else {
            nextRuleTextView.setText("אין חוק זמן מתוכנן בקרוב");
        }
    }

    private void showPermissionDialog() {
        new AlertDialog.Builder(this)
                .setTitle("הרשאת 'נא לא להפריע'")
                .setMessage("כדי שהאפליקציה תוכל להעביר את הטלפון למצב שקט, יש לאפשר גישה להגדרות 'נא לא להפריע'.")
                .setPositiveButton("לאפשר", (dialog, which) -> {
                    Intent intent = new Intent(android.provider.Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
                    startActivity(intent);
                })
                .setNegativeButton("בטל", null)
                .show();
    }
}
