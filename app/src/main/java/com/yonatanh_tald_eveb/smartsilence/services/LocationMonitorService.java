package com.yonatanh_tald_eveb.smartsilence.services;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
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
import com.yonatanh_tald_eveb.smartsilence.R;
import com.yonatanh_tald_eveb.smartsilence.database.RuleDatabaseHelper;
import com.yonatanh_tald_eveb.smartsilence.database.models.RuleModel;

import java.util.List;

public class LocationMonitorService extends Service {
    // Clients and managers
    private FusedLocationProviderClient fusedLocationClient;
    private RuleDatabaseHelper dbHelper;
    private AudioManager audioManager;
    private LocationCallback locationCallback;

    @Override
    public void onCreate() {
        super.onCreate();

        // Create notification channel and show foreground notification
        String channelId = getString(R.string.notification_channel_id);
        createForegroundNotificationChannel(channelId);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
                .setContentTitle(getString(R.string.app_name))
                .setContentText("ניטור מיקום פועל")
                .setSmallIcon(R.drawable.ic_notification_icon)
                .setPriority(NotificationCompat.PRIORITY_LOW);

        // Start service in the foreground
        startForeground(2001, builder.build());

        // Initialize location services and database
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        dbHelper = new RuleDatabaseHelper(this);
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        // Define location callback: triggered when location is received
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) return;
                Location location = locationResult.getLastLocation();
                checkAndApplyLocationRules(location);
            }
        };

        // Start location updates
        requestLocationUpdates();
    }

    // Return START_STICKY to keep service alive unless explicitly stopped
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        boolean forceRefresh = intent != null && intent.getBooleanExtra("forceRefresh", false);

        if (forceRefresh) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED ||
                    androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED) {

                fusedLocationClient.getLastLocation()
                        .addOnSuccessListener(location -> {
                            if (location != null) {
                                Log.d("SmartSilence", "Force refresh triggered after rule deletion");
                                checkAndApplyLocationRules(location);
                            } else {
                                Log.w("SmartSilence", "No last known location available for forceRefresh");
                            }
                        });

            } else {
                Log.w("SmartSilence", "Location permission not granted for forceRefresh");
            }
        }

        return START_STICKY;
    }

    // Stop location updates when service is destroyed
    @Override
    public void onDestroy() {
        super.onDestroy();
        fusedLocationClient.removeLocationUpdates(locationCallback);
    }

    // Not a bound service, return null
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    // Creates the notification channel
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

    // Requests periodic location updates (every 10 seconds, minimum 5 seconds)
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
     * This method checks if the current location matches any active location rules.
     * - Updates the `nowActive` field of each rule in the database.
     * - Updates shared preferences with whether a location rule is active.
     * - Calls `RuleStateManager.updateRingerMode` to adjust phone mode accordingly.
     */
    private void checkAndApplyLocationRules(Location location) {
        Log.d("SmartSilence", "Received location: Lat=" + location.getLatitude() + ", Lon=" + location.getLongitude());

        // Check if the app is currently active (user setting)
        boolean isAppActive = getSharedPreferences("settings_prefs", MODE_PRIVATE)
                .getBoolean("app_active", true);
        if (!isAppActive) return;

        // Check if DND access is granted
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (!nm.isNotificationPolicyAccessGranted()) return;

        boolean locationRuleActive = false;
        RuleModel activeRule = null;
        List<RuleModel> locationRules = dbHelper.getActiveLocationRules();

        // Iterate through all active location rules
        for (RuleModel rule : locationRules) {
            float[] result = new float[1];

            // Calculate distance between user and rule location
            Location.distanceBetween(
                    location.getLatitude(), location.getLongitude(),
                    rule.getLatitude(), rule.getLongitude(),
                    result
            );
            float distance = result[0];

            boolean isInside = distance <= rule.getRadius();

            // Update rule activation state in database
            rule.setNowActive(isInside);
            dbHelper.updateNowActiveStatus(rule.getId(), isInside);

            // Keep track of the first matching rule
            if (isInside && !locationRuleActive) {
                locationRuleActive = true;
                activeRule = rule;
            }
        }

        Log.d("SmartSilence", "checkAndApplyLocationRules: locationRuleActive=" + locationRuleActive +
                (activeRule != null ? ", rule=" + activeRule.getLocationName() : ""));

        // Save global flag indicating whether any location rule is active
        getSharedPreferences("smartsilence_state", MODE_PRIVATE)
                .edit()
                .putBoolean("location_rule_active", locationRuleActive)
                .apply();

        // Apply audio mode change if necessary
        com.yonatanh_tald_eveb.smartsilence.utils.RuleStateManager.updateRingerMode(this);
    }
}
