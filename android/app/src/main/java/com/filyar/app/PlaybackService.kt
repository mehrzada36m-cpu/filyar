package com.filyar.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.drawable.Icon
import android.media.MediaMetadata
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Build
import android.os.IBinder
import com.filyar.app.ui.MiniAudio

/** Keeps the music alive in the background and shows the media card (cover, previous, play/pause, next) in the notifications. */
class PlaybackService : Service() {
    private var session: MediaSession? = null
    private val manager by lazy { getSystemService(NotificationManager::class.java) }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        running = true
        manager.createNotificationChannel(NotificationChannel(CHANNEL, "پخش موسیقی", NotificationManager.IMPORTANCE_LOW))
        val audio = MiniAudio.get(this)
        session = MediaSession(this, "filyar").apply {
            setCallback(
                object : MediaSession.Callback() {
                    override fun onPlay() { if (!audio.playing) audio.toggle() }
                    override fun onPause() { if (audio.playing) audio.toggle() }
                    override fun onStop() { audio.close() }
                    override fun onSkipToNext() { audio.next() }
                    override fun onSkipToPrevious() { audio.previous() }
                    override fun onSeekTo(pos: Long) {
                        if (audio.duration > 0) audio.seekFraction(pos.toFloat() / audio.duration)
                    }
                },
            )
            isActive = true
        }
        audio.listener = { refresh() }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val audio = MiniAudio.get(this)
        when (intent?.action) {
            ACTION_TOGGLE -> audio.toggle()
            ACTION_NEXT -> audio.next()
            ACTION_PREV -> audio.previous()
            ACTION_CLOSE -> audio.close()
        }
        runCatching {
            val card = build(audio)
            if (Build.VERSION.SDK_INT >= 29) {
                startForeground(ID, card, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
            } else {
                startForeground(ID, card)
            }
        }
        if (audio.file == null) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
        return START_NOT_STICKY
    }

    private fun refresh() {
        val audio = MiniAudio.get(this)
        if (audio.file == null) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        } else {
            runCatching { manager.notify(ID, build(audio)) }
        }
    }

    private fun build(audio: MiniAudio): Notification {
        val title = audio.trackTitle ?: audio.file?.name?.substringBeforeLast('.') ?: ""
        val artist = audio.artistName ?: "فایل‌یار"
        val playing = audio.playing
        session?.setMetadata(
            MediaMetadata.Builder()
                .putString(MediaMetadata.METADATA_KEY_TITLE, title)
                .putString(MediaMetadata.METADATA_KEY_ARTIST, artist)
                .putLong(MediaMetadata.METADATA_KEY_DURATION, audio.duration.toLong())
                .apply { audio.art?.let { putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, it) } }
                .build(),
        )
        session?.setPlaybackState(
            PlaybackState.Builder()
                .setActions(
                    PlaybackState.ACTION_PLAY or PlaybackState.ACTION_PAUSE or PlaybackState.ACTION_PLAY_PAUSE or
                        PlaybackState.ACTION_SKIP_TO_NEXT or PlaybackState.ACTION_SKIP_TO_PREVIOUS or
                        PlaybackState.ACTION_STOP or PlaybackState.ACTION_SEEK_TO,
                )
                .setState(
                    if (playing) PlaybackState.STATE_PLAYING else PlaybackState.STATE_PAUSED,
                    audio.position.toLong(),
                    if (playing) audio.speed else 0f,
                )
                .build(),
        )
        val open = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        fun action(icon: Int, label: String, act: String, code: Int): Notification.Action =
            Notification.Action.Builder(
                Icon.createWithResource(this, icon),
                label,
                PendingIntent.getService(
                    this,
                    code,
                    Intent(this, PlaybackService::class.java).setAction(act),
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                ),
            ).build()
        val style = Notification.MediaStyle().setShowActionsInCompactView(0, 1, 2)
        session?.sessionToken?.let { style.setMediaSession(it) }
        return Notification.Builder(this, CHANNEL)
            .setSmallIcon(if (playing) android.R.drawable.ic_media_play else android.R.drawable.ic_media_pause)
            .setContentTitle(title)
            .setContentText(artist)
            .setLargeIcon(audio.art)
            .setContentIntent(open)
            .setOnlyAlertOnce(true)
            .setOngoing(playing)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .addAction(action(android.R.drawable.ic_media_previous, "قبلی", ACTION_PREV, 1))
            .addAction(
                action(
                    if (playing) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
                    if (playing) "توقف" else "پخش",
                    ACTION_TOGGLE,
                    2,
                ),
            )
            .addAction(action(android.R.drawable.ic_media_next, "بعدی", ACTION_NEXT, 3))
            .addAction(action(android.R.drawable.ic_menu_close_clear_cancel, "بستن", ACTION_CLOSE, 4))
            .setStyle(style)
            .build()
    }

    override fun onDestroy() {
        running = false
        MiniAudio.get(this).listener = null
        session?.release()
        super.onDestroy()
    }

    companion object {
        const val CHANNEL = "filyar_playback"
        const val ID = 4711
        const val ACTION_TOGGLE = "com.filyar.app.TOGGLE"
        const val ACTION_NEXT = "com.filyar.app.NEXT"
        const val ACTION_PREV = "com.filyar.app.PREV"
        const val ACTION_CLOSE = "com.filyar.app.CLOSE"

        /** True while the service is alive, so the auto-close timer leaves the app alone. */
        @Volatile var running = false
    }
}
