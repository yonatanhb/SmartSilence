package com.yonatanh_tald_evem.smartsilence.utils;

import android.content.Context;
import android.media.AudioManager;
import android.util.Log;

public class RuleStateManager {

    public static void updateRingerMode(Context context) {
        boolean locationRuleActive = context.getSharedPreferences("smartsilence_state", Context.MODE_PRIVATE)
                .getBoolean("location_rule_active", false);

        boolean timeRuleActive = context.getSharedPreferences("smartsilence_state", Context.MODE_PRIVATE)
                .getBoolean("time_rule_active", false);

        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

        int wantedMode;
        if (locationRuleActive) {
            wantedMode = AudioManager.RINGER_MODE_SILENT;
            Log.d("SmartSilence", "location rule: SILENT");
        } else if (timeRuleActive) {
            wantedMode = AudioManager.RINGER_MODE_SILENT;
            Log.d("SmartSilence", "time rule active: SILENT");
        } else {
            wantedMode = AudioManager.RINGER_MODE_NORMAL;
            Log.d("SmartSilence", "no active rules: NORMAL");
        }

        if (audioManager.getRingerMode() != wantedMode) {
            audioManager.setRingerMode(wantedMode);
            NotificationHelper.showRingerModeChanged(context, wantedMode);
        }
    }
}
