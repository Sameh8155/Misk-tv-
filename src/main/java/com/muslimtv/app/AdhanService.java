package com.muslimtv.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetFileDescriptor;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;

import org.json.JSONArray;
import org.json.JSONObject;

/** خدمة تعمل في الخلفية دائمًا وتشغّل الأذان في وقته حتى لو التطبيق مقفول. */
public class AdhanService extends Service {
    static final String PREFS = "misk";
    static final String[] FILES = {"makkah", "basit1", "basit2", "mustafa"};
    private static AdhanService inst;

    private final Handler h = new Handler(Looper.getMainLooper());
    private MediaPlayer mp;
    private PowerManager.WakeLock wl;
    private long lastFired = 0;

    private final Runnable loop = new Runnable() {
        @Override public void run() {
            try { check(); } catch (Exception ignored) {}
            h.postDelayed(this, 3000);
        }
    };

    @Override public void onCreate() {
        super.onCreate();
        inst = this;
        startFg("Misk TV", "يعمل في الخلفية ويرفع الأذان في وقته");
        h.post(loop);
    }

    @Override public int onStartCommand(Intent i, int f, int id) { return START_STICKY; }
    @Override public IBinder onBind(Intent i) { return null; }

    @Override public void onDestroy() {
        h.removeCallbacksAndMessages(null);
        stopAdhan();
        inst = null;
        super.onDestroy();
    }

    @Override public void onTaskRemoved(Intent root) {
        // لو التطبيق اتقفل من القائمة، الخدمة تفضل شغالة
        super.onTaskRemoved(root);
    }

    private void startFg(String title, String text) {
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= 26) {
            nm.createNotificationChannel(new NotificationChannel("misk", "Misk TV", NotificationManager.IMPORTANCE_LOW));
        }
        PendingIntent pi = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0));
        Notification.Builder b = Build.VERSION.SDK_INT >= 26 ? new Notification.Builder(this, "misk") : new Notification.Builder(this);
        b.setContentTitle(title).setContentText(text).setSmallIcon(R.mipmap.ic_launcher).setContentIntent(pi).setOngoing(true);
        startForeground(1, b.build());
    }

    private void check() throws Exception {
        SharedPreferences sp = getSharedPreferences(PREFS, MODE_PRIVATE);
        String s = sp.getString("sched", null);
        if (s == null) return;
        JSONObject o = new JSONObject(s);
        if (o.optInt("on", 1) == 0) return;
        JSONArray it = o.getJSONArray("items");
        long now = System.currentTimeMillis();
        for (int i = 0; i < it.length(); i++) {
            JSONObject e = it.getJSONObject(i);
            long t = e.getLong("t");
            if (now >= t && now < t + 60000 && lastFired != t) {
                lastFired = t;
                int v = e.optInt("v", 0);
                if (v >= 0 && v < FILES.length) {
                    play(v);
                    startFg("حان الآن موعد أذان " + e.optString("n"), "Misk TV");
                }
                return;
            }
        }
    }

    void play(int v) {
        stopAdhan();
        try {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "misk:adhan");
            wl.acquire(10 * 60 * 1000L);
            AssetFileDescriptor afd = getAssets().openFd("adhan/" + FILES[v] + ".mp3");
            mp = new MediaPlayer();
            mp.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build());
            mp.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            afd.close();
            mp.setOnCompletionListener(m -> stopAdhan());
            mp.setOnErrorListener((m, a, b) -> { stopAdhan(); return true; });
            mp.prepare();
            mp.start();
        } catch (Exception e) { stopAdhan(); }
    }

    void stopAdhan() {
        try { if (mp != null) { mp.release(); } } catch (Exception ignored) {}
        mp = null;
        try { if (wl != null && wl.isHeld()) wl.release(); } catch (Exception ignored) {}
        wl = null;
    }

    static void playNow(int v) { if (inst != null) inst.play(v); }
    static void stopNow() { if (inst != null) inst.stopAdhan(); }
    static boolean isPlaying() { return inst != null && inst.mp != null; }
}
