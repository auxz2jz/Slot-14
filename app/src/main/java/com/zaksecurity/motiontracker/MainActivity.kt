package com.zaksecurity.motiontracker

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import org.opencv.android.OpenCVLoader
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.max

class MainActivity:Activity(){
    companion object{const val VERSION="0.1.0";const val REQUEST_OPEN_VIDEO=1001;const val REQUEST_EXPORT_DIAGNOSTICS=1002;const val PREFS="motion_tracker_prefs"}
    internal lateinit var logger:DiagnosticLogger;private lateinit var engine:MotionTrackingEngine;private lateinit var ui:TrackerUi
    private val worker=Executors.newSingleThreadExecutor();private val main=Handler(Looper.getMainLooper());private val stop=AtomicBoolean(false)
    private var selectedVideo:Uri?=null;private var running=false;@Volatile private var showMask=false;private var lastPreview:Bitmap?=null
    internal var processedFrames=0;internal var maxTrackCount=0;internal var maxChangedPixels=0;internal var lastError:String?=null

    override fun onCreate(state:Bundle?){super.onCreate(state);logger=DiagnosticLogger(this);logger.log("LIFECYCLE","APP_CREATED",details=mapOf("version" to VERSION))
        val cv=OpenCVLoader.initLocal();logger.log("PRECONDITION","OPENCV_LOAD",result=if(cv)"SUCCESS" else "FAIL");if(!cv)Toast.makeText(this,"OpenCV failed to load.",Toast.LENGTH_LONG).show()
        engine=MotionTrackingEngine();ui=TrackerUi(this);setContentView(ui.build(engine));selectedVideo=getSharedPreferences(PREFS,MODE_PRIVATE).getString("selectedVideo",null)?.let{runCatching{Uri.parse(it)}.getOrNull()};status(if(selectedVideo==null)"Select a security-camera video." else "Video selected. Ready to track.")
    }

    internal fun selectVideo(){logger.log("USER_ACTION","SELECT_VIDEO_PRESSED");startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply{addCategory(Intent.CATEGORY_OPENABLE);type="video/*";addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)},REQUEST_OPEN_VIDEO)}
    internal fun setMaskDisplay(v:Boolean){showMask=v;logger.log("USER_ACTION","MASK_DISPLAY_CHANGED",details=mapOf("enabled" to v))}
    internal fun logSetting(name:String,value:Int)=logger.log("USER_ACTION","SETTING_CHANGED",details=mapOf("name" to name,"value" to value))
    internal fun stopTracking(){logger.log("USER_ACTION","STOP_TRACKING_PRESSED");stop.set(true)}

    internal fun startTracking(){if(running)return;val uri=selectedVideo?:run{Toast.makeText(this,"Select a video first.",Toast.LENGTH_SHORT).show();return}
        logger.log("USER_ACTION","START_TRACKING_PRESSED");logger.log("OPERATION_START","TRACKING_RUN_STARTED",details=mapOf("pixelThreshold" to engine.pixelThreshold,"minArea" to engine.minArea,"analysisWidth" to engine.analysisWidth))
        running=true;stop.set(false);ui.start.isEnabled=false;ui.stop.isEnabled=true;processedFrames=0;maxTrackCount=0;maxChangedPixels=0;lastError=null;ui.progress.progress=0;engine.reset();worker.execute{VideoProcessor(this,engine,stop).run(uri)}
    }

    internal fun onTrackingFrame(r:MotionTrackingEngine.Result,timeUs:Long,durationUs:Long){processedFrames++;maxTrackCount=max(maxTrackCount,r.activeTracks);maxChangedPixels=max(maxChangedPixels,r.changedPixels)
        if(processedFrames%25==0)logger.log("PROGRESS","TRACKING_PROGRESS",details=mapOf("frame" to r.frameNumber,"mediaTimeUs" to timeUs,"changedPixels" to r.changedPixels,"tracks" to r.activeTracks,"predicted" to r.predictedTracks))
        val shown=if(showMask)r.motionMask else r.annotated;val unused=if(showMask)r.annotated else r.motionMask
        main.post{val old=lastPreview;lastPreview=shown;ui.preview.setImageBitmap(shown);old?.let{if(it!==shown&&!it.isRecycled)it.recycle()};if(!unused.isRecycled)unused.recycle();val pct=if(durationUs>0)(timeUs.toDouble()/durationUs).coerceIn(0.0,1.0) else 0.0;ui.progress.progress=(pct*1000).toInt();status("Frame ${r.frameNumber} • ${"%.1f".format(timeUs/1_000_000.0)} s • changed ${r.changedPixels} • tracks ${r.activeTracks} (${r.predictedTracks} predicted)")}
    }
    internal fun onTrackingFinished(cancelled:Boolean,elapsedMs:Long){logger.log("OPERATION_RESULT","TRACKING_RUN_FINISHED",result=if(cancelled)"CANCELLED" else "SUCCESS",details=mapOf("processedFrames" to processedFrames,"maxTracks" to maxTrackCount,"maxChangedPixels" to maxChangedPixels,"durationMs" to elapsedMs));main.post{if(!cancelled)ui.progress.progress=1000;status(if(cancelled)"Tracking stopped. Processed $processedFrames frames." else "Tracking complete. Processed $processedFrames frames.")}}
    internal fun onTrackingError(t:Throwable){lastError="${t.javaClass.simpleName}: ${t.message}";logger.log("ERROR","TRACKING_RUN_FAILED",result="FAIL",details=mapOf("errorType" to t.javaClass.name,"errorMessage" to (t.message?:"")));main.post{status("Tracking failed: ${lastError?:"unknown"}");Toast.makeText(this,"Tracking failed. Export diagnostics.",Toast.LENGTH_LONG).show()}}
    internal fun onProcessorStopped(){running=false;main.post{ui.start.isEnabled=true;ui.stop.isEnabled=false}}
    internal fun showGuidedTest()=GuidedTest.show(this);internal fun chooseDiagnosticsDestination()=DiagnosticExporter.choose(this);private fun status(s:String){ui.status.text=s}

    @Suppress("DEPRECATION") override fun onActivityResult(code:Int,result:Int,data:Intent?){super.onActivityResult(code,result,data);if(result!=RESULT_OK)return;when(code){REQUEST_OPEN_VIDEO->{val uri=data?.data?:return;val flags=data.flags and (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION);runCatching{contentResolver.takePersistableUriPermission(uri,flags)};selectedVideo=uri;getSharedPreferences(PREFS,MODE_PRIVATE).edit().putString("selectedVideo",uri.toString()).apply();logger.log("OPERATION_RESULT","VIDEO_SELECTED",result="SUCCESS",details=mapOf("uriScheme" to (uri.scheme?:"unknown")));status("Video selected. Ready to track.")}REQUEST_EXPORT_DIAGNOSTICS->data?.data?.let{DiagnosticExporter.export(this,it)}}}
    override fun onDestroy(){stop.set(true);worker.shutdownNow();lastPreview?.let{if(!it.isRecycled)it.recycle()};logger.log("LIFECYCLE","APP_DESTROYED");super.onDestroy()}
}
