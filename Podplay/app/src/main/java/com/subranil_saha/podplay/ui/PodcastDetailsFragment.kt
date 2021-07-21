package com.subranil_saha.podplay.ui

import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.subranil_saha.podplay.R
import com.subranil_saha.podplay.adapter.EpisodeListAdapter
import com.subranil_saha.podplay.databinding.FragmentPodcastDetailsBinding
import com.subranil_saha.podplay.viewmodel.PodcastViewModel

class PodcastDetailsFragment : Fragment() {
    private lateinit var dataBinding: FragmentPodcastDetailsBinding
    private val podcastViewModel: PodcastViewModel by activityViewModels()
    private lateinit var episodeListAdapter: EpisodeListAdapter

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
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        dataBinding = FragmentPodcastDetailsBinding.inflate(inflater, container, false)
        return dataBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        podcastViewModel.podcastLiveData.observe(viewLifecycleOwner, {
            if (it != null) {
                dataBinding.feedTitleTextview.text = it.feedTitle
                dataBinding.feedDescriptionTextView.text = it.feedDesc
                activity?.let { activity ->
                    Glide.with(activity).load(it.imageUrl).into(dataBinding.feedImageView)
                }
                dataBinding.feedDescriptionTextView.movementMethod = ScrollingMovementMethod() // if list is long then it can scroll
                dataBinding.episodeRecyclerView.setHasFixedSize(true) // standard setup for recyclerView
                val layoutManager = LinearLayoutManager(activity)
                dataBinding.episodeRecyclerView.layoutManager = layoutManager
                val dividerItemDecoration = DividerItemDecoration(
                    dataBinding.episodeRecyclerView.context,
                    layoutManager.orientation
                )
                dataBinding.episodeRecyclerView.addItemDecoration(dividerItemDecoration)
                episodeListAdapter = EpisodeListAdapter(it.episode) // list of episodes and assigning to recyclerView
                dataBinding.episodeRecyclerView.adapter = episodeListAdapter
            }
        })
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.menu_details, menu)
    }
}