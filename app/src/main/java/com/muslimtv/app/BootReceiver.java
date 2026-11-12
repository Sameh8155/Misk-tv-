package com.muslimtv.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class BootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c, Intent i) {
        Intent s = new Intent(c, AdhanService.class);
        try {
            if (Build.VERSION.SDK_INT >= 26) c.startForegroundService(s); else c.startService(s);
        } catch (Exception ignored) {}
        // على أجهزة أندرويد القديمة يفتح الشاشة تلقائيًا بعد التشغيل (الأحدث تمنعه النظام)
        if (Build.VERSION.SDK_INT < 29 && Intent.ACTION_BOOT_COMPLETED.equals(i.getAction())) {
            try {
                Intent a = new Intent(c, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                c.startActivity(a);
            } catch (Exception ignored) {}
        }
    }
}
