package com.zaksecurity.motiontracker

import org.opencv.core.Mat
import org.opencv.core.Point
import org.opencv.core.Rect

internal data class MotionDetection(
    val box: Rect,
    val centroid: Point,
    val area: Double,
    val histogram: Mat,
)

internal data class MotionTrack(
    val id: Int,
    var box: Rect,
    var centroid: Point,
    var smoothCentroid: Point,
    var histogram: Mat,
    var velocity: Point = Point(0.0, 0.0),
    var missed: Int = 0,
    var age: Int = 1,
    var hits: Int = 1,
    var predicted: Boolean = false,
    var quality: Double = 1.0,
    val path: ArrayDeque<Point> = ArrayDeque(),
)
