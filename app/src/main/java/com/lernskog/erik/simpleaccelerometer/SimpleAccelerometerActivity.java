package com.lernskog.erik.simpleaccelerometer;

import android.app.Activity;
import android.os.Bundle;

public class SimpleAccelerometerActivity extends Activity {
    private BounceView the_view;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_simple_accelerometer);
        the_view = (BounceView)findViewById(R.id.bounceview);
    }
    @Override
    protected void onResume() {
        super.onResume();
        // App is now visible (perhaps: again). Tell the view to start moving.
        the_view.resume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // App has been hidden. Tell the view to stop moving.
        the_view.pause();
    }
}
