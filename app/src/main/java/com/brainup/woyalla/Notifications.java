package com.brainup.woyalla;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.format.Time;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * Created by Roger on 8/15/2016.
 */
public class Notifications {
    Context context;

    public Notifications(Context context){
        this.context = context;
    }

    public void buildNotification() {
        NotificationManager mgr = (NotificationManager) context.getSystemService(context.NOTIFICATION_SERVICE);
        Intent notificationIntent = new Intent(context, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(context, 0, notificationIntent, PendingIntent.FLAG_ONE_SHOT);
        Uri uri = Uri.parse("android.resource://" + context.getPackageName() + "/" + R.raw.ringtone);
        Notification.Builder builder = new Notification.Builder(context)
                .setContentTitle(context.getString(R.string.notification_title))
                .setContentIntent(pi)
                .setContentText(context.getString(R.string.notification_content))
                .setAutoCancel(true)
                .setDefaults(Notification.DEFAULT_LIGHTS | Notification.DEFAULT_VIBRATE)
                .setWhen(System.currentTimeMillis())
                .setSound(uri)
                .setSmallIcon(R.drawable.ic_taxi);

        Notification notification = builder.build();

        Calendar date = new GregorianCalendar();
        mgr.notify(1,notification);

    }
}
