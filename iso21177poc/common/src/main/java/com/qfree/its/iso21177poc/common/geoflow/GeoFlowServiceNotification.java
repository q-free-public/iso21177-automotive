package com.qfree.its.iso21177poc.common.geoflow;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import androidx.core.app.NotificationCompat;

import com.qfree.its.iso21177poc.common.R;

public class GeoFlowServiceNotification {
    private static final String CHANNEL_ID = "LOCATION_SERVICE_NOTIFICATION_CHANNEL";
    private static final int NOTIFICATION_ID = 1;


    //TODO: Must consider car usage
    public static NotificationCompat.Builder buildNotification(Context context){
        Intent notificationIntent = new Intent(context, GeoFlowService.class);//new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, notificationIntent, 0);

        return new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(context.getString(R.string.app_name))
                .setContentText("test")
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);
    }

    public static Notification createNotificationChannel(Context context){
        int importance = NotificationManager.IMPORTANCE_DEFAULT;
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_ID, importance);
        channel.setDescription(CHANNEL_ID);
        NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(channel);
        Notification notification = buildNotification(context).build();
        notificationManager.notify(NOTIFICATION_ID, notification);
        return notification;
    }

}
