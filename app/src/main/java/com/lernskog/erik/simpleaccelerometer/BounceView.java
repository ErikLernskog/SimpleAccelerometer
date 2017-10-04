package com.lernskog.erik.simpleaccelerometer;

import java.util.Timer;
import java.util.TimerTask;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;

public class BounceView extends View {
    // Ball position, measured in SCREENS. Starts in the middle.
    private float ball_x = 0.5f;
    private float ball_y = 0.5f;
    // Ball movement, measured in SCREENS PER SECOND
    private float ball_dx = 0.1f;
    private float ball_dy = 0.05f;
    // Measured in pixels
    private int ball_radius = 10;

    private int width_pixels;
    private int height_pixels;

    private Timer the_ticker;
    private SensorManager sensor_manager;
    private Sensor accelerometer;
    private SensorEventListener accelerometer_listener;
    private WindowManager window_manager;
    private Display display;

    private float acceleration_x = 0;
    private float acceleration_y = 0;
    private float acceleration_z = 0;

    private void init(Context context) {
        // We need the SensorManager to access the accelerometer
        sensor_manager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensor_manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        // We need the WindowManager to get the Display, and the Display to find the rotation
        window_manager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        display = window_manager.getDefaultDisplay();
        accelerometer_listener = new SensorEventListener() {
            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
                // Who cares? Do nothing.
            }

            @Override
            public void onSensorChanged(SensorEvent event) {
                if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                    // acceleration_x = event.values[0];
                    // acceleration_y = event.values[1];
                    acceleration_z = event.values[2];

                    switch (display.getOrientation()) {

                        case Surface.ROTATION_0:
                            acceleration_x = event.values[0];
                            acceleration_y = event.values[1];
                            break;
                        case Surface.ROTATION_90:
                            acceleration_x = -event.values[1];
                            acceleration_y = event.values[0];
                            break;
                        case Surface.ROTATION_180:
                            acceleration_x = -event.values[0];
                            acceleration_y = -event.values[1];
                            break;
                        case Surface.ROTATION_270:
                            acceleration_x = event.values[1];
                            acceleration_y = -event.values[0];
                            break;
                    }
                }
            }
        };
    }

    public BounceView(Context context) {
        super(context);
        init(context);
    }

    public BounceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        width_pixels = w;
        height_pixels = h;
    }

    public static final double roundDouble(double d, int places) {
        return Math.round(d * Math.pow(10, (double) places)) / Math.pow(10, (double) places);
    }

    private int frames = 0;

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        Paint p = new Paint();
        int w = width_pixels > 0 ? width_pixels : canvas.getHeight();
        int h = height_pixels > 0 ? height_pixels : canvas.getWidth();

        // The ball is green when in the crosshairs
        if (Math.sqrt(Math.pow(ball_x * w - w / 2.0f, 2) + Math.pow(ball_y * h - h / 2.0f, 2)) < ball_radius)
            p.setColor(Color.GREEN);
        else
            p.setColor(Color.BLUE);
        canvas.drawCircle(ball_x * w, ball_y * h, ball_radius, p);

        // Show acceleration as a white ball
        float ax = 0.5f - 0.04f * acceleration_x;
        float ay = 0.5f + 0.04f * acceleration_y;
        p.setColor(Color.GREEN);
        canvas.drawCircle(ax * w, ay * h, ball_radius, p);
        canvas.drawLine(w * 0.5f, h * 0.5f, w * ax, h * ay, p);

        p.setColor(Color.RED);
        canvas.drawText("Ball: x = " + String.format("%.2f", ball_x) +
                        ", y = " + String.format("%.2f", ball_y) +
                        ", dx = " + String.format("%.2f", ball_dx) +
                        ", dy = " + String.format("%.2f", ball_dy),
                20.0f, 20.0f, p);
        canvas.drawText("w = " + w + " (" + width_pixels + ")" +
                        ", h = " + h + " (" + height_pixels + ")",
                20.0f, 40.0f, p);
        canvas.drawText("Acc: X = " + String.format("%.2f", acceleration_x) +
                        ", Y = " + String.format("%.2f", acceleration_y) +
                        ", Z = " + String.format("%.2f", acceleration_z),
                20.0f, 60.0f, p);
        ++frames;
        canvas.drawText("frames = " + frames,
                20.0f, 80.0f, p);

        // A crosshair at the middle of the screen
        float crosswidth = Math.min(w, h) * 0.1f;
        canvas.drawLine(w * 0.5f - crosswidth, h * 0.5f, w * 0.5f + crosswidth, h * 0.5f, p);
        canvas.drawLine(w * 0.5f, h * 0.5f - crosswidth, w * 0.5f, h * 0.5f + crosswidth, p);

        canvas.drawText("basen.oru.se/android",
                20.0f, h - 5.0f, p);
    } // onDraw

    // Called when the app is visible(possibly: again). We should start moving.
    public void resume() {
        the_ticker = new Timer();
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                update_simulation();
                // We get CalledFromWrongThreadException if we just just call invalidate().
                // It must be done in the right thread!
                Runnable r = new Runnable() {
                    @Override
                    public void run() {
                        invalidate();
                    }
                };
                getHandler().post(r);
            }
        };
        // Give it a full second to set things up, before we start ticking
        // (It crashed with java.lang.NullPointerException when starting after 30 ms, but worked with 40.)
        the_ticker.schedule(task, 1000, 10);
        // Start listening to the accelerometer
        sensor_manager.registerListener(accelerometer_listener, accelerometer, SensorManager.SENSOR_DELAY_UI);
    } // resume

    // Called when the app has been hidden. We should stop moving.
    public void pause() {
        nanos_when_paused = java.lang.System.nanoTime();
        the_ticker.cancel();
        the_ticker = null;
        sensor_manager.unregisterListener(accelerometer_listener);
    }

    // -1 is not guaranteed to never happen, but we ignore that
    private long previous_nanos = -1;
    private long nanos_when_paused = -1;

    // Calculate the ball's new position and speed
    private void update_simulation() {
        long now_nanos = java.lang.System.nanoTime();
        if (previous_nanos == -1 || now_nanos < previous_nanos) {
            // First time, or overflow, so don't update the game
            previous_nanos = now_nanos;
            return;
        }

        long nanos = now_nanos - previous_nanos;
        if (nanos_when_paused != -1) {
            // We have been paused!
            nanos = nanos_when_paused - previous_nanos;
            nanos_when_paused = -1;
        }

        previous_nanos = now_nanos;
        double seconds = nanos / 1e9;

        ball_dx -= acceleration_x / 4000;
        ball_dy += acceleration_y / 4000;

        ball_x += ball_dx * seconds;
        ball_y += ball_dy * seconds;

        // Yes, this ignores that the ball has a radius.

        if (ball_x < 0) {
            Log.d("Bounce", "Bounce on left wall");
            ball_x = -ball_x;
            ball_dx = -ball_dx;
        }
        if (ball_x > 1) {
            Log.d("Bounce", "Bounce on right wall");
            ball_x = 2 - ball_x;
            ball_dx = -ball_dx;
        }
        if (ball_y < 0) {
            Log.d("Bounce", "Bounce on ceiling");
            ball_y = -ball_y;
            ball_dy = -ball_dy;
        }
        if (ball_y > 1) {
            Log.d("Bounce", "Bounce on floor");
            ball_y = 2 - ball_y;
            ball_dy = -ball_dy;
        }
    } // update_simulation
} // class BounceView