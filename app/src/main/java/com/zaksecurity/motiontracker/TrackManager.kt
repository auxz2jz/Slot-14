package com.zaksecurity.motiontracker

import org.opencv.core.Core
import org.opencv.core.Mat
import org.opencv.core.Point
import org.opencv.core.Rect
import org.opencv.core.Scalar
import org.opencv.imgproc.Imgproc
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

internal class TrackManager(
    var maxDistance: Double = 95.0,
    var maxMissedFrames: Int = 24,
) {
    private val tracks = linkedMapOf<Int, MotionTrack>()
    private var nextId = 1

    fun reset() {
        tracks.values.forEach { it.histogram.release() }
        tracks.clear(); nextId = 1
    }

    fun update(detections: List<MotionDetection>, width: Int, height: Int) {
        data class Candidate(val cost: Double, val id: Int, val index: Int)
        val candidates = mutableListOf<Candidate>()
        for ((id, track) in tracks) {
            val horizon = min(3.0, 1.0 + track.missed * 0.35)
            val predicted = Point(track.centroid.x + track.velocity.x*horizon, track.centroid.y + track.velocity.y*horizon)
            val gate = maxDistance * (1.0 + min(track.missed,8)*0.18)
            val oldArea = max(1.0, track.box.area())
            detections.forEachIndexed { index, d ->
                val distance = hypot(predicted.x-d.centroid.x, predicted.y-d.centroid.y)
                if (distance <= gate) {
                    val ratio = max(oldArea,max(1.0,d.area))/min(oldArea,max(1.0,d.area))
                    val sizeCost = min(1.0,max(0.0,(ratio-1.0)/3.0))
                    val appearance = similarity(track.histogram,d.histogram)
                    val cost = .58*min(1.0,distance/gate) + .28*(1.0-appearance) + .14*sizeCost
                    candidates += Candidate(cost,id,index)
                }
            }
        }
        candidates.sortBy { it.cost }
        val usedTracks=mutableSetOf<Int>(); val usedDetections=mutableSetOf<Int>()
        for (c in candidates) {
            if (c.cost>.82 || c.id in usedTracks || c.index in usedDetections) continue
            val t=tracks[c.id] ?: continue; val d=detections[c.index]
            val dx=d.centroid.x-t.centroid.x; val dy=d.centroid.y-t.centroid.y
            t.velocity=Point(.62*t.velocity.x+.38*dx,.62*t.velocity.y+.38*dy)
            t.centroid=d.centroid
            t.smoothCentroid=Point(.70*t.smoothCentroid.x+.30*d.centroid.x,.70*t.smoothCentroid.y+.30*d.centroid.y)
            t.box=d.box
            Core.addWeighted(t.histogram,.72,d.histogram,.28,0.0,t.histogram)
            Core.normalize(t.histogram,t.histogram,0.0,1.0,Core.NORM_MINMAX)
            t.missed=0; t.age++; t.hits++; t.predicted=false; t.quality=1.0-c.cost
            addPath(t,t.smoothCentroid); usedTracks += c.id; usedDetections += c.index
        }
        val stale=mutableListOf<Int>()
        for ((id,t) in tracks) if (id !in usedTracks) {
            t.missed++; t.age++
            if (t.missed>maxMissedFrames) { stale += id; continue }
            val dx=t.velocity.x; val dy=t.velocity.y
            t.box=translate(t.box,dx,dy,width,height)
            t.centroid=Point(t.centroid.x+dx,t.centroid.y+dy)
            t.smoothCentroid=Point(.75*t.smoothCentroid.x+.25*t.centroid.x,.75*t.smoothCentroid.y+.25*t.centroid.y)
            t.velocity=Point(dx*.82,dy*.82); t.predicted=true; t.quality*=.88
            if (t.missed<=6) addPath(t,t.smoothCentroid)
        }
        stale.forEach { tracks.remove(it)?.histogram?.release() }
        detections.forEachIndexed { index,d -> if (index !in usedDetections) {
            val id=nextId++; val t=MotionTrack(id,d.box,d.centroid,d.centroid,d.histogram.clone())
            addPath(t,d.centroid); tracks[id]=t
        }}
    }

    private fun addPath(t: MotionTrack, p: Point) { t.path.addLast(p); while(t.path.size>400) t.path.removeFirst() }
    private fun similarity(a: Mat,b: Mat)=((Imgproc.compareHist(a,b,Imgproc.HISTCMP_CORREL)+1.0)*.5).coerceIn(0.0,1.0)
    private fun translate(r: Rect,dx:Double,dy:Double,w:Int,h:Int):Rect {
        val x=(r.x+dx).toInt().coerceIn(0,max(0,w-1)); val y=(r.y+dy).toInt().coerceIn(0,max(0,h-1))
        return Rect(x,y,r.width.coerceIn(1,max(1,w-x)),r.height.coerceIn(1,max(1,h-y)))
    }

    fun draw(rgba: Mat) {
        for (t in tracks.values) {
            val color=if(t.predicted) Scalar(255.0,170.0,0.0,255.0) else Scalar(0.0,220.0,0.0,255.0)
            Imgproc.rectangle(rgba,t.box.tl(),t.box.br(),color,2)
            val pts=t.path.toList(); for(i in 1 until pts.size) Imgproc.line(rgba,pts[i-1],pts[i],color,2,Imgproc.LINE_AA)
            val state=if(t.predicted) "PRED ${t.missed}" else "TRACK"
            Imgproc.putText(rgba,"Motion #${t.id} $state",Point(t.box.x.toDouble(),max(16,t.box.y-6).toDouble()),Imgproc.FONT_HERSHEY_SIMPLEX,.45,color,1,Imgproc.LINE_AA)
        }
    }

    val activeCount:Int get()=tracks.size
    val predictedCount:Int get()=tracks.values.count{it.predicted}
}
