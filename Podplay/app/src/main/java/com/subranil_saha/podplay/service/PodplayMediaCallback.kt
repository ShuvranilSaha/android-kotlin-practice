package com.subranil_saha.podplay.service

import android.content.Context
import android.media.*
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.media.MediaPlayer
import android.media.session.PlaybackState
import android.os.ResultReceiver
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import java.lang.Exception

class PodplayMediaCallback(
    val context: Context,
    val mediaSession: MediaSessionCompat,
    var mediaPlayer: MediaPlayer? = null
) : MediaSessionCompat.Callback() {
    private var mediaUri: Uri? = null
    private var newMedia: Boolean = false
    private var mediaExtras: Bundle? = null
    private var focusRequest: AudioFocusRequest? = null

    var listener: PodplayMediaListener? = null
    private var mediaNeedsPrepare: Boolean = false

    override fun onPlay() {
        super.onPlay()
        if (ensureAudioFocus()) {
            mediaSession.isActive = true
            initialiseMediaPlayer()
            prepareMedia()
            startPlaying()
        }
    }

    override fun onPlayFromUri(uri: Uri?, extras: Bundle?) {
        super.onPlayFromUri(uri, extras)
        if (mediaUri == uri) {
            newMedia = false
            mediaExtras = null
        } else {
            mediaExtras = extras
            setNewMedia(uri)
        }
        onPlay()
    }

    override fun onPause() {
        super.onPause()
        println("on pause called")
        pausePlaying()
    }

    override fun onStop() {
        super.onStop()
        stopPlaying()
    }

    private fun startPlaying() {
        mediaPlayer?.let {
            if (!it.isPlaying) {
                it.start()
                setState(PlaybackStateCompat.STATE_PLAYING)
            }
        }
    }

    private fun pausePlaying() {
        removeAudioFocus()
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.pause()
                setState(PlaybackStateCompat.STATE_PAUSED)
            }
        }
        listener?.onPausePlaying()
    }

    companion object {
        const val CMD_CHANGE_SPEED = "change_speed"
        const val CMD_EXTRA_SPEED = "speed"
    }

    private fun stopPlaying() {
        removeAudioFocus()
        mediaSession.isActive = false
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.stop()
                setState(PlaybackStateCompat.STATE_STOPPED)
            }
        }
        listener?.onStopPlaying()
    }

    private fun setState(state: Int, newSpeed: Float? = null) {
        var position: Long = -1
        mediaPlayer?.let {
            position = it.currentPosition.toLong()
        }
        var speed = 1.0f
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            speed = newSpeed ?: (mediaPlayer?.playbackParams?.speed ?: 1.0f)
            mediaPlayer?.let {
                try {
                    it.playbackParams = it.playbackParams.setSpeed(speed)
                } catch (e: Exception) {
                    it.reset()
                    mediaUri?.let { mediaUri ->
                        it.setDataSource(context, mediaUri)
                    }
                    it.prepare()
                    it.playbackParams = it.playbackParams.setSpeed(speed)
                    it.seekTo(position.toInt())
                    if (state == PlaybackStateCompat.STATE_PLAYING) {
                        it.start()
                    }
                }
            }
        }
        val playbackState = PlaybackStateCompat.Builder()
            .setActions(
                PlaybackStateCompat.ACTION_PLAY or
                        PlaybackStateCompat.ACTION_STOP or
                        PlaybackStateCompat.ACTION_PLAY_PAUSE or
                        PlaybackStateCompat.ACTION_PAUSE
            )
            .setState(state, position, speed)
            .build()
        mediaSession.setPlaybackState(playbackState)
        if (state == PlaybackStateCompat.STATE_PAUSED || state == PlaybackStateCompat.STATE_PLAYING) {
            listener?.onStateChanged()
        }
    }

    private fun changeSpeed(extras: Bundle) {
        var playbackState = PlaybackStateCompat.STATE_PAUSED
        if (mediaSession.controller.playbackState != null) {
            playbackState = mediaSession.controller.playbackState.state
        }
        setState(playbackState, extras.getFloat(CMD_EXTRA_SPEED))
    }

    private fun setNewMedia(uri: Uri?) {
        newMedia = true
        mediaUri = uri
    }

    private fun ensureAudioFocus(): Boolean {
        val audioManager = this.context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN).run {
                setAudioAttributes(AudioAttributes.Builder().run {
                    setUsage(AudioAttributes.USAGE_MEDIA)
                    setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                })
                build()
            }
            this.focusRequest = focusRequest
            val result = audioManager.requestAudioFocus(focusRequest)
            return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        } else {
            val result = audioManager.requestAudioFocus(
                null,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            )
            return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }
    }

    private fun removeAudioFocus() {
        val audioManager = this.context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            focusRequest?.let {
                audioManager.abandonAudioFocusRequest(it)
            }
        } else {
            audioManager.abandonAudioFocus(null)
        }
    }

    private fun initialiseMediaPlayer() {
        if (mediaPlayer == null) {
            mediaNeedsPrepare = true
            mediaPlayer = MediaPlayer()
            mediaPlayer?.setOnCompletionListener {
                setState(PlaybackStateCompat.STATE_PAUSED)
            }
        }
    }

    private fun prepareMedia() {
        if (newMedia) {
            newMedia = false
            mediaPlayer?.let { mediaPlayer ->
                mediaUri?.let { mediaUri ->
                   if (mediaNeedsPrepare) {
                       mediaPlayer.reset()
                       mediaPlayer.setDataSource(context, mediaUri)
                       mediaPlayer.prepare()
                   }
                    mediaExtras?.let {
                        mediaSession.setMetadata(
                            MediaMetadataCompat.Builder()
                                .putString(
                                    MediaMetadataCompat.METADATA_KEY_TITLE,
                                    it.getString(MediaMetadataCompat.METADATA_KEY_TITLE)
                                ).putString(
                                    MediaMetadataCompat.METADATA_KEY_ARTIST,
                                    it.getString(MediaMetadataCompat.METADATA_KEY_ARTIST)
                                ).putString(
                                    MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI,
                                    it.getString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI)
                                ).putLong(
                                    MediaMetadataCompat.METADATA_KEY_DURATION,
                                    mediaPlayer.duration.toLong()
                                ).build()
                        )
                    }
                }
            }
        }
    }

    override fun onSeekTo(pos: Long) {
        super.onSeekTo(pos)
        mediaPlayer?.seekTo(pos.toInt())
        val playbackState: PlaybackStateCompat? = mediaSession.controller.playbackState

        if (playbackState != null) {
            setState(playbackState.state)
        } else {
            setState(PlaybackStateCompat.STATE_PAUSED)
        }
    }

    override fun onCommand(command: String?, extras: Bundle?, cb: ResultReceiver?) {
        super.onCommand(command, extras, cb)
        when (command) {
            CMD_CHANGE_SPEED -> extras?.let { changeSpeed(it) }
        }
    }

    interface PodplayMediaListener {
        fun onStateChanged()
        fun onStopPlaying()
        fun onPausePlaying()
    }
}