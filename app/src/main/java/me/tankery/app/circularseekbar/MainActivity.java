package me.tankery.app.circularseekbar;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.util.Log;
import android.widget.TextView;

import me.tankery.lib.circularseekbar.CircularSeekBar;


public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final TextView textEvent = findTheViewById(R.id.text_event);
        final TextView textProgress = findTheViewById(R.id.text_progress);

        CircularSeekBar seekBar = (CircularSeekBar) findViewById(R.id.seek_bar);
        seekBar.setOnSeekBarChangeListener(new CircularSeekBar.OnCircularSeekBarChangeListener() {
            @Override
            public void onProgressChanged(CircularSeekBar circularSeekBar, float progress, boolean fromUser) {
                String message = String.format("Progress changed to %.2f, fromUser %s", progress, fromUser);
                Log.d("Main", message);
                textProgress.setText(message);
            }

            @Override
            public void onStopTrackingTouch(CircularSeekBar seekBar) {
                Log.d("Main", "onStopTrackingTouch");
                textEvent.setText("");
            }

            @Override
            public void onStartTrackingTouch(CircularSeekBar seekBar) {
                Log.d("Main", "onStartTrackingTouch");
                textEvent.setText("touched | ");
            }
        });
    }

    @SuppressWarnings("unchecked")
    private <T> T findTheViewById(@IdRes int id) {
        return (T) super.findViewById(id);
    }

}
