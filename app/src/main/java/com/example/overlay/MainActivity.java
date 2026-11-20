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
import android.view.Gravity;
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
            Toast.makeText(getApplicationContext(), "Init err: " + e.getMessage(), Toast.LENGTH_LONG).show();
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
            Toast.makeText(getApplicationContext(), "Keyguard err: " + e.getMessage(), Toast.LENGTH_LONG).show();
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
            Toast.makeText(getApplicationContext(), "WakeLock err: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private View buildViewFromCode() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(36, 36, 36, 36);
        root.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT
        ));

        TextView label = new TextView(this);
        label.setText("Full-Screen Overlay (" + OverlayConfig.ACCENT_HEX + ")");
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
                ? OverlayConfig.OVERLAY_TYPE_26_PLUS
                : OverlayConfig.LEGACY_WINDOW_TYPE;

            int combinedFlags = OverlayConfig.EXTRA_FLAGS
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;

            WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                windowType,
                combinedFlags,
                PixelFormat.TRANSLUCENT
            );
            params.gravity = OverlayConfig.GRAVITY;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                params.layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            }

            if (overlayView.getParent() == null) {
                wm.addView(overlayView, params);
            }
        } catch (Exception e) {
            Toast.makeText(getApplicationContext(), "Show err: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void checkPhraseAndDestroy(String enteredPhrase) {
        try {
            String actual = (enteredPhrase == null) ? "" : enteredPhrase.trim();
            String expected = (OverlayConfig.DESTROY_PHRASE == null) ? "" : OverlayConfig.DESTROY_PHRASE.trim();

            if (actual.equals(expected)) {
                Toast.makeText(getApplicationContext(), "Phrase matched! Destroying...", Toast.LENGTH_SHORT).show();
                destroyOverlay();
            } else {
                Toast.makeText(
                    getApplicationContext(),
                    "No match. Expected: [" + expected + "] Got: [" + actual + "]",
                    Toast.LENGTH_LONG
                ).show();
            }
        } catch (Exception e) {
            Toast.makeText(getApplicationContext(), "Check err: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void destroyOverlay() {
        try {
            if (wm != null && overlayView != null) {
                try {
                    if (overlayView.getWindowToken() != null || overlayView.getParent() != null) {
                        wm.removeViewImmediate(overlayView);
                    }
                } catch (Exception inner) {
                    wm.removeView(overlayView);
                }
                overlayView = null;
            }
            if (wakeLock != null && wakeLock.isHeld()) {
                wakeLock.release();
                wakeLock = null;
            }
            finish();
        } catch (Exception e) {
            Toast.makeText(getApplicationContext(), "Destroy err: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if (wm != null && overlayView != null) {
                try {
                    wm.removeViewImmediate(overlayView);
                } catch (Exception ignored) {
                    wm.removeView(overlayView);
                }
                overlayView = null;
            }
            if (wakeLock != null && wakeLock.isHeld()) {
                wakeLock.release();
                wakeLock = null;
            }
        } catch (Exception ignored) {
        }
    }
}
