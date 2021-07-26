package com.subranil_saha.podplay.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.media.MediaBrowserServiceCompat
import androidx.media.session.MediaButtonReceiver
import com.subranil_saha.podplay.R
import com.subranil_saha.podplay.ui.PodcastActivity
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.net.URL

class PodplayMediaService : MediaBrowserServiceCompat(), PodplayMediaCallback.PodplayMediaListener {
    private lateinit var mediaSession: MediaSessionCompat

    override fun onCreate() {
        super.onCreate()
        createMediaSession()
    }

    companion object {
        private const val PODPLAY_EMPTY_ROOT_MEDIA_ID = "podplay_empty_root_media_id"
        private const val PLAYER_CHANNEL_ID = "podplay_player_channel"
        private const val NOTIFICATION_ID = 1
    }

    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?
    ): BrowserRoot {
        return BrowserRoot(PODPLAY_EMPTY_ROOT_MEDIA_ID, null)
    }

    override fun onLoadChildren(
        parentId: String,
        result: Result<MutableList<MediaBrowserCompat.MediaItem>>
    ) {
        if (parentId == PODPLAY_EMPTY_ROOT_MEDIA_ID) {
            result.sendResult(null)
        }
    }

    private fun createMediaSession() {
        mediaSession = MediaSessionCompat(this, "PodplayMediaService")
        sessionToken = mediaSession.sessionToken
        val callback = PodplayMediaCallback(this, mediaSession)
        callback.listener = this
        mediaSession.setCallback(callback)
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        mediaSession.controller.transportControls.stop()
    }

    private fun getPlayPauseActions(): Pair<NotificationCompat.Action, NotificationCompat.Action> {
        val pauseAction = NotificationCompat.Action(
            R.drawable.ic_pause_white,
            getString(R.string.pause),
            MediaButtonReceiver.buildMediaButtonPendingIntent(
                this,
                PlaybackStateCompat.ACTION_PAUSE
            )
        )
        val playAction = NotificationCompat.Action(
            R.drawable.ic_play_arrow_white,
            getString(R.string.play),
            MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_PLAY)
        )
        return Pair(pauseAction, playAction)
    }

    private fun isPlaying() =
        mediaSession.controller.playbackState != null && mediaSession.controller.playbackState.state == PlaybackStateCompat.STATE_PLAYING

    private fun getNotificationIntent(): PendingIntent {
        val openActivityIntent = Intent(this, PodcastActivity::class.java)
        openActivityIntent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        return PendingIntent.getActivities(
            this@PodplayMediaService,
            0,
            arrayOf(openActivityIntent),
            PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel() {
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (notificationManager.getNotificationChannel(PLAYER_CHANNEL_ID) == null) {
            val channel =
                NotificationChannel(PLAYER_CHANNEL_ID, "Player", NotificationManager.IMPORTANCE_LOW)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(
        mediaDescription: MediaDescriptionCompat,
        bitmap: Bitmap?
    ): Notification {
        val notificationIntent = getNotificationIntent()
        val (pauseAction, playAction) = getPlayPauseActions()
        val notification = NotificationCompat.Builder(this@PodplayMediaService, PLAYER_CHANNEL_ID)
        notification.setContentTitle(mediaDescription.title)
            .setContentText(mediaDescription.subtitle)
            .setLargeIcon(bitmap)
            .setContentIntent(notificationIntent)
            .setDeleteIntent(
                MediaButtonReceiver.buildMediaButtonPendingIntent(
                    this,
                    PlaybackStateCompat.ACTION_STOP
                )
            )
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setSmallIcon(R.drawable.ic_episode_icon)
            .addAction(if (isPlaying()) pauseAction else playAction)
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setMediaSession(mediaSession.sessionToken).setShowActionsInCompactView(0)
                    .setShowCancelButton(true).setCancelButtonIntent(
                        MediaButtonReceiver.buildMediaButtonPendingIntent(
                            this,
                            PlaybackStateCompat.ACTION_STOP
                        )
                    )
            )
        return notification.build()
    }

    @DelicateCoroutinesApi
    private fun displayNotification() {
        if (mediaSession.controller.metadata == null) {
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel()
        }
        val mediaDescription = mediaSession.controller.metadata.description

        GlobalScope.launch {
            val iconUrl = URL(mediaDescription.iconUri.toString())
            val bitmap = BitmapFactory.decodeStream(iconUrl.openStream())
            val notification = createNotification(mediaDescription, bitmap)

            ContextCompat.startForegroundService(
                this@PodplayMediaService,
                Intent(this@PodplayMediaService, PodplayMediaService::class.java)
            )
            startForeground(PodplayMediaService.NOTIFICATION_ID, notification)
        }
    }

    @DelicateCoroutinesApi
    override fun onStateChanged() {
        displayNotification()
    }

    override fun onStopPlaying() {
        stopSelf()
        stopForeground(true)
    }

    override fun onPausePlaying() {
        stopForeground(true)
    }
}