package io.github.frostie77.dadnme;

import android.app.Activity;
import android.content.Intent;
import android.content.Context;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.webkit.WebViewAssetLoader;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;

/** First standalone loading/input smoke-test. FrostFrontend is integrated later. */
public final class MainActivity extends Activity {
    private static final int IMPORT_REQUEST = 7;
    private static final String SWF_NAME = "dad-n-me.swf";
    private FrameLayout root;
    private WebView webView;
    private boolean stickUp, stickDown, stickLeft, stickRight;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);
        setContentView(root);
        if (gameFile().isFile()) startGame(); else showImport();
    }

    private File gameFile() { return new File(getFilesDir(), SWF_NAME); }

    private void showImport() {
        root.removeAllViews();
        LinearLayout panel = new LinearLayout(this);
        panel.setGravity(Gravity.CENTER);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(24, 24, 24, 24);
        TextView title = new TextView(this);
        title.setText("Dad 'n Me — Android Test");
        title.setTextColor(Color.WHITE);
        title.setTextSize(25);
        title.setGravity(Gravity.CENTER);
        panel.addView(title);
        TextView details = new TextView(this);
        details.setText("Import your original Dad 'n Me SWF to start. The file is stored privately by this app.");
        details.setTextColor(0xFFD1C9D1);
        details.setTextSize(15);
        details.setGravity(Gravity.CENTER);
        panel.addView(details);
        Button pick = new Button(this);
        pick.setText("SELECT SWF FILE");
        pick.setOnClickListener(v -> pickFile());
        panel.addView(pick);
        root.addView(panel, new FrameLayout.LayoutParams(-1, -1));
    }

    private void pickFile() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, IMPORT_REQUEST);
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != IMPORT_REQUEST || resultCode != RESULT_OK || data == null || data.getData() == null) return;
        File tmp = new File(getFilesDir(), SWF_NAME + ".tmp");
        try (InputStream in = getContentResolver().openInputStream(data.getData());
             FileOutputStream out = new FileOutputStream(tmp)) {
            if (in == null) throw new IOException("File could not be opened");
            byte[] buf = new byte[65536];
            int count;
            long total = 0;
            while ((count = in.read(buf)) > 0) {
                total += count;
                if (total > 40_000_000L) throw new IOException("SWF too large");
                out.write(buf, 0, count);
            }
            out.flush();
        } catch (Exception e) {
            tmp.delete();
            showError("Unable to import: " + e.getMessage());
            return;
        }
        try (FileInputStream in = new FileInputStream(tmp)) {
            byte[] head = new byte[4];
            if (in.read(head) != 4 || head[1] != 'W' || head[2] != 'S' ||
                    (head[0] != 'C' && head[0] != 'F' && head[0] != 'Z'))
                throw new IOException("This is not a Flash SWF file");
            File dst = gameFile();
            if (dst.exists() && !dst.delete()) throw new IOException("Can't replace old file");
            if (!tmp.renameTo(dst)) throw new IOException("Can't save imported file");
            startGame();
        } catch (Exception e) {
            tmp.delete();
            showError("Unable to import: " + e.getMessage());
        }
    }

    private void showError(String message) {
        showImport();
        android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_LONG).show();
    }

    private void startGame() {
        root.removeAllViews();
        if (webView != null) { webView.destroy(); webView = null; }
        webView = new WebView(this);
        webView.setBackgroundColor(Color.BLACK);
        webView.setFocusable(true);
        webView.setFocusableInTouchMode(true);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);

        WebViewAssetLoader loader = new WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(this))
            .addPathHandler("/game/", path -> {
                if (!SWF_NAME.equals(path.startsWith("/") ? path.substring(1) : path)) return null;
                try { return new WebResourceResponse("application/x-shockwave-flash", null,
                    new FileInputStream(gameFile())); }
                catch (IOException e) { return null; }
            }).build();
        webView.setWebViewClient(new WebViewClient() {
            @Override public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                Uri u = request.getUrl();
                if (!"appassets.androidplatform.net".equals(u.getHost()) || !"https".equals(u.getScheme()))
                    return new WebResourceResponse("text/plain", "UTF-8",
                        new java.io.ByteArrayInputStream(new byte[0]));
                return loader.shouldInterceptRequest(u);
            }
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return !"appassets.androidplatform.net".equals(request.getUrl().getHost());
            }
        });
        webView.setWebChromeClient(new WebChromeClient() {
            @Override public boolean onConsoleMessage(android.webkit.ConsoleMessage m) {
                Log.d("DadNMeRuffle", m.message());
                return true;
            }
        });
        root.addView(webView, new FrameLayout.LayoutParams(-1, -1));
        webView.loadUrl("https://appassets.androidplatform.net/assets/www/index.html");
        webView.requestFocus();
    }

    private void jsKey(String key, boolean down, String source) {
        if (webView == null) return;
        // All values are fixed strings from the input map below, not user input.
        webView.evaluateJavascript("window.dadKey && window.dadKey('" + key + "'," + down + ",'" + source + "')", null);
    }

    private String keyFor(int code) {
        switch (code) {
            case KeyEvent.KEYCODE_DPAD_UP: return "up";
            case KeyEvent.KEYCODE_DPAD_DOWN: return "down";
            case KeyEvent.KEYCODE_DPAD_LEFT: return "left";
            case KeyEvent.KEYCODE_DPAD_RIGHT: return "right";
            case KeyEvent.KEYCODE_BUTTON_A: return "a";
            case KeyEvent.KEYCODE_BUTTON_X: return "s";
            default: return null;
        }
    }

    @Override public boolean dispatchKeyEvent(KeyEvent event) {
        String key = keyFor(event.getKeyCode());
        if (key != null && webView != null &&
                (event.getAction() == KeyEvent.ACTION_DOWN || event.getAction() == KeyEvent.ACTION_UP)) {
            jsKey(key, event.getAction() == KeyEvent.ACTION_DOWN, "buttons");
            return true;
        }
        return super.dispatchKeyEvent(event);
    }

    @Override public boolean dispatchGenericMotionEvent(MotionEvent e) {
        if (webView != null && (e.getSource() & InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK &&
                e.getAction() == MotionEvent.ACTION_MOVE) {
            float x = e.getAxisValue(MotionEvent.AXIS_X), y = e.getAxisValue(MotionEvent.AXIS_Y);
            boolean l = x < -0.35f, r = x > 0.35f, u = y < -0.35f, d = y > 0.35f;
            if (l != stickLeft) { jsKey("left", l, "stick"); stickLeft = l; }
            if (r != stickRight) { jsKey("right", r, "stick"); stickRight = r; }
            if (u != stickUp) { jsKey("up", u, "stick"); stickUp = u; }
            if (d != stickDown) { jsKey("down", d, "stick"); stickDown = d; }
            return true;
        }
        return super.dispatchGenericMotionEvent(e);
    }

    @Override protected void onPause() { super.onPause(); if(webView!=null)webView.onPause(); }
    @Override protected void onResume() { super.onResume(); if(webView!=null)webView.onResume(); }
    @Override protected void onDestroy() {
        if (webView != null) { root.removeView(webView); webView.destroy(); webView = null; }
        super.onDestroy();
    }
}
