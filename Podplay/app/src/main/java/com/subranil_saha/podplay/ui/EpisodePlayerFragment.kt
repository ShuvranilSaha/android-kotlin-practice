package com.subranil_saha.podplay.ui

import android.animation.ValueAnimator
import android.content.ComponentName
import android.content.Context
import android.graphics.Color
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.text.method.ScrollingMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.activityViewModels
import com.bumptech.glide.Glide
import com.subranil_saha.podplay.databinding.FragmentEpisodePlayerBinding
import com.subranil_saha.podplay.service.PodplayMediaCallback.Companion.CMD_EXTRA_SPEED
import com.subranil_saha.podplay.service.PodplayMediaService
import android.text.format.DateUtils
import android.view.SurfaceHolder
import android.view.animation.LinearInterpolator
import android.widget.LinearLayout
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import com.subranil_saha.podplay.service.PodplayMediaCallback
import com.subranil_saha.podplay.util.HtmlUtils
import com.subranil_saha.podplay.viewmodel.PodcastViewModel

class EpisodePlayerFragment : Fragment() {
    private val podcastViewModel: PodcastViewModel by activityViewModels()
    private var mediaControllerCallback: MediaControllerCallback? = null
    private lateinit var mediaBrowser: MediaBrowserCompat
    private lateinit var dataBinding: FragmentEpisodePlayerBinding
    private var playerSpeed: Float = 1.0f
    private var episodeDuration: Long = 0
    private var draggingScrubber: Boolean = false
    private var progressAnimator: ValueAnimator? = null
    private var mediaSession: MediaSessionCompat? = null
    private var mediaPlayer: MediaPlayer? = null
    private var playOnPrepare: Boolean = false
    private var isVideo: Boolean = false

    companion object {
        fun newInstance(): EpisodePlayerFragment {
            return EpisodePlayerFragment()
        }
    }

    private fun updateControls() {
        dataBinding.episodeTitleTextView.text = podcastViewModel.activeEpisodeViewData?.title
        val htmlDesc = podcastViewModel.activeEpisodeViewData?.description ?: ""
        val descSpan = HtmlUtils.htmlToSpannable(htmlDesc)
        dataBinding.episodeDescriptionTextView.text = descSpan
        dataBinding.episodeDescriptionTextView.movementMethod = ScrollingMovementMethod()
        val fragmentActivity = activity as FragmentActivity
        Glide.with(fragmentActivity).load(podcastViewModel.podcastLiveData.value?.imageUrl)
            .into(dataBinding.episodeImageView)

        val speedBtnText = "${playerSpeed}x"
        dataBinding.speedButton.text = speedBtnText

        mediaPlayer?.let {
            updateControlsFromController()
        }
    }

    private fun startPlaying(episodeViewData: PodcastViewModel.EpisodeViewData) {
        val fragmentActivity = activity as FragmentActivity
        val controller = MediaControllerCompat.getMediaController(fragmentActivity)
        val viewData = podcastViewModel.podcastLiveData
        val bundle = Bundle()

        bundle.putString(MediaMetadataCompat.METADATA_KEY_TITLE, episodeViewData.title)
        bundle.putString(MediaMetadataCompat.METADATA_KEY_ARTIST, viewData.value?.feedTitle)
        bundle.putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI, viewData.value?.imageUrl)

