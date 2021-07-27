package com.subranil_saha.podplay.ui

import android.content.ComponentName
import android.content.Context
import android.media.MediaMetadata
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.text.method.ScrollingMovementMethod
import android.view.*
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.subranil_saha.podplay.R
import com.subranil_saha.podplay.adapter.EpisodeListAdapter
import com.subranil_saha.podplay.databinding.FragmentPodcastDetailsBinding
import com.subranil_saha.podplay.service.PodplayMediaService
import com.subranil_saha.podplay.viewmodel.PodcastViewModel
import java.lang.RuntimeException

class PodcastDetailsFragment : Fragment(), EpisodeListAdapter.EpisodeListAdapterListener {

    private val podcastViewModel: PodcastViewModel by activityViewModels()
    private lateinit var databinding: FragmentPodcastDetailsBinding
    private lateinit var episodeListAdapter: EpisodeListAdapter
    private var listener: OnPodcastDetailsListener? = null

    companion object {
        fun newInstance(): PodcastDetailsFragment {
            return PodcastDetailsFragment()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        databinding = FragmentPodcastDetailsBinding.inflate(inflater, container, false)
        return databinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        podcastViewModel.podcastLiveData.observe(viewLifecycleOwner, { viewData ->
            if (viewData != null) {
                databinding.feedTitleTextview.text = viewData.feedTitle
                databinding.feedDescriptionTextView.text = viewData.feedDesc
                activity?.let { activity ->
                    Glide.with(activity).load(viewData.imageUrl).into(databinding.feedImageView)
                }

                // 1
                databinding.feedDescriptionTextView.movementMethod = ScrollingMovementMethod()
                // 2
                databinding.episodeRecyclerView.setHasFixedSize(true)

                val layoutManager = LinearLayoutManager(activity)
                databinding.episodeRecyclerView.layoutManager = layoutManager

                val dividerItemDecoration = DividerItemDecoration(
                    databinding.episodeRecyclerView.context, layoutManager.orientation
                )
                databinding.episodeRecyclerView.addItemDecoration(dividerItemDecoration)
                // 3
                episodeListAdapter = EpisodeListAdapter(viewData.episode, this)
                databinding.episodeRecyclerView.adapter = episodeListAdapter
                activity?.invalidateOptionsMenu()
            }
        })
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnPodcastDetailsListener) {
            listener = context
        } else {
            throw RuntimeException(
                context.toString() +
                        " must implement OnPodcastDetailsListener"
            )
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.menu_details, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_feed_action -> {
                if (item.title == getString(R.string.unsubscribed)) {
                    listener?.onUnsubscribe()
                } else {
                    listener?.onSubscribe()
                }
                true
            }
            else ->
                super.onOptionsItemSelected(item)
        }
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        podcastViewModel.podcastLiveData.observe(viewLifecycleOwner, { podcast ->
            if (podcast != null) {
                menu.findItem(R.id.menu_feed_action).title = if (podcast.subscribed)
                    getString(R.string.unsubscribed) else getString(R.string.subscribe)
            }
        })

        super.onPrepareOptionsMenu(menu)
    }

    override fun onStart() {
        super.onStart()
    }

    override fun onStop() {
        super.onStop()
    }

    private fun updateControls() {
        val viewData = podcastViewModel.podcastLiveData
        databinding.feedTitleTextview.text = viewData.value?.feedTitle
        databinding.feedDescriptionTextView.text = viewData.value?.feedDesc
        activity?.let { activity ->
            Glide.with(activity).load(viewData.value?.imageUrl).into(databinding.feedImageView)
        }
    }

    interface OnPodcastDetailsListener {
        fun onSubscribe()
        fun onUnsubscribe()
        fun onShowEpisodePlayer(episodeViewData: PodcastViewModel.EpisodeViewData)
    }

    override fun onSelectedEpisode(episodeViewData: PodcastViewModel.EpisodeViewData) {
        listener?.onShowEpisodePlayer(episodeViewData)
    }
}