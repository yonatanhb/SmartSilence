package com.yet.smartsilence.services;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import com.yet.smartsilence.database.RuleDatabaseHelper;
import com.yet.smartsilence.database.models.RuleModel;
import com.yet.smartsilence.utils.TimeUtils;

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
            if (com.yet.smartsilence.utils.TimeUtils.isRuleActiveNow(rule)) {
                timeRuleActive = true;
                break;
            }
        }

        getSharedPreferences("smartsilence_state", MODE_PRIVATE)
                .edit()
                .putBoolean("time_rule_active", timeRuleActive)
                .apply();

        com.yet.smartsilence.utils.RuleStateManager.updateRingerMode(this);
    }



    private void setRingerMode(int mode) {
        if (audioManager.getRingerMode() != mode) {
            audioManager.setRingerMode(mode);
            // שלח התראה על שינוי מצב הצלצול
            com.yet.smartsilence.utils.NotificationHelper.showRingerModeChanged(this, mode);
        }
    }


    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
