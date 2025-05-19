package com.yet.smartsilence.utils;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.yet.smartsilence.R;
import com.yet.smartsilence.activities.HomeActivity;

public class NotificationHelper {

    public static void showRingerModeChanged(Context context, int ringerMode) {
        String channelId = context.getString(R.string.notification_channel_id);
        createNotificationChannel(context, channelId);

        String title = context.getString(R.string.notification_title);
        String message;
        int iconRes = R.drawable.ic_notification_icon;

        switch (ringerMode) {
            case android.media.AudioManager.RINGER_MODE_SILENT:
                message = context.getString(R.string.notification_silence);
                break;
            case android.media.AudioManager.RINGER_MODE_VIBRATE:
                message = context.getString(R.string.notification_vibrate);
                break;
            case android.media.AudioManager.RINGER_MODE_NORMAL:
            default:
                message = context.getString(R.string.notification_normal);
                break;
        }

        Intent intent = new Intent(context, HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Notification notification = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(iconRes)
                .setContentTitle(title)
                .setContentText(message)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build();

        NotificationManager manager = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(1001, notification);
        Log.d("SmartSilence", "Notification sent: " + message);
    }

    private static void createNotificationChannel(Context context, String channelId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String channelName = context.getString(R.string.notification_channel_name);
            String channelDesc = context.getString(R.string.notification_channel_desc);
            NotificationChannel channel = new NotificationChannel(
                    channelId, channelName, NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription(channelDesc);
            channel.setBypassDnd(true);
            NotificationManager manager = (NotificationManager)
                    context.getSystemService(Context.NOTIFICATION_SERVICE);
            manager.createNotificationChannel(channel);
            Log.d("SmartSilence", "Notification channel '" + channelName + "' created (id: " + channelId + ") [Bypass DND]");
        }
    }

}
