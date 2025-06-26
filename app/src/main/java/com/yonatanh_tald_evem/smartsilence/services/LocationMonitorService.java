package com.yonatanh_tald_evem.smartsilence.services;

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
import com.yonatanh_tald_evem.smartsilence.R;
import com.yonatanh_tald_evem.smartsilence.database.RuleDatabaseHelper;
import com.yonatanh_tald_evem.smartsilence.database.models.RuleModel;
import com.yonatanh_tald_evem.smartsilence.utils.NotificationHelper;

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
                .setContentTitle(getString(R.string.app_name))
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

    /**
     * עדכון location_rule_active גם אם אין אף חוק תקף!
     * קריאה ל-RuleStateManager תמיד.
     */
    private void checkAndApplyLocationRules(Location location) {
        Log.d("SmartSilence", "Received location: Lat=" + location.getLatitude() + ", Lon=" + location.getLongitude());

        boolean isAppActive = getSharedPreferences("settings_prefs", MODE_PRIVATE)
                .getBoolean("app_active", true);
        if (!isAppActive) return;

        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (!nm.isNotificationPolicyAccessGranted()) return;

        boolean locationRuleActive = false;
        RuleModel activeRule = null;
        List<RuleModel> locationRules = dbHelper.getActiveLocationRules();

        for (RuleModel rule : locationRules) {
            float[] result = new float[1];
            Location.distanceBetween(
                    location.getLatitude(), location.getLongitude(),
                    rule.getLatitude(), rule.getLongitude(),
                    result
            );
            float distance = result[0];

            boolean isInside = distance <= rule.getRadius();

            // עדכון העמודה nowActive לפי האם החוק תקף
            rule.setNowActive(isInside);
            dbHelper.updateNowActiveStatus(rule.getId(), isInside);

            if (isInside && !locationRuleActive) {
                locationRuleActive = true;
                activeRule = rule;
            }
        }

        Log.d("SmartSilence", "checkAndApplyLocationRules: locationRuleActive=" + locationRuleActive +
                (activeRule != null ? ", rule=" + activeRule.getLocationName() : ""));

        getSharedPreferences("smartsilence_state", MODE_PRIVATE)
                .edit()
                .putBoolean("location_rule_active", locationRuleActive)
                .apply();

        com.yonatanh_tald_evem.smartsilence.utils.RuleStateManager.updateRingerMode(this);
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
