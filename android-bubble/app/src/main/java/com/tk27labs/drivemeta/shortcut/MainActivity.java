package com.tk27labs.drivemeta.shortcut;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {
    private boolean waitingForOverlayPermission = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        int pad = (int) (24 * getResources().getDisplayMetrics().density);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(pad, pad, pad, pad);

        TextView title = new TextView(this);
        title.setText("DriveMeta — bolha de atalho");
        title.setTextSize(24);
        title.setGravity(Gravity.CENTER);

        TextView info = new TextView(this);
        info.setText("A bolha serve somente para abrir o DriveMeta rapidamente sobre Uber, 99, inDrive e outros apps.");
        info.setTextSize(16);
        info.setGravity(Gravity.CENTER);
        info.setPadding(0, pad, 0, pad);

        Button activate = new Button(this);
        activate.setText("Ativar bolha");
        activate.setOnClickListener(v -> enableBubble());

        Button stop = new Button(this);
        stop.setText("Desativar bolha");
        stop.setOnClickListener(v -> stopService(new Intent(this, BubbleService.class)));

        root.addView(title);
        root.addView(info);
        root.addView(activate);
        root.addView(stop);
        setContentView(root);
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
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (waitingForOverlayPermission && Settings.canDrawOverlays(this)) {
            waitingForOverlayPermission = false;
            startBubbleService();
        }
    }
}
