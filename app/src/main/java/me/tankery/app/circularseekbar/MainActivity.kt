package me.tankery.app.circularseekbar

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import me.tankery.app.circularseekbar.R.id
import me.tankery.app.circularseekbar.R.layout
import me.tankery.lib.circularseekbar.CircularSeekBar
import me.tankery.lib.circularseekbar.CircularSeekBar.OnCircularSeekBarChangeListener

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(layout.activity_main)

        val textEvent = findViewById<TextView>(id.text_event)
        val textProgress = findViewById<TextView>(id.text_progress)
        val seekBar = findViewById<CircularSeekBar>(id.seek_bar)

        seekBar.setOnSeekBarChangeListener(object : OnCircularSeekBarChangeListener {
            override fun onProgressChanged(circularSeekBar: CircularSeekBar?, progress: Float, fromUser: Boolean) {
                val message = String.format("Progress changed to %.2f, fromUser %s", progress, fromUser)
                Log.d("Main", message)
                textProgress.text = message
            }

            override fun onStopTrackingTouch(seekBar: CircularSeekBar?) {
                Log.d("Main", "onStopTrackingTouch")
                textEvent.text = ""
            }

            override fun onStartTrackingTouch(seekBar: CircularSeekBar?) {
                Log.d("Main", "onStartTrackingTouch")
                textEvent.text = "touched | "
            }
        })
    }
}