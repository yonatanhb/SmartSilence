package com.yonatanh_tald_eveb.smartsilence.utils;

import android.content.Context;
import android.media.AudioManager;
import android.util.Log;

public class RuleStateManager {
    //Updates the device's ringer mode based on the currently active rules.
    public static void updateRingerMode(Context context) {
        // Check whether a location-based rule is currently active
        boolean locationRuleActive = context.getSharedPreferences("smartsilence_state", Context.MODE_PRIVATE)
                .getBoolean("location_rule_active", false);

        // Check whether a time-based rule is currently active
        boolean timeRuleActive = context.getSharedPreferences("smartsilence_state", Context.MODE_PRIVATE)
                .getBoolean("time_rule_active", false);

        // Get the AudioManager to control the ringer mode
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

        int wantedMode;

        // Determine the desired ringer mode based on active rules
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

        // If current mode is different from desired, update it and notify the user
        if (audioManager.getRingerMode() != wantedMode) {
            audioManager.setRingerMode(wantedMode);
            NotificationHelper.showRingerModeChanged(context, wantedMode);

            // ---- שידור עדכון לרשימת החוקים הפעילים ----
            context.sendBroadcast(new android.content.Intent(
                    "com.yonatanh_tald_evem.smartsilence.ACTION_ACTIVE_RULES_CHANGED"
            ));
        }
    }
}

