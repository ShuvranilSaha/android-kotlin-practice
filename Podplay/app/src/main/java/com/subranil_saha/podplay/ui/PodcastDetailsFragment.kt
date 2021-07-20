package com.subranil_saha.podplay.ui

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.bumptech.glide.Glide
import com.subranil_saha.podplay.R
import com.subranil_saha.podplay.databinding.FragmentPodcastDetailsBinding
import com.subranil_saha.podplay.viewmodel.PodcastViewModel

class PodcastDetailsFragment : Fragment() {
    private lateinit var dataBinding: FragmentPodcastDetailsBinding
    private val podcastViewModel: PodcastViewModel by activityViewModels()

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
        super.onViewCreated(view, savedInstanceState)
        updateControls()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.menu_details, menu)
    }

    private fun updateControls() {
        val viewData = podcastViewModel.activePodcastViewData ?: return
        dataBinding.feedTitleTextview.text = viewData.feedTitle
        dataBinding.feedDescriptionTextView.text = viewData.feedDesc
        activity?.let {
            Glide.with(it).load(viewData.imageUrl).into(dataBinding.feedImageView)
        }
    }
}