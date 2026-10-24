package com.pavan.wallpaperlock;

import android.app.Activity;
import android.app.WallpaperManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.InputStream;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final int PICK_IMAGE = 41;
    private static final String PREFS = "wallpaper_lock";
    private static final String END_AT = "end_at";
    private static final String IMAGE_URI = "image_uri";
    private static final String STARTED = "started";

    private SharedPreferences prefs;
    private TextView status, timer, preview;
    private Button chooseButton, oneHour, oneDay, sevenDays, startButton;
    private Uri selectedUri;
    private long selectedDurationMs = 24L * 60 * 60 * 1000;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private final Runnable tick = new Runnable() {
        @Override public void run() {
            updateLockedUi();
            if (prefs.getBoolean(STARTED, false) && System.currentTimeMillis() < prefs.getLong(END_AT, 0)) {
                handler.postDelayed(this, 1000);
            }
        }
    };

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        String saved = prefs.getString(IMAGE_URI, null);
        if (saved != null) selectedUri = Uri.parse(saved);
        buildUi();
        updateLockedUi();
        handler.post(tick);
    }

    private int dp(float n) {
        return (int) (n * getResources().getDisplayMetrics().density + 0.5f);
    }

    private TextView text(String value, int size) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(0xFFF3F4F6);
        t.setGravity(Gravity.CENTER);
        t.setPadding(dp(8), dp(8), dp(8), dp(8));
        return t;
    }

    private Button button(String label) {
        Button b = new Button(this);
        b.setText(label);
        b.setAllCaps(false);
        return b;
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(dp(22), dp(24), dp(22), dp(24));
        root.setBackgroundColor(0xFF0B0D12);

        TextView title = text("WALLPAPER LOCK", 25);
        title.setLetterSpacing(0.04f);
        root.addView(title, new LinearLayout.LayoutParams(-1, -2));

        TextView subtitle = text("Choose once. Stay committed.", 14);
        subtitle.setTextColor(0xFF9CA3AF);
        root.addView(subtitle);

        preview = text("No wallpaper selected", 17);
        preview.setBackgroundColor(0xFF171B25);
        LinearLayout.LayoutParams pp = new LinearLayout.LayoutParams(-1, dp(120));
        pp.setMargins(0, dp(18), 0, dp(14));
        root.addView(preview, pp);

        chooseButton = button("Choose wallpaper");
        root.addView(chooseButton, fullWidth());
        chooseButton.setOnClickListener(v -> {
            if (isLocked()) return;
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("image/*");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
            startActivityForResult(intent, PICK_IMAGE);
        });

        TextView durationLabel = text("LOCK DURATION", 14);
        durationLabel.setTextColor(0xFFA5B4FC);
        LinearLayout.LayoutParams dl = new LinearLayout.LayoutParams(-1, -2);
        dl.setMargins(0, dp(18), 0, dp(4));
        root.addView(durationLabel, dl);

        LinearLayout choices = new LinearLayout(this);
        choices.setOrientation(LinearLayout.HORIZONTAL);
        oneHour = button("1 hour");
        oneDay = button("24 hours");
        sevenDays = button("7 days");
        choices.addView(oneHour, new LinearLayout.LayoutParams(0, -2, 1));
        choices.addView(oneDay, new LinearLayout.LayoutParams(0, -2, 1));
        choices.addView(sevenDays, new LinearLayout.LayoutParams(0, -2, 1));
        root.addView(choices, fullWidth());
        oneHour.setOnClickListener(v -> setDuration(60L * 60 * 1000));
        oneDay.setOnClickListener(v -> setDuration(24L * 60 * 60 * 1000));
        sevenDays.setOnClickListener(v -> setDuration(7L * 24 * 60 * 60 * 1000));

        startButton = button("Apply wallpaper & start lock");
        LinearLayout.LayoutParams sp = fullWidth();
        sp.setMargins(0, dp(18), 0, dp(8));
        root.addView(startButton, sp);
        startButton.setOnClickListener(v -> startLock());

        status = text("", 16);
        status.setTextColor(0xFF86EFAC);
        root.addView(status, fullWidth());

        timer = text("", 32);
        timer.setTypeface(android.graphics.Typeface.MONOSPACE);
        root.addView(timer, fullWidth());

        TextView note = text("The lock prevents changes inside this app. Android may still allow wallpaper changes in system settings or other apps.", 12);
        note.setTextColor(0xFF9CA3AF);
        LinearLayout.LayoutParams np = new LinearLayout.LayoutParams(-1, -2);
        np.setMargins(0, dp(14), 0, 0);
        root.addView(note, np);

        setContentView(root);
        setDuration(selectedDurationMs);
        if (selectedUri != null) preview.setText("Wallpaper selected ✓");
    }

    private LinearLayout.LayoutParams fullWidth() {
        return new LinearLayout.LayoutParams(-1, -2);
    }

    private void setDuration(long duration) {
        if (isLocked()) return;
        selectedDurationMs = duration;
        oneHour.setAlpha(duration == 3600000L ? 1f : 0.65f);
        oneDay.setAlpha(duration == 86400000L ? 1f : 0.65f);
        sevenDays.setAlpha(duration == 604800000L ? 1f : 0.65f);
    }

    private boolean isLocked() {
        return prefs.getBoolean(STARTED, false) && System.currentTimeMillis() < prefs.getLong(END_AT, 0);
    }

    private void startLock() {
        if (isLocked()) return;
        if (selectedUri == null) {
            Toast.makeText(this, "Choose a wallpaper first.", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            getContentResolver().takePersistableUriPermission(selectedUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } catch (Exception ignored) {}
        try (InputStream in = getContentResolver().openInputStream(selectedUri)) {
            Bitmap bitmap = BitmapFactory.decodeStream(in);
            if (bitmap == null) throw new Exception("Could not read image");
            WallpaperManager wm = WallpaperManager.getInstance(this);
            wm.setBitmap(bitmap, null, true, WallpaperManager.FLAG_SYSTEM);
            // Apply to lock screen as well where supported.
            try { wm.setBitmap(bitmap, null, true, WallpaperManager.FLAG_LOCK); } catch (Exception ignored) {}
            prefs.edit()
                .putString(IMAGE_URI, selectedUri.toString())
                .putLong(END_AT, System.currentTimeMillis() + selectedDurationMs)
                .putBoolean(STARTED, true)
                .apply();
            updateLockedUi();
            handler.removeCallbacks(tick);
            handler.post(tick);
            Toast.makeText(this, "Wallpaper applied. Lock timer started.", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "Couldn't apply wallpaper. Please try another image.", Toast.LENGTH_LONG).show();
        }
    }

    private void updateLockedUi() {
        boolean locked = isLocked();
        long remaining = Math.max(0, prefs.getLong(END_AT, 0) - System.currentTimeMillis());
        chooseButton.setEnabled(!locked);
        oneHour.setEnabled(!locked);
        oneDay.setEnabled(!locked);
        sevenDays.setEnabled(!locked);
        startButton.setEnabled(!locked);
        chooseButton.setAlpha(locked ? 0.35f : 1f);
        startButton.setAlpha(locked ? 0.35f : 1f);

        if (locked) {
            status.setText("🔒 LOCKED · No in-app unlock");
            timer.setText(format(remaining));
            startButton.setText("LOCK ACTIVE");
            chooseButton.setText("Wallpaper locked");
        } else if (prefs.getBoolean(STARTED, false)) {
            status.setText("Timer complete · You can choose again");
            timer.setText("00:00:00");
            startButton.setText("Apply wallpaper & start lock");
            chooseButton.setText("Choose wallpaper");
        } else {
            status.setText("Ready when you are");
            timer.setText("—");
        }
    }

    private String format(long ms) {
        long total = Math.max(0, ms / 1000);
        long days = total / 86400;
        long hours = (total % 86400) / 3600;
        long mins = (total % 3600) / 60;
        long secs = total % 60;
        if (days > 0) return String.format(Locale.US, "%dd %02d:%02d:%02d", days, hours, mins, secs);
        return String.format(Locale.US, "%02d:%02d:%02d", hours, mins, secs);
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE && resultCode == RESULT_OK && data != null && data.getData() != null && !isLocked()) {
            selectedUri = data.getData();
            preview.setText("Wallpaper selected ✓");
            try {
                getContentResolver().takePersistableUriPermission(selectedUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                prefs.edit().putString(IMAGE_URI, selectedUri.toString()).apply();
            } catch (Exception ignored) {}
        }
    }

    @Override protected void onDestroy() {
        handler.removeCallbacks(tick);
        super.onDestroy();
    }
}
