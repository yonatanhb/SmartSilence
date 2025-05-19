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
        if (!isAppActive) {
            Log.d("SmartSilence", "App setting disabled, no change");
            return;
        }

        List<RuleModel> timeRules = dbHelper.getActiveTimeRules();
        for (RuleModel rule : timeRules) {
            if (TimeUtils.isRuleActiveNow(rule)) {
                setRingerMode(AudioManager.RINGER_MODE_SILENT);
                Log.d("SmartSilence", "rule active: turn to silence mode");
                return;
            }
        }

        // אם אף חוק לא תקף עכשיו – חזור למצב רגיל
        setRingerMode(AudioManager.RINGER_MODE_NORMAL);
        Log.d("SmartSilence", "There is no active rule: no change");
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
