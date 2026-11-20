package com.example.overlay;

import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    private WindowManager wm;
    private View overlayView;
    private PowerManager.WakeLock wakeLock;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
                Intent intent = new Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName())
                );
                startActivity(intent);
                return;
            }

            applyKeyguardIfEnabled();
            acquireWakeLockIfEnabled();
            showOverlay();
        } catch (Exception e) {
            Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void applyKeyguardIfEnabled() {
        try {
            if (!OverlayConfig.KEYGUARD_ENABLED) {
                return;
            }
            KeyguardManager km = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
            if (km != null && "DISMISS".equals(OverlayConfig.KEYGUARD_MODE) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                km.requestDismissKeyguard(this, null);
            }
        } catch (Exception e) {
            Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void acquireWakeLockIfEnabled() {
        try {
            if (!OverlayConfig.WAKE_LOCK_ENABLED) {
                return;
            }
            PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
            if (pm != null) {
                wakeLock = pm.newWakeLock(OverlayConfig.WAKE_LOCK_FLAG, getPackageName() + ":OverlayWL");
                wakeLock.setReferenceCounted(false);
                if (!wakeLock.isHeld()) {
                    wakeLock.acquire(10 * 60 * 1000L);
                }
            }
        } catch (Exception e) {
            Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private View buildViewFromCode() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(32, 32, 32, 32);

        TextView label = new TextView(this);
        label.setText("Overlay (" + OverlayConfig.ACCENT_HEX + ")");
        root.addView(label);

        final EditText phraseInput = new EditText(this);
        phraseInput.setHint("Destroy phrase");
        root.addView(phraseInput);

        Button checkBtn = new Button(this);
        checkBtn.setText("Check Phrase");
        checkBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkPhraseAndDestroy(phraseInput.getText().toString());
            }
        });
        root.addView(checkBtn);
        return root;
    }

    private void showOverlay() {
        try {
            if ("from_code".equals(OverlayConfig.XML_MODE)) {
                overlayView = buildViewFromCode();
            } else {
                LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
                overlayView = inflater.inflate(R.layout.overlay_view, null);
            }

            overlayView.setAlpha(OverlayConfig.ALPHA);

            View btn = overlayView.findViewById(R.id.check_destroy_btn);
            final EditText input = overlayView.findViewById(R.id.destroy_phrase_input);
            if (btn != null && input != null) {
                btn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        checkPhraseAndDestroy(input.getText().toString());
                    }
                });
            }

            int windowType = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : OverlayConfig.LEGACY_WINDOW_TYPE;

            WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                windowType,
                OverlayConfig.EXTRA_FLAGS,
                PixelFormat.TRANSLUCENT
            );
            params.gravity = OverlayConfig.GRAVITY;

            if (overlayView.getParent() == null) {
                wm.addView(overlayView, params);
            }
        } catch (Exception e) {
            Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void checkPhraseAndDestroy(String enteredPhrase) {
        try {
            if (enteredPhrase != null && enteredPhrase.trim().equals(OverlayConfig.DESTROY_PHRASE.trim())) {
                destroyOverlay();
            } else {
                Toast.makeText(getApplicationContext(), "Phrase did not match", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void destroyOverlay() {
        try {
            if (overlayView != null && overlayView.getParent() != null) {
                wm.removeView(overlayView);
                overlayView = null;
            }
            if (wakeLock != null && wakeLock.isHeld()) {
                wakeLock.release();
            }
        } catch (Exception e) {
            Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
