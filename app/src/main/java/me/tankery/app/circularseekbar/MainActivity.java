package me.tankery.app.circularseekbar;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

import me.tankery.lib.circularseekbar.CircularSeekBar;


public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        CircularSeekBar seekBar = (CircularSeekBar) findViewById(R.id.seek_bar);
        seekBar.setOnSeekBarChangeListener(new CircularSeekBar.OnCircularSeekBarChangeListener() {
            @Override
            public void onProgressChanged(CircularSeekBar circularSeekBar, float progress, boolean fromUser) {
                Log.d("Main", String.format("Progress changed to %.2f, fromUser %s", progress, fromUser));
            }

            @Override
            public void onStopTrackingTouch(CircularSeekBar seekBar) {
                Log.d("Main", "onStopTrackingTouch");
            }

            @Override
            public void onStartTrackingTouch(CircularSeekBar seekBar) {
                Log.d("Main", "onStartTrackingTouch");
            }
        });
    }

}
