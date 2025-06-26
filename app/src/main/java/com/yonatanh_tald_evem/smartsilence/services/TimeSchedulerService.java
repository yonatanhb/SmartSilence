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

        handler = new Handler();
        dbHelper = new RuleDatabaseHelper(this);
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        checkTask = new Runnable() {
            @Override
            public void run() {
                checkAndApplyRules();
                handler.postDelayed(this, 60 * 1000); // כל דקה
            }
        };

        handler.post(checkTask);
    }

    private void checkAndApplyRules() {
        boolean isAppActive = getSharedPreferences("settings_prefs", MODE_PRIVATE)
                .getBoolean("app_active", true);
        if (!isAppActive) return;

        boolean timeRuleActive = false;
        List<RuleModel> timeRules = dbHelper.getActiveTimeRules();
        for (RuleModel rule : timeRules) {
            boolean isActiveNow = com.yonatanh_tald_evem.smartsilence.utils.TimeUtils.isRuleActiveNow(rule);

            // עדכון עמודת nowActive עבור כל חוק
            rule.setNowActive(isActiveNow);
            dbHelper.updateNowActiveStatus(rule.getId(), isActiveNow);

            if (isActiveNow && !timeRuleActive) {
                timeRuleActive = true;
            }
        }

        getSharedPreferences("smartsilence_state", MODE_PRIVATE)
                .edit()
                .putBoolean("time_rule_active", timeRuleActive)
                .apply();

        com.yonatanh_tald_evem.smartsilence.utils.RuleStateManager.updateRingerMode(this);
    }

    private void setRingerMode(int mode) {
        if (audioManager.getRingerMode() != mode) {
            audioManager.setRingerMode(mode);
            // שלח התראה על שינוי מצב הצלצול
            com.yonatanh_tald_evem.smartsilence.utils.NotificationHelper.showRingerModeChanged(this, mode);
        }
    }


    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