        controller.transportControls.playFromUri(Uri.parse(episodeViewData.mediaUrl), bundle)
    }

    private fun initMediaBrowser() {
        val fragmentActivity = activity as FragmentActivity
        mediaBrowser = MediaBrowserCompat(
            fragmentActivity,
            ComponentName(fragmentActivity, PodplayMediaService::class.java),
            MediaBrowserCallBacks(),
            null
        )
    }

    private fun initMediaSession() {
        if (mediaSession == null) {
            mediaSession = MediaSessionCompat(activity as Context, "EpisodePlayerFragment")
            mediaSession?.setMediaButtonReceiver(null)
        }
        mediaSession?.let {
            registerMediaController(it.sessionToken)
        }
    }

    private fun setSurfaceSize() {
        val mediaPlayer = mediaPlayer ?: return
        val videoWidth = mediaPlayer.videoWidth
        val videoHeight = mediaPlayer.videoHeight

        val parent = dataBinding.videoSurfaceView.parent as View
        val containerWidth = parent.width
        val containerHeight = parent.height

        val layoutAspectRatio = containerWidth.toFloat() / containerHeight
        val videoAspectRatio = videoWidth.toFloat() / videoHeight

        val layoutParams = dataBinding.videoSurfaceView.layoutParams

        if (videoAspectRatio > layoutAspectRatio) {
            layoutParams.height = (containerWidth / videoAspectRatio).toInt()
        } else {
            layoutParams.width = (containerHeight * videoAspectRatio).toInt()
        }
        dataBinding.videoSurfaceView.layoutParams = layoutParams
    }

    private fun initMediaPlayer() {
        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer()
            mediaPlayer?.let { mediaPlayer ->
                mediaPlayer.setAudioAttributes(
                    AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build()
                )
                mediaPlayer.setDataSource(podcastViewModel.activeEpisodeViewData?.mediaUrl)
                mediaPlayer.setOnPreparedListener {
                    val fragmentActivity = activity as FragmentActivity
                    mediaSession?.let { mediaSession ->
                        val episodeCallback =
                            PodplayMediaCallback(fragmentActivity, mediaSession, it)
                        mediaSession.setCallback(episodeCallback)
                    }
                    setSurfaceSize()
                    if (playOnPrepare) {
                        togglePlayPauseButton()
                    }
                }
                mediaPlayer.prepareAsync()
            }
        } else {
            setSurfaceSize()
        }
    }

    private fun initVideoPlayer() {
        dataBinding.videoSurfaceView.visibility = View.VISIBLE
        val surfaceHolder = dataBinding.videoSurfaceView.holder
        surfaceHolder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                initMediaPlayer()
                mediaPlayer?.setDisplay(holder)
            }

            override fun surfaceChanged(
                holder: SurfaceHolder,
                format: Int,
                width: Int,
                height: Int
            ) {
                TODO("Not yet implemented")
            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                TODO("Not yet implemented")
            }
        })
    }

    private fun registerMediaController(token: MediaSessionCompat.Token) {
        val fragmentActivity = activity as FragmentActivity
        val mediaController = MediaControllerCompat(fragmentActivity, token)
        MediaControllerCompat.setMediaController(fragmentActivity, mediaController)
        mediaControllerCallback = MediaControllerCallback()
        mediaController.registerCallback(mediaControllerCallback!!)
    }

    private fun togglePlayPauseButton() {
        playOnPrepare = true
        val fragmentActivity = activity as FragmentActivity
        val controller = MediaControllerCompat.getMediaController(fragmentActivity)
        if (controller.playbackState != null) {
            if (controller.playbackState.state == PlaybackStateCompat.STATE_PLAYING) {
                controller.transportControls.pause()
            } else {
                podcastViewModel.activeEpisodeViewData?.let { startPlaying(it) }
            }
        } else {
            podcastViewModel.activeEpisodeViewData?.let { startPlaying(it) }
        }
    }

    private fun setupControls() {
        dataBinding.playToggleButton.setOnClickListener { togglePlayPauseButton() }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            dataBinding.speedButton.setOnClickListener {
                changeSpeed()
            }
        } else {
            dataBinding.speedButton.visibility = View.INVISIBLE
        }
        dataBinding.forwardButton.setOnClickListener {
            seekBy(30)
        }
        dataBinding.replayButton.setOnClickListener {
            seekBy(-10)
        }
        dataBinding.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                dataBinding.currentTimeTextView.text =
                    DateUtils.formatElapsedTime((progress / 1000).toLong())
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                draggingScrubber = true
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                draggingScrubber = false
                val fragmentActivity = activity as FragmentActivity
                val controller = MediaControllerCompat.getMediaController(fragmentActivity)
                if (controller.playbackState != null) {
                    controller.transportControls.seekTo(seekBar.progress.toLong())
                } else {
                    seekBar.progress = 0
                }
            }
        })
    }

    private fun handleStateChanged(state: Int, position: Long, speed: Float) {
        if (isVideo) {
            setupVideoUI()
        }
        progressAnimator?.cancel()
        progressAnimator = null
        val isPlaying = state == PlaybackStateCompat.STATE_PLAYING
        dataBinding.playToggleButton.isActivated = isPlaying

        val progress = position.toInt()
        dataBinding.seekBar.progress = progress
        val speedBtnText = "${playerSpeed}x"
        dataBinding.speedButton.text = speedBtnText

        if (isPlaying) {
            animateScrubber(progress, speed)
        }
    }

    private fun changeSpeed() {
        playerSpeed += 0.25f
        if (playerSpeed > 2.0f) {
            playerSpeed = 0.75f
        }
        val bundle = Bundle()
        bundle.putFloat(CMD_EXTRA_SPEED, playerSpeed)
        val fragmentActivity = activity as FragmentActivity
        val controller = MediaControllerCompat.getMediaController(fragmentActivity)
        controller.sendCommand(CMD_EXTRA_SPEED, bundle, null)
        val speedBtnText = "${playerSpeed}x"
        dataBinding.speedButton.text = speedBtnText
    }

    private fun seekBy(seconds: Int) {
        val fragmentActivity = activity as FragmentActivity
        val controller = MediaControllerCompat.getMediaController(fragmentActivity)
        val newPosition = controller.playbackState.position + seconds * 1000
        controller.transportControls.seekTo(newPosition)
    }

    private fun updateControlsFromMetadata(metadata: MediaMetadataCompat) {
        episodeDuration = metadata.getLong(MediaMetadataCompat.METADATA_KEY_DURATION)
        dataBinding.endTimeTextView.text = DateUtils.formatElapsedTime((episodeDuration / 1000))
        dataBinding.seekBar.max = episodeDuration.toInt()
    }

    private fun animateScrubber(progress: Int, speed: Float) {
        val timeRemaining = ((episodeDuration - progress) / speed).toInt()
        if (timeRemaining < 0) {
            return
        }
        progressAnimator = ValueAnimator.ofInt(progress, episodeDuration.toInt())
        progressAnimator?.let { animator ->
            animator.duration = timeRemaining.toLong()
            animator.interpolator = LinearInterpolator()
            animator.addUpdateListener {
                if (draggingScrubber) {
                    animator.cancel()
                } else {
                    dataBinding.seekBar.progress = animator.animatedValue as Int
                }
            }
            animator.start()
        }
    }

    private fun updateControlsFromController() {
        val fragmentActivity = activity as FragmentActivity
        val controller = MediaControllerCompat.getMediaController(fragmentActivity)
        if (controller != null) {
            val metadata = controller.metadata
            if (metadata != null) {
                handleStateChanged(
                    controller.playbackState.state,
                    controller.playbackState.position,
                    playerSpeed
                )
                updateControlsFromMetadata(controller.metadata)
            }
        }
    }

    private fun setupVideoUI() {
        dataBinding.episodeDescriptionTextView.visibility = View.INVISIBLE
        dataBinding.headerView.visibility = View.INVISIBLE
        val activity = activity as AppCompatActivity
        activity.supportActionBar?.hide()

        dataBinding.playerControls.setBackgroundColor(Color.argb(255 / 2, 0, 0, 0))
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isVideo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            podcastViewModel.activeEpisodeViewData?.isVideo ?: false
        } else {
            false
        }
        if (!isVideo) {
            initMediaBrowser()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        dataBinding = FragmentEpisodePlayerBinding.inflate(inflater, container, false)
        return dataBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (isVideo) {
            initMediaSession()
            initVideoPlayer()
        }
        setupControls()
        updateControls()
    }

    override fun onStart() {
        super.onStart()
        if (!isVideo) {
            if (mediaBrowser.isConnected) {
                val fragmentActivity = activity as FragmentActivity
                if (MediaControllerCompat.getMediaController(fragmentActivity) == null) {
                    registerMediaController(mediaBrowser.sessionToken)
                }
                updateControlsFromController()
            } else {
                mediaBrowser.connect()
            }
        }
    }

    override fun onStop() {
        super.onStop()
        progressAnimator?.cancel()
        val fragmentActivity = activity as FragmentActivity
        if (MediaControllerCompat.getMediaController(fragmentActivity) != null) {
            mediaControllerCallback?.let {
                MediaControllerCompat.getMediaController(fragmentActivity)
                    .unregisterCallback(it)
            }
        }
        if (isVideo) {
            mediaPlayer?.setDisplay(null)
        }
        if (!fragmentActivity.isChangingConfigurations) {
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }

    inner class MediaBrowserCallBacks : MediaBrowserCompat.ConnectionCallback() {

        override fun onConnected() {
            super.onConnected()
            registerMediaController(mediaBrowser.sessionToken)
            updateControlsFromController()
        }

        override fun onConnectionSuspended() {
            super.onConnectionSuspended()
            println("onConnectionSuspended")
            // Disable transport controls
        }

        override fun onConnectionFailed() {
            super.onConnectionFailed()
            println("onConnectionFailed")
            // Fatal error handling
        }
    }

    inner class MediaControllerCallback : MediaControllerCompat.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            metadata?.let { updateControlsFromMetadata(metadata) }
        }

        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
            val s = state ?: return
            handleStateChanged(s.state, s.position, s.playbackSpeed)
        }
    }
}