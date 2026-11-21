package com.filyar.app

import android.app.ActivityManager
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.os.SystemClock
import com.filyar.app.ui.MiniAudio

/** Closes the app completely a few minutes after you leave it, and removes it from the recent apps. */
object AutoClose {
    const val DELAY_MS = 3 * 60 * 1000L

    @Volatile var visible = false

    private fun pending(context: Context): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            77,
            Intent(context, CloseReceiver::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    /** Minutes to wait after leaving the app; 0 turns the automatic close off. */
    fun minutes(context: Context): Int =
        context.getSharedPreferences("filyar_ui", Context.MODE_PRIVATE).getInt("autoCloseMin", 3)

    fun schedule(context: Context, delay: Long = minutes(context) * 60_000L) {
        if (delay <= 0L) return
        val alarms = context.getSystemService(AlarmManager::class.java)
        alarms.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + delay, pending(context))
    }

    fun cancel(context: Context) {
        context.getSystemService(AlarmManager::class.java).cancel(pending(context))
    }
}

class CloseReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (AutoClose.visible) return
        // A song is loaded (playing or paused) or the playback service is running: keep the app and look again later.
        if (PlaybackService.running || MiniAudio.get(context).file != null) {
            AutoClose.schedule(context)
            return
        }
        runCatching {
            java.io.File(context.filesDir, "last_autoclose.txt")
                .writeText("${java.util.Date()}\nsong: ${MiniAudio.get(context).file?.name}\nplaying: ${MiniAudio.get(context).playing}\nservice: ${PlaybackService.running}")
        }
        val manager = context.getSystemService(ActivityManager::class.java)
        manager.appTasks.forEach { it.finishAndRemoveTask() }
        val async = goAsync()
        Handler(Looper.getMainLooper()).postDelayed({
            Process.killProcess(Process.myPid())
            async.finish()
        }, 700)
    }
}
