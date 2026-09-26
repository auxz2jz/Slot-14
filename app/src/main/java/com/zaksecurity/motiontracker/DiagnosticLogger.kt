package com.zaksecurity.motiontracker

import android.content.Context
import java.io.File
import java.time.Instant
import java.util.UUID
import java.util.concurrent.atomic.AtomicLong

class DiagnosticLogger(context: Context) {
    private val directory = File(context.filesDir, "motion_tracker_diagnostics").apply { mkdirs() }
    val eventsFile = File(directory, "events.jsonl")
    val sessionId: String = UUID.randomUUID().toString()
    private val sequence = AtomicLong(0)
    private val startedNanos = System.nanoTime()

    @Synchronized
    fun log(
        category: String,
        event: String,
        result: String? = null,
        details: Map<String, Any?> = emptyMap(),
    ) {
        val json = buildString {
            append('{')
            append("\"sequenceNumber\":${sequence.incrementAndGet()},")
            append("\"timestampUtc\":\"").append(escape(Instant.now().toString())).append("\",")
            append("\"monotonicTimeMs\":${(System.nanoTime() - startedNanos) / 1_000_000},")
            append("\"appSessionId\":\"").append(escape(sessionId)).append("\",")
            append("\"category\":\"").append(escape(category)).append("\",")
            append("\"event\":\"").append(escape(event)).append('"')
            if (result != null) append(",\"result\":\"").append(escape(result)).append('"')
            for ((key, value) in details) {
                append(",\"").append(escape(key)).append("\":")
                when (value) {
                    null -> append("null")
                    is Number, is Boolean -> append(value.toString())
                    else -> append('"').append(escape(value.toString())).append('"')
                }
            }
            append('}')
        }
        rotateIfNeeded()
        eventsFile.appendText(json + "\n")
    }

    private fun rotateIfNeeded() {
        val limitBytes = 2L * 1024L * 1024L
        if (eventsFile.exists() && eventsFile.length() >= limitBytes) {
            val previous = File(eventsFile.parentFile, "events.previous.jsonl")
            if (previous.exists()) previous.delete()
            eventsFile.renameTo(previous)
        }
    }

    private fun escape(value: String): String = value
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
}
