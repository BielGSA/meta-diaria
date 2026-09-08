package com.tk27labs.drivemeta.shortcut;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private static final String DRIVEMETA_URL = "https://bielgsa.github.io/meta-diaria/";
    private boolean waitingForOverlayPermission = false;
    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FrameLayout root = new FrameLayout(this);

        webView = new WebView(this);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                String host = uri.getHost();
                if (host != null && (host.equals("bielgsa.github.io") || host.endsWith(".github.io"))) {
                    return false;
                }
                startActivity(new Intent(Intent.ACTION_VIEW, uri));
                return true;
            }
        });
        webView.loadUrl(DRIVEMETA_URL);

        root.addView(webView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        TextView settingsButton = new TextView(this);
        settingsButton.setText("⚙");
        settingsButton.setTextSize(24);
        settingsButton.setGravity(Gravity.CENTER);
        settingsButton.setBackgroundColor(0xDD0B1219);
        settingsButton.setTextColor(0xFFFFFFFF);
        settingsButton.setElevation(dp(8));
        settingsButton.setOnClickListener(v -> showShortcutSettings());

        FrameLayout.LayoutParams buttonParams = new FrameLayout.LayoutParams(dp(52), dp(52));
        buttonParams.gravity = Gravity.TOP | Gravity.END;
        buttonParams.topMargin = dp(16);
        buttonParams.rightMargin = dp(16);
        root.addView(settingsButton, buttonParams);

        setContentView(root);
    }

    private void showShortcutSettings() {
        String[] options = new String[]{
                "Ativar atalho flutuante",
                "Desativar atalho flutuante"
        };

        new AlertDialog.Builder(this)
                .setTitle("Atalho flutuante")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        enableBubble();
                    } else {
                        stopService(new Intent(this, BubbleService.class));
                        Toast.makeText(this, "Atalho flutuante desativado", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Fechar", null)
                .show();
    }

    private void enableBubble() {
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1002);
        }

        if (!Settings.canDrawOverlays(this)) {
            waitingForOverlayPermission = true;
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivity(intent);
            return;
        }
        startBubbleService();
    }

    private void startBubbleService() {
        Intent service = new Intent(this, BubbleService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(service);
        } else {
            startService(service);
        }
        Toast.makeText(this, "Atalho flutuante ativado", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (waitingForOverlayPermission && Settings.canDrawOverlays(this)) {
            waitingForOverlayPermission = false;
            startBubbleService();
        }
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.destroy();
        }
        super.onDestroy();
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }
}
