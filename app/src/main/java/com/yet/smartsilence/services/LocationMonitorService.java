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
                Priority.PRIORITY_BALANCED_POWER_ACCURACY, 600_000)
                .setMinUpdateIntervalMillis(300_000)
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
        if (!isAppActive) {
            Log.d("SmartSilence", "App setting disabled, no change (location)");
            return;
        }

        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (!nm.isNotificationPolicyAccessGranted()) {
            Log.w("SmartSilence", "Notification policy access not granted. Cannot change ringer mode (location).");
            return;
        }

        List<RuleModel> locationRules = dbHelper.getAllRules();
        for (RuleModel rule : locationRules) {
            Log.d("SmartSilence", "Rule: " + rule.getType() + ", active=" + rule.isActive() + ", loc=" + rule.getLatitude() + "/" + rule.getLongitude() + ", radius=" + rule.getRadius());
            if ("location".equals(rule.getType()) && rule.isActive()) {
                float[] result = new float[1];
                Log.d("SmartSilence", "Location check: current=" + location.getLatitude() + "," + location.getLongitude() + " | rule=" + rule.getLatitude() + "," + rule.getLongitude() + " | radius=" + rule.getRadius());
                Location.distanceBetween(
                        location.getLatitude(), location.getLongitude(),
                        rule.getLatitude(), rule.getLongitude(),
                        result
                );
                float distance = result[0];
                if (distance <= rule.getRadius()) {
                    setRingerMode(AudioManager.RINGER_MODE_SILENT, "נכנסת לאזור שקט: " + rule.getLocationName());
                    Log.d("SmartSilence", "Entered silent zone: " + rule.getLocationName());
                    return;
                }
            }
        }
        // לא נכנס לאף אזור: מצב רגיל
        setRingerMode(AudioManager.RINGER_MODE_NORMAL, "לא נמצאים באף אזור שקט");
        Log.d("SmartSilence", "No active location rule, ringer mode NORMAL");
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
