package com.zaksecurity.motiontracker

import org.opencv.core.Core
import org.opencv.core.Mat
import org.opencv.core.MatOfFloat
import org.opencv.core.MatOfInt
import org.opencv.core.Point
import org.opencv.core.Rect
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import org.opencv.video.BackgroundSubtractorMOG2
import org.opencv.video.Video
import kotlin.math.max
import kotlin.math.min

internal class MotionDetector(
    var pixelThreshold: Double = 20.0,
    var minArea: Double = 90.0,
    var mergeGap: Int = 22,
    private var warmupFrames: Int = 12,
) {
    private var background: BackgroundSubtractorMOG2 = newBackground()
    private var previousGray: Mat? = null
    private var frameNumber = 0L

    fun reset() {
        previousGray?.release()
        previousGray = null
        background = newBackground()
        frameNumber = 0
    }

    private fun newBackground(): BackgroundSubtractorMOG2 =
        Video.createBackgroundSubtractorMOG2(400, 18.0, true)

    fun analyze(rgba: Mat): Pair<Mat, List<MotionDetection>> {
        frameNumber++
        val gray = Mat()
        Imgproc.cvtColor(rgba, gray, Imgproc.COLOR_RGBA2GRAY)
        val mask = buildMask(gray)
        val detections = detect(rgba, mask)
        previousGray?.release()
        previousGray = gray.clone()
        gray.release()
        return mask to detections
    }

    private fun buildMask(gray: Mat): Mat {
        val blur = Mat()
        Imgproc.GaussianBlur(gray, blur, Size(5.0, 5.0), 0.0)
        val diff = Mat.zeros(gray.size(), gray.type())
        previousGray?.let { prev ->
            if (!prev.empty()) {
                val prevBlur = Mat()
                val delta = Mat()
                Imgproc.GaussianBlur(prev, prevBlur, Size(5.0, 5.0), 0.0)
                Core.absdiff(prevBlur, blur, delta)
                Imgproc.threshold(delta, diff, pixelThreshold, 255.0, Imgproc.THRESH_BINARY)
                prevBlur.release(); delta.release()
            }
        }

        val bg = Mat()
        background.apply(gray, bg)
        Imgproc.threshold(bg, bg, 200.0, 255.0, Imgproc.THRESH_BINARY)
        val combined = Mat()
        if (frameNumber <= warmupFrames) {
            diff.copyTo(combined)
        } else {
            val kernel = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, Size(9.0, 9.0))
            val near = Mat()
            val nearbyBg = Mat()
            Imgproc.dilate(diff, near, kernel, Point(-1.0, -1.0), 2)
            Core.bitwise_and(bg, near, nearbyBg)
            Core.bitwise_or(diff, nearbyBg, combined)
            kernel.release(); near.release(); nearbyBg.release()
        }

        val k3 = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, Size(3.0, 3.0))
        val k7 = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, Size(7.0, 7.0))
        val clean = Mat()
        Imgproc.morphologyEx(combined, clean, Imgproc.MORPH_OPEN, k3)
        Imgproc.morphologyEx(clean, clean, Imgproc.MORPH_CLOSE, k7, Point(-1.0, -1.0), 2)
        Imgproc.dilate(clean, clean, k3)
        blur.release(); diff.release(); bg.release(); combined.release(); k3.release(); k7.release()
        return clean
    }

    private fun detect(rgba: Mat, mask: Mat): List<MotionDetection> {
        val labels = Mat()
        val stats = Mat()
        val centroids = Mat()
        val count = Imgproc.connectedComponentsWithStats(mask, labels, stats, centroids)
        val boxes = mutableListOf<Rect>()
        for (i in 1 until count) {
            val area = stats.get(i, 4)?.firstOrNull() ?: 0.0
            if (area < minArea) continue
            val x = (stats.get(i, 0)?.firstOrNull() ?: 0.0).toInt()
            val y = (stats.get(i, 1)?.firstOrNull() ?: 0.0).toInt()
            val w = (stats.get(i, 2)?.firstOrNull() ?: 0.0).toInt()
            val h = (stats.get(i, 3)?.firstOrNull() ?: 0.0).toInt()
            if (w > 0 && h > 0) boxes += Rect(x, y, w, h)
        }
        labels.release(); stats.release(); centroids.release()
        return merge(boxes).filter { it.area() >= minArea }.map { box ->
            MotionDetection(
                box,
                Point(box.x + box.width / 2.0, box.y + box.height / 2.0),
                box.area(),
                histogram(rgba, mask, box),
            )
        }
    }

    private fun merge(input: MutableList<Rect>): List<Rect> {
        var boxes = input.toMutableList()
        var changed = true
        while (changed) {
            changed = false
            val used = BooleanArray(boxes.size)
            val out = mutableListOf<Rect>()
            for (i in boxes.indices) {
                if (used[i]) continue
                var current = boxes[i]; used[i] = true
                var again = true
                while (again) {
                    again = false
                    for (j in boxes.indices) if (!used[j] && close(current, boxes[j])) {
                        current = union(current, boxes[j]); used[j] = true; changed = true; again = true
                    }
                }
                out += current
            }
            boxes = out
        }
        return boxes
    }

    private fun close(a: Rect, b: Rect): Boolean {
        val dx = max(0, max(a.x, b.x) - min(a.x + a.width, b.x + b.width))
        val dy = max(0, max(a.y, b.y) - min(a.y + a.height, b.y + b.height))
        return dx <= mergeGap && dy <= mergeGap
    }

    private fun union(a: Rect, b: Rect): Rect {
        val l=min(a.x,b.x); val t=min(a.y,b.y); val r=max(a.x+a.width,b.x+b.width); val bot=max(a.y+a.height,b.y+b.height)
        return Rect(l,t,r-l,bot-t)
    }

    private fun histogram(rgba: Mat, mask: Mat, box: Rect): Mat {
        val roi = rgba.submat(box); val roiMask = mask.submat(box)
        val rgb = Mat(); val hsv = Mat(); val hist = Mat()
        Imgproc.cvtColor(roi, rgb, Imgproc.COLOR_RGBA2RGB)
        Imgproc.cvtColor(rgb, hsv, Imgproc.COLOR_RGB2HSV)
        Imgproc.calcHist(listOf(hsv), MatOfInt(0,1), roiMask, hist, MatOfInt(24,16), MatOfFloat(0f,180f,0f,256f))
        if (Core.sumElems(hist).`val`[0] <= 0.0) {
            Imgproc.calcHist(listOf(hsv), MatOfInt(0,1), Mat(), hist, MatOfInt(24,16), MatOfFloat(0f,180f,0f,256f))
        }
        Core.normalize(hist, hist, 0.0, 1.0, Core.NORM_MINMAX)
        roi.release(); roiMask.release(); rgb.release(); hsv.release()
        return hist
    }
}
