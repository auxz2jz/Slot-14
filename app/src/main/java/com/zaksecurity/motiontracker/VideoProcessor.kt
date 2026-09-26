package com.zaksecurity.motiontracker

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import java.util.concurrent.atomic.AtomicBoolean

internal class VideoProcessor(private val activity:MainActivity,private val engine:MotionTrackingEngine,private val stop:AtomicBoolean){
    fun run(uri:Uri){
        val retriever=MediaMetadataRetriever()
        try{
            retriever.setDataSource(activity,uri)
            val durationMs=retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()?:0L
            val durationUs=durationMs*1000L;val stepUs=200_000L;var timeUs=0L;val started=System.nanoTime()
            while(!stop.get() && (durationUs<=0L || timeUs<durationUs)){
                val bitmap=retriever.getFrameAtTime(timeUs,MediaMetadataRetriever.OPTION_CLOSEST)
                if(bitmap==null){timeUs+=stepUs;continue}
                val result=engine.process(bitmap);bitmap.recycle();activity.onTrackingFrame(result,timeUs,durationUs)
                timeUs+=stepUs
            }
            activity.onTrackingFinished(stop.get(),(System.nanoTime()-started)/1_000_000L)
        }catch(t:Throwable){activity.onTrackingError(t)}finally{retriever.release();activity.onProcessorStopped()}
    }
}
