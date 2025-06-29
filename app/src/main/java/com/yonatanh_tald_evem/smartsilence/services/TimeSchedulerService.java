package com.yonatanh_tald_evem.smartsilence.services;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Handler;
import android.os.IBinder;

import com.yonatanh_tald_evem.smartsilence.database.RuleDatabaseHelper;
import com.yonatanh_tald_evem.smartsilence.database.models.RuleModel;

import java.util.List;

public class TimeSchedulerService extends Service {
    private Handler handler;
    private Runnable checkTask;
    private RuleDatabaseHelper dbHelper;
    private AudioManager audioManager;

    @Override
    public void onCreate() {
        super.onCreate();

        // Initialize handler for periodic task
        handler = new Handler();

        // Initialize database and audio manager
        dbHelper = new RuleDatabaseHelper(this);
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        // Define a task that checks rules every minute
        checkTask = new Runnable() {
            @Override
            public void run() {
                checkAndApplyRules();
                handler.postDelayed(this, 60 * 1000);
            }
        };

        // Start running the check task
        handler.post(checkTask);
    }

    /**
     * Checks all active time-based rules and updates their nowActive state.
     * Also updates the shared preference indicating whether a time rule is currently active,
     * and calls the RuleStateManager to apply the appropriate ringer mode.
     */
    private void checkAndApplyRules() {
        // Check if the app is active from SharedPreferences
        boolean isAppActive = getSharedPreferences("settings_prefs", MODE_PRIVATE)
                .getBoolean("app_active", true);
        if (!isAppActive) return;

        boolean timeRuleActive = false;

        // Get all time-based rules marked as active in the database
        List<RuleModel> timeRules = dbHelper.getActiveTimeRules();
        for (RuleModel rule : timeRules) {
            // Check if the rule should be active right now (based on time and day)
            boolean isActiveNow = com.yonatanh_tald_evem.smartsilence.utils.TimeUtils.isRuleActiveNow(rule);

            // Update nowActive column in DB
            rule.setNowActive(isActiveNow);
            dbHelper.updateNowActiveStatus(rule.getId(), isActiveNow);

            // If any rule is currently active, mark it
            if (isActiveNow && !timeRuleActive) {
                timeRuleActive = true;
            }
        }

        // Save current state to SharedPreferences
        getSharedPreferences("smartsilence_state", MODE_PRIVATE)
                .edit()
                .putBoolean("time_rule_active", timeRuleActive)
                .apply();

        // Update the phone's ringer mode according to current rule state
        com.yonatanh_tald_evem.smartsilence.utils.RuleStateManager.updateRingerMode(this);
    }

    public static void scheduleImmediateCheck(Context context) {
        new Thread(() -> {
            RuleDatabaseHelper dbHelper = new RuleDatabaseHelper(context);
            AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

            boolean isAppActive = context.getSharedPreferences("settings_prefs", Context.MODE_PRIVATE)
                    .getBoolean("app_active", true);
            if (!isAppActive) return;

            boolean timeRuleActive = false;
            List<RuleModel> timeRules = dbHelper.getActiveTimeRules();
            for (RuleModel rule : timeRules) {
                boolean isActiveNow = com.yonatanh_tald_evem.smartsilence.utils.TimeUtils.isRuleActiveNow(rule);
                rule.setNowActive(isActiveNow);
                dbHelper.updateNowActiveStatus(rule.getId(), isActiveNow);
                if (isActiveNow && !timeRuleActive) timeRuleActive = true;
            }

            context.getSharedPreferences("smartsilence_state", Context.MODE_PRIVATE)
                    .edit()
                    .putBoolean("time_rule_active", timeRuleActive)
                    .apply();

            com.yonatanh_tald_evem.smartsilence.utils.RuleStateManager.updateRingerMode(context);
        }).start();
    }

    //Set the phone's ringer mode directly.
    private void setRingerMode(int mode) {
        if (audioManager.getRingerMode() != mode) {
            audioManager.setRingerMode(mode);
            com.yonatanh_tald_evem.smartsilence.utils.NotificationHelper.showRingerModeChanged(this, mode);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
