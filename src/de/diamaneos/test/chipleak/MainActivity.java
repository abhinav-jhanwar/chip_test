/*
 * Copyright (C) 2026 The DiamaneOS Project
 * SPDX-License-Identifier: Apache-2.0
 */
package de.diamaneos.test.chipleak;

import android.Manifest;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Posts one promoted ongoing notification ("Live Update") with visibility PRIVATE. Its private
 * version's chip text is "4827"; its public version's is "PUB". On a locked phone that
 * hides sensitive notification content, the status bar must never show "4827".
 */
public class MainActivity extends Activity {
    private static final String CHANNEL = "live";
    private static final int ID = 1;
    // Notification.EXTRA_REQUEST_PROMOTED_ONGOING (Builder.setRequestPromotedOngoing is newer
    // than the API 36 SDK this is built against).
    private static final String EXTRA_REQUEST_PROMOTED_ONGOING = "android.requestPromotedOngoing";

    private TextView status;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        NotificationManager nm = getSystemService(NotificationManager.class);
        nm.createNotificationChannel(
                new NotificationChannel(CHANNEL, "Live test", NotificationManager.IMPORTANCE_DEFAULT));
        if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[] {Manifest.permission.POST_NOTIFICATIONS}, 1);
        }
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (24 * getResources().getDisplayMetrics().density);
        root.setPadding(pad, pad * 3, pad, pad);
        Button post = new Button(this);
        post.setText("Post Live Update");
        post.setOnClickListener(v -> post());
        Button cancel = new Button(this);
        cancel.setText("Cancel");
        cancel.setOnClickListener(v -> {
            nm.cancel(ID);
            update(null);
        });
        status = new TextView(this);
        root.addView(post);
        root.addView(cancel);
        root.addView(status);
        setContentView(root);
        update(null);
    }

    private Notification.Builder base(String title, String text, String chip) {
        Notification.Builder b = new Notification.Builder(this, CHANNEL)
                .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
                .setContentTitle(title)
                .setContentText(text)
                .setShortCriticalText(chip)
                .setOngoing(true);
        Bundle extras = new Bundle();
        extras.putBoolean(EXTRA_REQUEST_PROMOTED_ONGOING, true);
        b.addExtras(extras);
        return b;
    }

    private void post() {
        PendingIntent open = PendingIntent.getActivity(this, 0,
                new Intent(this, MainActivity.class), PendingIntent.FLAG_IMMUTABLE);
        Notification pub = base("Hidden", "Public version", "PUB").build();
        Notification n = base("Private title", "Private body 1234", "4827")
                .setVisibility(Notification.VISIBILITY_PRIVATE)
                .setPublicVersion(pub)
                .setContentIntent(open)
                .build();
        getSystemService(NotificationManager.class).notify(ID, n);
        update(n);
    }

    private void update(Notification n) {
        NotificationManager nm = getSystemService(NotificationManager.class);
        StringBuilder s = new StringBuilder();
        s.append("notifications allowed: ").append(nm.areNotificationsEnabled());
        s.append("\ncan post promoted: ").append(nm.canPostPromotedNotifications());
        if (n != null) {
            s.append("\nposted; promotable: ").append(n.hasPromotableCharacteristics());
        }
        status.setText(s);
    }
}
