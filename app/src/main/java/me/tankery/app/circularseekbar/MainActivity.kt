package me.tankery.app.circularseekbar

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import me.tankery.app.circularseekbar.R.id
import me.tankery.app.circularseekbar.R.layout
import me.tankery.lib.circularseekbar.CircularSeekBarV2

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(layout.activity_main)

        val textEvent = findViewById<TextView>(id.text_event)
        val textProgress = findViewById<TextView>(id.text_progress)
        val seekBar = findViewById<CircularSeekBarV2>(id.seek_bar)

        seekBar.setOnSeekBarChangeListener(object :
            CircularSeekBarV2.OnCircularSeekBarChangeListener {
            override fun onProgressChanged(circularSeekBar: CircularSeekBarV2?, progress: Float, fromUser: Boolean) {
                val message = String.format("Progress changed to %.2f, fromUser %s", progress, fromUser)
                Log.d("Main", message)
                textProgress.text = message
            }

            override fun onStopTrackingTouch(seekBar: CircularSeekBarV2?) {
                Log.d("Main", "onStopTrackingTouch")
                textEvent.text = ""
            }

            @SuppressLint("SetTextI18n")
            override fun onStartTrackingTouch(seekBar: CircularSeekBarV2?) {
                Log.d("Main", "onStartTrackingTouch")
                textEvent.text = "touched | "
            }
        })
    }
}