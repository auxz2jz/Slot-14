package com.zaksecurity.motiontracker

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import java.io.BufferedOutputStream
import java.time.Instant
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

internal object DiagnosticExporter{
    fun choose(a:MainActivity){
        a.logger.log("USER_ACTION","EXPORT_DIAGNOSTICS_PRESSED")
        val name="MotionTracker_Diagnostics_v${MainActivity.VERSION}_${Instant.now().toString().replace(':','-')}.zip"
        val i=Intent(Intent.ACTION_CREATE_DOCUMENT).apply{addCategory(Intent.CATEGORY_OPENABLE);type="application/zip";putExtra(Intent.EXTRA_TITLE,name)}
        a.startActivityForResult(i,MainActivity.REQUEST_EXPORT_DIAGNOSTICS)
    }
    fun export(a:MainActivity,uri:Uri){
        a.logger.log("EXPORT","DIAGNOSTIC_EXPORT_STARTED")
        try{
            a.contentResolver.openOutputStream(uri)?.use{raw->ZipOutputStream(BufferedOutputStream(raw)).use{zip->
                fun entry(name:String,text:String){zip.putNextEntry(ZipEntry(name));zip.write(text.toByteArray());zip.closeEntry()}
                entry("README.txt","Security Motion Tracker diagnostics v${MainActivity.VERSION}\nNo source video is included.\n")
                entry("summary.txt","Version: ${MainActivity.VERSION}\nApp session: ${a.logger.sessionId}\nProcessed frames: ${a.processedFrames}\nMax tracks: ${a.maxTrackCount}\nMax changed pixels: ${a.maxChangedPixels}\nLast error: ${a.lastError?:"none"}\nAndroid: ${android.os.Build.VERSION.RELEASE}\nSDK: ${android.os.Build.VERSION.SDK_INT}\nDevice: ${android.os.Build.MODEL}\n")
                zip.putNextEntry(ZipEntry("events.jsonl"));if(a.logger.eventsFile.exists())a.logger.eventsFile.inputStream().use{it.copyTo(zip)};zip.closeEntry()
            }}?:error("Could not open export destination")
            a.logger.log("EXPORT","DIAGNOSTIC_EXPORT_COMPLETED",result="SUCCESS");Toast.makeText(a,"Diagnostics exported.",Toast.LENGTH_SHORT).show()
        }catch(t:Throwable){a.logger.log("ERROR","DIAGNOSTIC_EXPORT_FAILED",result="FAIL",details=mapOf("error" to (t.message?:t.javaClass.name)));Toast.makeText(a,"Diagnostics export failed: ${t.message}",Toast.LENGTH_LONG).show()}
    }
}
