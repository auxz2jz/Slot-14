package com.zaksecurity.motiontracker

import android.app.AlertDialog
import android.widget.Toast
import java.util.UUID

internal object GuidedTest {
    fun show(a:MainActivity){
        a.logger.log("USER_ACTION","TEST_THIS_VERSION_PRESSED")
        val prefs=a.getSharedPreferences(MainActivity.PREFS,MainActivity.MODE_PRIVATE)
        var id=prefs.getString("testSessionId",null)
        if(id==null){id=UUID.randomUUID().toString();prefs.edit().putString("testSessionId",id).putString("testState","IN_PROGRESS").apply();a.logger.log("TEST","GUIDED_TEST_STARTED",details=mapOf("testSessionId" to id,"version" to MainActivity.VERSION))}
        val ready=a.processedFrames>=30 && a.maxTrackCount>0 && a.maxChangedPixels>0 && a.lastError==null
        val message="""TEST THIS VERSION — v${MainActivity.VERSION}

WHAT TO DO
1. Select the yard/goat clip.
2. Tap Start Tracking.
3. Let at least 30 analysis frames process.
4. Watch the red changed pixels and green/amber tracking path.

EXPECTED RESULT
The goat/moving object should remain inside one main motion box and its trail should follow its movement. Brief weak-motion periods may turn amber rather than immediately losing the ID.

AUTOMATIC EVIDENCE
Processed frames: ${a.processedFrames}
Maximum simultaneous tracks: ${a.maxTrackCount}
Maximum changed pixels: ${a.maxChangedPixels}
Error: ${a.lastError?:"none"}
Automatic prerequisites: ${if(ready)"PASS" else "NOT READY"}

After the automatic prerequisites pass, use your visual judgment below."""
        AlertDialog.Builder(a).setTitle("Guided Tracking Test").setMessage(message)
            .setPositiveButton("Tracking Looks Correct"){_,_->if(!ready){Toast.makeText(a,"Run at least 30 frames with detected motion first.",Toast.LENGTH_LONG).show();a.logger.log("TEST_RESULT","MANUAL_PASS_BLOCKED",result="BLOCKED",details=mapOf("testSessionId" to id))}else{prefs.edit().putString("testState","PASS").remove("testSessionId").apply();a.logger.log("TEST_RESULT","GUIDED_TEST_FINISHED",result="MANUAL_PASS",details=mapOf("testSessionId" to id));Toast.makeText(a,"Test recorded as PASS.",Toast.LENGTH_SHORT).show()}}
            .setNegativeButton("Problem"){_,_->prefs.edit().putString("testState","FAIL").apply();a.logger.log("TEST_RESULT","GUIDED_TEST_FINISHED",result="MANUAL_FAIL",details=mapOf("testSessionId" to id));Toast.makeText(a,"Failure recorded. Export Diagnostics and share the ZIP.",Toast.LENGTH_LONG).show()}
            .setNeutralButton("Continue Later",null).show()
    }
}
