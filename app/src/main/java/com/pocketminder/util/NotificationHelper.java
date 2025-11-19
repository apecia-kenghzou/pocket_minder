package com.pocketminder.util;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.pocketminder.MainActivity;
import com.pocketminder.R;

/**
 * Helper class for managing notifications
 */
public class NotificationHelper {
    private static final String CHANNEL_ID_LOCATION = "location_tracking";
    private static final String CHANNEL_ID_REMINDER = "shopping_reminder";
    private static final String CHANNEL_NAME_LOCATION = "Location Tracking";
    private static final String CHANNEL_NAME_REMINDER = "Shopping Reminders";

    public static final int NOTIFICATION_ID_FOREGROUND = 1001;
    public static final int NOTIFICATION_ID_REMINDER = 2001;

    private final Context context;
    private final NotificationManager notificationManager;

    public NotificationHelper(Context context) {
        this.context = context.getApplicationContext();
        this.notificationManager = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);
        createNotificationChannels();
    }

    /**
     * Create notification channels for Android O and above
     */
    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Location tracking channel
            NotificationChannel locationChannel = new NotificationChannel(
                    CHANNEL_ID_LOCATION,
                    CHANNEL_NAME_LOCATION,
                    NotificationManager.IMPORTANCE_LOW
            );
            locationChannel.setDescription("Ongoing notification for location tracking");
            locationChannel.setShowBadge(false);
            notificationManager.createNotificationChannel(locationChannel);

            // Shopping reminder channel
            NotificationChannel reminderChannel = new NotificationChannel(
                    CHANNEL_ID_REMINDER,
                    CHANNEL_NAME_REMINDER,
                    NotificationManager.IMPORTANCE_HIGH
            );
            reminderChannel.setDescription("Notifications when you're near a supermarket");
            reminderChannel.enableVibration(true);
            reminderChannel.setShowBadge(true);
            notificationManager.createNotificationChannel(reminderChannel);
        }
    }

    /**
     * Create foreground service notification
     */
    public android.app.Notification createForegroundNotification() {
        Intent notificationIntent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0,
                notificationIntent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );

        return new NotificationCompat.Builder(context, CHANNEL_ID_LOCATION)
                .setContentTitle("Pocket Minder Active")
                .setContentText("Monitoring your location for nearby supermarkets")
                .setSmallIcon(R.drawable.ic_notification)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .build();
    }

    /**
     * Show shopping reminder notification
     */
    public void showShoppingReminder(String supermarketName, int itemCount) {
        Intent notificationIntent = new Intent(context, MainActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0,
                notificationIntent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );

        String title = "Shopping Reminder!";
        String message = String.format("You're near %s. You have %d item%s to buy!",
                supermarketName, itemCount, itemCount > 1 ? "s" : "");

        android.app.Notification notification = new NotificationCompat.Builder(context, CHANNEL_ID_REMINDER)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setSmallIcon(R.drawable.ic_notification)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setVibrate(new long[]{0, 500, 200, 500})
                .build();

        notificationManager.notify(NOTIFICATION_ID_REMINDER, notification);
    }

    /**
     * Cancel reminder notification
     */
    public void cancelReminderNotification() {
        notificationManager.cancel(NOTIFICATION_ID_REMINDER);
    }

    /**
     * Update foreground notification
     */
    public void updateForegroundNotification(String contentText) {
        Intent notificationIntent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0,
                notificationIntent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );

        android.app.Notification notification = new NotificationCompat.Builder(context, CHANNEL_ID_LOCATION)
                .setContentTitle("Pocket Minder Active")
                .setContentText(contentText)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .build();

        notificationManager.notify(NOTIFICATION_ID_FOREGROUND, notification);
    }
}
