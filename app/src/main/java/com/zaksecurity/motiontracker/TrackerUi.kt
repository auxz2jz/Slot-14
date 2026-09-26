package com.zaksecurity.motiontracker

import android.view.Gravity
import android.view.View
import android.widget.*

internal class TrackerUi(private val activity: MainActivity) {
    lateinit var preview:ImageView; lateinit var status:TextView; lateinit var progress:ProgressBar
    lateinit var start:Button; lateinit var stop:Button; lateinit var mask:CheckBox

    fun build(engine:MotionTrackingEngine):View {
        val d=activity.resources.displayMetrics.density
        val root=LinearLayout(activity).apply{orientation=LinearLayout.VERTICAL; val p=(12*d).toInt(); setPadding(p,p,p,(24*d).toInt())}
        root.addView(TextView(activity).apply{text="Security Motion Tracker v${MainActivity.VERSION}";textSize=22f})
        root.addView(TextView(activity).apply{text="Red = changed pixels • Green = observed • Amber = predicted";textSize=14f})
        val actions=LinearLayout(activity).apply{orientation=LinearLayout.HORIZONTAL}
        fun addAction(text:String,click:()->Unit):Button=Button(activity).apply{this.text=text;setOnClickListener{click()};actions.addView(this,LinearLayout.LayoutParams(0,-2,1f))}
        addAction("Select Video"){activity.selectVideo()}; start=addAction("Start Tracking"){activity.startTracking()}; stop=addAction("Stop"){activity.stopTracking()}.apply{isEnabled=false}
        root.addView(actions)
        mask=CheckBox(activity).apply{text="Show black/white motion mask";setOnCheckedChangeListener{_,v->activity.setMaskDisplay(v)}};root.addView(mask)
        val thresholdLabel=TextView(activity);root.addView(thresholdLabel)
        root.addView(SeekBar(activity).apply{max=55;progress=15;setOnSeekBarChangeListener(listener{v,from->engine.pixelThreshold=(v+5).toDouble();thresholdLabel.text="Pixel-change threshold: ${v+5} (lower = more sensitive)";if(from)activity.logSetting("pixelThreshold",v+5)})})
        thresholdLabel.text="Pixel-change threshold: 20 (lower = more sensitive)"
        val areaLabel=TextView(activity);root.addView(areaLabel)
        root.addView(SeekBar(activity).apply{max=490;progress=40;setOnSeekBarChangeListener(listener{v,from->engine.minArea=(v+50).toDouble();areaLabel.text="Minimum tracked motion area: ${v+50} pixels";if(from)activity.logSetting("minArea",v+50)})})
        areaLabel.text="Minimum tracked motion area: 90 pixels"
        progress=ProgressBar(activity,null,android.R.attr.progressBarStyleHorizontal).apply{max=1000};root.addView(progress,LinearLayout.LayoutParams(-1,-2))
        status=TextView(activity).apply{textSize=15f;val p=(8*d).toInt();setPadding(p,p,p,p)};root.addView(status)
        preview=ImageView(activity).apply{adjustViewBounds=true;scaleType=ImageView.ScaleType.FIT_CENTER;minimumHeight=(300*d).toInt();setBackgroundColor(0xFF101010.toInt())};root.addView(preview,LinearLayout.LayoutParams(-1,-2))
        val bottom=LinearLayout(activity).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER}
        fun bottomButton(text:String,click:()->Unit)=Button(activity).apply{this.text=text;setOnClickListener{click()};bottom.addView(this,LinearLayout.LayoutParams(0,-2,1f))}
        bottomButton("Test This Version"){activity.showGuidedTest()};bottomButton("Export Diagnostics"){activity.chooseDiagnosticsDestination()};root.addView(bottom)
        return ScrollView(activity).apply{addView(root)}
    }

    private fun listener(block:(Int,Boolean)->Unit)=object:SeekBar.OnSeekBarChangeListener{
        override fun onProgressChanged(s:SeekBar?,p:Int,from:Boolean)=block(p,from)
        override fun onStartTrackingTouch(s:SeekBar?){};override fun onStopTrackingTouch(s:SeekBar?){}
    }
}
