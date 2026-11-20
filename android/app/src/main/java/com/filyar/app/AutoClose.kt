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

    fun schedule(context: Context, delay: Long = DELAY_MS) {
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
        // Music is still playing: keep the app (and its notification) and look again later.
        if (MiniAudio.get(context).playing) {
            AutoClose.schedule(context)
            return
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
