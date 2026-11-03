package com.kyenglish2.offline;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;
import android.widget.Toast;
import androidx.webkit.WebViewAssetLoader;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final String TAG = "KYEnglish2";
    private static final String STUDY_URL = "https://appassets.androidplatform.net/assets/index.html";
    private static final int PICK_FILE_REQUEST = 101;
    private WebView webView;
    private TextToSpeech tts;
    private volatile boolean ttsReady = false;
    private ValueCallback<Uri[]> fileCallback;
    private boolean initialLoadComplete = false;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        try {
            WebViewAssetLoader loader = new WebViewAssetLoader.Builder()
                    .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(this))
                    .build();
            webView = new WebView(this);
            WebSettings cfg = webView.getSettings();
            cfg.setJavaScriptEnabled(true);
            cfg.setDomStorageEnabled(true);
            cfg.setAllowFileAccess(false);
            cfg.setAllowContentAccess(true);
            cfg.setJavaScriptCanOpenWindowsAutomatically(false);
            cfg.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
            webView.setWebViewClient(new WebViewClient() {
                @Override public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest req) {
                    return loader.shouldInterceptRequest(req.getUrl());
                }
                @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest req) {
                    Uri u = req.getUrl();
                    return !"appassets.androidplatform.net".equals(u.getHost());
                }
                @Override public void onPageFinished(WebView view, String url) {
                    initialLoadComplete = true;
                }
            });
            webView.setWebChromeClient(new WebChromeClient() {
                @Override public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback, FileChooserParams params) {
                    if (fileCallback != null) fileCallback.onReceiveValue(null);
                    fileCallback = callback;
                    try {
                        Intent intent = params.createIntent();
                        intent.addCategory(Intent.CATEGORY_OPENABLE);
                        startActivityForResult(intent, PICK_FILE_REQUEST);
                        return true;
                    } catch (ActivityNotFoundException e) {
                        fileCallback = null;
                        Toast.makeText(MainActivity.this, "请安装系统文件管理器", Toast.LENGTH_LONG).show();
                        return false;
                    }
                }
            });
            webView.addJavascriptInterface(new AudioBridge(), "AndroidAudio");
            setContentView(webView);
            tts = new TextToSpeech(this, status -> {
                ttsReady = status == TextToSpeech.SUCCESS;
                if (ttsReady && tts != null) tts.setLanguage(Locale.US);
            });
            webView.loadUrl(STUDY_URL + "#daily");
        } catch (Throwable e) {
            Log.e(TAG, "Initialization failed", e);
            TextView message = new TextView(this);
            message.setText("英语学习 App 启动失败。请更新 Android System WebView 或 Chrome 后重试。\n" + e.getClass().getSimpleName());
            message.setTextSize(18);
            message.setPadding(40, 60, 40, 40);
            setContentView(message);
        }
    }

    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (webView != null && initialLoadComplete) {
            String path = intent != null && intent.getData() != null ? intent.getData().getLastPathSegment() : null;
            String fragment = "daily";
            if ("review".equals(path)) fragment = "review";
            else if ("reading".equals(path)) fragment = "reading";
            webView.loadUrl(STUDY_URL + "#" + fragment);
        }
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_FILE_REQUEST && fileCallback != null) {
            fileCallback.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(resultCode, data));
            fileCallback = null;
        }
    }

    public final class AudioBridge {
        @JavascriptInterface public void speak(String words, float rate, String localeTag) {
            String msg = words == null ? "" : words.substring(0, Math.min(words.length(), 3500));
            runOnUiThread(() -> {
                if (!ttsReady || tts == null) return;
                try {
                    Locale lang = Locale.forLanguageTag(localeTag == null ? "en-US" : localeTag);
                    tts.setLanguage(lang);
                    tts.setSpeechRate(Math.max(0.65f, Math.min(1.6f, rate)));
                    tts.speak(msg, TextToSpeech.QUEUE_FLUSH, null, "ky2-speech");
                } catch (Exception err) { Log.w(TAG, "TTS failed", err); }
            });
        }
        @JavascriptInterface public void stop() {
            runOnUiThread(() -> { if (tts != null) tts.stop(); });
        }
        @JavascriptInterface public void shareJson(String name, String json) {
            runOnUiThread(() -> {
                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType("text/plain");
                intent.putExtra(Intent.EXTRA_SUBJECT, name);
                intent.putExtra(Intent.EXTRA_TEXT, json);
                try { startActivity(Intent.createChooser(intent, "保存学习记录")); }
                catch (ActivityNotFoundException e) { Toast.makeText(MainActivity.this, "未找到可分享的应用", Toast.LENGTH_LONG).show(); }
            });
        }
    }

    @Override public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }

    @Override protected void onDestroy() {
        if (fileCallback != null) { fileCallback.onReceiveValue(null); fileCallback = null; }
        if (tts != null) { tts.stop(); tts.shutdown(); }
        if (webView != null) { webView.destroy(); webView = null; }
        super.onDestroy();
    }
}
