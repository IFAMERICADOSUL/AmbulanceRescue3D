
package com.ambulancerescue.game;

import android.app.Activity;
import android.os.Bundle;
import android.opengl.GLSurfaceView;
import android.os.Handler;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.content.SharedPreferences;

public class MainActivity extends Activity {

    private GameRenderer renderer;
    private ToneGenerator tone;
    private Handler handler = new Handler();
    private boolean siren = false;
    private boolean sirenHigh = false;
    private TextView info;
    private SharedPreferences prefs;

    private final Runnable sirenLoop = new Runnable() {
        @Override
        public void run() {
            if (siren && tone != null) {
                tone.startTone(
                    sirenHigh
                        ? ToneGenerator.TONE_PROP_BEEP
                        : ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD,
                    450
                );
                sirenHigh = !sirenHigh;
                handler.postDelayed(this, 480);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_FULLSCREEN
            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        );

        prefs = getSharedPreferences("ambulance3d", MODE_PRIVATE);

        tone = new ToneGenerator(AudioManager.STREAM_MUSIC, 75);

        FrameLayout root = new FrameLayout(this);

        GLSurfaceView surface = new GLSurfaceView(this);
        surface.setEGLContextClientVersion(2);

        renderer = new GameRenderer(this);
        surface.setRenderer(renderer);
        surface.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);

        root.addView(surface);

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setPadding(12, 8, 12, 8);
        top.setBackgroundColor(0x990D1720);

        info = new TextView(this);
        info.setTextColor(Color.WHITE);
        info.setTextSize(15);
        info.setTypeface(Typeface.DEFAULT_BOLD);
        info.setText("AMBULANCE RESCUE 3D  •  Pronto");
        top.addView(info, new LinearLayout.LayoutParams(
            0, -2, 1
        ));

        Button camera = makeButton("VISUALE");
        camera.setOnClickListener(v -> {
            renderer.nextCamera();
            info.setText("Visuale: " + renderer.cameraName());
        });
        top.addView(camera);

        Button sirenButton = makeButton("SIRENA");
        sirenButton.setOnClickListener(v -> toggleSiren());
        top.addView(sirenButton);

        Button vehicle = makeButton("MEZZO");
        vehicle.setOnClickListener(v -> {
            renderer.nextVehicle();
            info.setText("Ambulanza: " + renderer.vehicleName());
        });
        top.addView(vehicle);

        FrameLayout.LayoutParams topParams =
            new FrameLayout.LayoutParams(
                -1, dp(55), Gravity.TOP
            );
        root.addView(top, topParams);

        LinearLayout controls = new LinearLayout(this);
        controls.setGravity(Gravity.CENTER_VERTICAL);
        controls.setPadding(dp(12), dp(5), dp(12), dp(10));
        controls.setOrientation(LinearLayout.HORIZONTAL);
        controls.setBackgroundColor(0x66000000);

        Button left = makeButton("◀");
        Button right = makeButton("▶");
        Button brake = makeButton("FRENO");
        Button gas = makeButton("ACCELERA");
        Button mission = makeButton("SOCCORSO");

        addHoldControl(controls, left, 0);
        addHoldControl(controls, right, 1);

        View spacer = new View(this);
        controls.addView(spacer, new LinearLayout.LayoutParams(0, 1, 1));

        addHoldControl(controls, brake, 2);
        addHoldControl(controls, gas, 3);

        mission.setOnClickListener(v -> {
            renderer.tryMission();
            info.setText(renderer.getStatus());
        });
        controls.addView(mission);

        FrameLayout.LayoutParams bottomParams =
            new FrameLayout.LayoutParams(
                -1, dp(76), Gravity.BOTTOM
            );
        root.addView(controls, bottomParams);

        setContentView(root);
    }

    private Button makeButton(String text) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextColor(Color.WHITE);
        b.setTextSize(12);
        b.setAllCaps(false);
        b.setTypeface(Typeface.DEFAULT_BOLD);
        b.setBackgroundTintList(
            android.content.res.ColorStateList.valueOf(0xFF24566A)
        );
        return b;
    }

    private void addHoldControl(
        LinearLayout row, Button button, int control
    ) {
        LinearLayout.LayoutParams p =
            new LinearLayout.LayoutParams(dp(75), dp(58));
        p.setMargins(dp(4), 0, dp(4), 0);
        row.addView(button, p);

        button.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                renderer.setControl(control, true);
                return true;
            }

            if (event.getAction() == MotionEvent.ACTION_UP
                || event.getAction() == MotionEvent.ACTION_CANCEL) {
                renderer.setControl(control, false);
                return true;
            }

            return true;
        });
    }

    private int dp(float value) {
        return (int)(value * getResources()
            .getDisplayMetrics().density + 0.5f);
    }

    private void toggleSiren() {
        siren = !siren;
        renderer.setSiren(siren);

        if (siren) {
            handler.removeCallbacks(sirenLoop);
            handler.post(sirenLoop);
            info.setText("Sirena attivata");
        } else {
            handler.removeCallbacks(sirenLoop);
            if (tone != null) tone.stopTone();
            info.setText("Sirena disattivata");
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (renderer != null) renderer.save();
        if (tone != null) tone.stopTone();
        handler.removeCallbacks(sirenLoop);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(sirenLoop);
        if (tone != null) {
            tone.stopTone();
            tone.release();
            tone = null;
        }
    }
}
