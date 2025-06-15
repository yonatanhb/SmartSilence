package com.yet.smartsilence.services;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.media.AudioManager;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.yet.smartsilence.R;
import com.yet.smartsilence.database.RuleDatabaseHelper;
import com.yet.smartsilence.database.models.RuleModel;
import com.yet.smartsilence.utils.NotificationHelper;

import java.util.List;

public class LocationMonitorService extends Service {

    private FusedLocationProviderClient fusedLocationClient;
    private RuleDatabaseHelper dbHelper;
    private AudioManager audioManager;
    private LocationCallback locationCallback;

    @Override
    public void onCreate() {
        super.onCreate();

        String channelId = getString(R.string.notification_channel_id);
        createForegroundNotificationChannel(channelId);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
                .setContentTitle(getString(com.yet.smartsilence.R.string.app_name))
                .setContentText("ניטור מיקום פועל")
                .setSmallIcon(R.drawable.ic_notification_icon)
                .setPriority(NotificationCompat.PRIORITY_LOW);

        startForeground(2001, builder.build());

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        dbHelper = new RuleDatabaseHelper(this);
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) return;
                Location location = locationResult.getLastLocation();
                checkAndApplyLocationRules(location);
            }
        };

        requestLocationUpdates();
    }

    private void createForegroundNotificationChannel(String channelId) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    getString(R.string.notification_channel_name),
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription(getString(R.string.notification_channel_desc));
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void requestLocationUpdates() {
        LocationRequest locationRequest = new LocationRequest.Builder(
                Priority.PRIORITY_BALANCED_POWER_ACCURACY, 10_000)
                .setMinUpdateIntervalMillis(5_000)
                .build();

        try {
            fusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    locationCallback,
                    Looper.getMainLooper()
            );
        } catch (SecurityException e) {
            Log.e("SmartSilence", "Missing location permission", e);
        }
    }



    private void checkAndApplyLocationRules(Location location) {
        boolean isAppActive = getSharedPreferences("settings_prefs", MODE_PRIVATE)
                .getBoolean("app_active", true);
        if (!isAppActive) return;

        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (!nm.isNotificationPolicyAccessGranted()) return;

        boolean locationRuleActive = false;
        List<RuleModel> locationRules = dbHelper.getAllRules();
        for (RuleModel rule : locationRules) {
            if ("location".equals(rule.getType()) && rule.isActive()) {
                float[] result = new float[1];
                Location.distanceBetween(
                        location.getLatitude(), location.getLongitude(),
                        rule.getLatitude(), rule.getLongitude(),
                        result
                );
                float distance = result[0];
                if (distance <= rule.getRadius()) {
                    locationRuleActive = true;
                    break;
                }
            }
        }

        getSharedPreferences("smartsilence_state", MODE_PRIVATE)
                .edit()
                .putBoolean("location_rule_active", locationRuleActive)
                .apply();

        com.yet.smartsilence.utils.RuleStateManager.updateRingerMode(this);
    }



    private void setRingerMode(int mode, String reason) {
        if (audioManager.getRingerMode() != mode) {
            audioManager.setRingerMode(mode);

            // בדיקת האם לשלוח התראות
            boolean notificationsEnabled = getSharedPreferences("settings_prefs", MODE_PRIVATE)
                    .getBoolean("notifications_enabled", true);
            if (notificationsEnabled) {
                NotificationHelper.showRingerModeChanged(this, mode);
                Log.d("SmartSilence", "Notification sent (location-based, " + reason + ")");
            } else {
                Log.d("SmartSilence", "Notifications are disabled in settings, not sending.");
            }
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        fusedLocationClient.removeLocationUpdates(locationCallback);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
