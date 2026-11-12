package com.muslimtv.app;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetFileDescriptor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.io.FileInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends Activity {
    private static final String HOST = "appassets.androidplatform.net";
    private WebView web;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (Build.VERSION.SDK_INT >= 33) {
            try { requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1); } catch (Exception ignored) {}
        }
        startAdhanService();

        web = new WebView(this);
        setContentView(web);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setAllowFileAccess(false);
        s.setGeolocationEnabled(true);
        web.setBackgroundColor(0xFF1F7FD6);
        web.setWebChromeClient(new WebChromeClient() {
            @Override public void onGeolocationPermissionsShowPrompt(String o, android.webkit.GeolocationPermissions.Callback cb) { cb.invoke(o, true, false); }
        });
        web.addJavascriptInterface(new Bridge(), "MiskNative");
        web.setWebViewClient(new WebViewClient() {
            @Override public WebResourceResponse shouldInterceptRequest(WebView v, WebResourceRequest r) {
                Uri u = r.getUrl();
                if (!HOST.equals(u.getHost())) return null;
                String path = u.getPath();
                if (path == null || path.equals("/")) path = "/index.html";
                String name = path.substring(1);
                try {
                    String mime = name.endsWith(".mp4") ? "video/mp4" : name.endsWith(".mp3") ? "audio/mpeg"
                            : name.endsWith(".html") ? "text/html" : "application/octet-stream";
                    if (name.endsWith(".mp4") || name.endsWith(".mp3")) return ranged(r, name, mime);
                    InputStream in = getAssets().open(name);
                    return new WebResourceResponse(mime, "utf-8", in);
                } catch (Exception e) { return null; }
            }
        });
        web.loadUrl("https://" + HOST + "/index.html");
        hideBars();
    }

    /** يخدّم الفيديو والصوت مع دعم Range عشان التشغيل المتكرر (loop) يشتغل صح. */
    private WebResourceResponse ranged(WebResourceRequest r, String name, String mime) throws IOException {
        AssetFileDescriptor afd = getAssets().openFd(name);
        long len = afd.getLength(), start = 0, end = len - 1;
        int code = 200; String reason = "OK";
        String range = r.getRequestHeaders().get("Range");
        if (range != null && range.startsWith("bytes=")) {
            String[] p = range.substring(6).split("-");
            try {
                start = Long.parseLong(p[0].trim());
                if (p.length > 1 && !p[1].trim().isEmpty()) end = Math.min(len - 1, Long.parseLong(p[1].trim()));
                code = 206; reason = "Partial Content";
            } catch (Exception e) { start = 0; end = len - 1; }
        }
        final long count = end - start + 1;
        FileInputStream fis = afd.createInputStream();
        long skipped = 0;
        while (skipped < start) { long k = fis.skip(start - skipped); if (k <= 0) break; skipped += k; }
        InputStream lim = new FilterInputStream(fis) {
            long left = count;
            @Override public int read() throws IOException { if (left <= 0) return -1; int x = super.read(); if (x >= 0) left--; return x; }
            @Override public int read(byte[] b, int o, int l) throws IOException {
                if (left <= 0) return -1;
                int n = super.read(b, o, (int) Math.min(l, left)); if (n > 0) left -= n; return n;
            }
        };
        Map<String, String> h = new HashMap<>();
        h.put("Accept-Ranges", "bytes");
        h.put("Content-Length", String.valueOf(count));
        h.put("Content-Range", "bytes " + start + "-" + end + "/" + len);
        h.put("Access-Control-Allow-Origin", "*");
        return new WebResourceResponse(mime, null, code, reason, h, lim);
    }

    private void startAdhanService() {
        Intent s = new Intent(this, AdhanService.class);
        try { if (Build.VERSION.SDK_INT >= 26) startForegroundService(s); else startService(s); } catch (Exception ignored) {}
    }

    private void hideBars() {
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
    }

    @Override protected void onResume() { super.onResume(); hideBars(); if (web != null) web.onResume(); startAdhanService(); }
    // لا نوقف الـ WebView عند الخروج من الشاشة: الأذان بيشتغل من الخدمة
    @Override public void onBackPressed() { moveTaskToBack(true); }

    /** جسر بين الصفحة والخدمة */
    class Bridge {
        @JavascriptInterface public void setSchedule(String json) {
            getSharedPreferences(AdhanService.PREFS, MODE_PRIVATE).edit().putString("sched", json).apply();
        }
        @JavascriptInterface public void play(int voice) { AdhanService.playNow(voice); }
        @JavascriptInterface public void stop() { AdhanService.stopNow(); }
        @JavascriptInterface public boolean playing() { return AdhanService.isPlaying(); }
    }
}
