package com.zaksecurity.motiontracker

import android.graphics.Bitmap
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.Mat
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc

class MotionTrackingEngine(
    var analysisWidth: Int = 640,
    pixelThreshold: Double = 20.0,
    minArea: Double = 90.0,
) {
    data class Result(val annotated:Bitmap,val motionMask:Bitmap,val changedPixels:Int,val activeTracks:Int,val predictedTracks:Int,val frameNumber:Long)
    private val detector=MotionDetector(pixelThreshold,minArea)
    private val manager=TrackManager()
    private var frameNumber=0L

    var pixelThreshold:Double
        get()=detector.pixelThreshold
        set(value){detector.pixelThreshold=value}
    var minArea:Double
        get()=detector.minArea
        set(value){detector.minArea=value}

    fun reset(){detector.reset();manager.reset();frameNumber=0}

    fun process(input:Bitmap):Result {
        frameNumber++
        var rgba=Mat(); Utils.bitmapToMat(input,rgba)
        if(rgba.cols()>analysisWidth){
            val scale=analysisWidth.toDouble()/rgba.cols(); val resized=Mat()
            Imgproc.resize(rgba,resized,Size(analysisWidth.toDouble(),rgba.rows()*scale),0.0,0.0,Imgproc.INTER_AREA)
            rgba.release(); rgba=resized
        }
        val (mask,detections)=detector.analyze(rgba)
        manager.update(detections,rgba.cols(),rgba.rows()); detections.forEach{it.histogram.release()}
        val annotated=rgba.clone(); tintRed(annotated,mask); manager.draw(annotated)
        val annotatedBitmap=Bitmap.createBitmap(annotated.cols(),annotated.rows(),Bitmap.Config.ARGB_8888); Utils.matToBitmap(annotated,annotatedBitmap)
        val maskRgba=Mat(); Imgproc.cvtColor(mask,maskRgba,Imgproc.COLOR_GRAY2RGBA)
        val maskBitmap=Bitmap.createBitmap(maskRgba.cols(),maskRgba.rows(),Bitmap.Config.ARGB_8888); Utils.matToBitmap(maskRgba,maskBitmap)
        val changed=Core.countNonZero(mask)
        rgba.release(); mask.release(); annotated.release(); maskRgba.release()
        return Result(annotatedBitmap,maskBitmap,changed,manager.activeCount,manager.predictedCount,frameNumber)
    }

    private fun tintRed(rgba:Mat,mask:Mat){
        val red=Mat(rgba.size(),rgba.type(),Scalar(255.0,0.0,0.0,255.0)); val tint=rgba.clone()
        red.copyTo(tint,mask); Core.addWeighted(rgba,.66,tint,.34,0.0,rgba); red.release(); tint.release()
    }
}
