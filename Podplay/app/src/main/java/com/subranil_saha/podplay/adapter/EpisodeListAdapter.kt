package com.subranil_saha.podplay.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.subranil_saha.podplay.databinding.EpisodeItemBinding
import com.subranil_saha.podplay.util.DateUtils
import com.subranil_saha.podplay.util.HtmlUtils
import com.subranil_saha.podplay.viewmodel.PodcastViewModel

class EpisodeListAdapter(private var episodeList: List<PodcastViewModel.EpisodeViewData>?) :
    RecyclerView.Adapter<EpisodeListAdapter.ViewHolder>() {
    inner class ViewHolder(
        dataBinding: EpisodeItemBinding
    ) : RecyclerView.ViewHolder(dataBinding.root) {
        var episodeData: PodcastViewModel.EpisodeViewData? = null
        val titleTextView: TextView = dataBinding.titleView
        val descriptionTextView: TextView = dataBinding.descptionView
        val durationTextView: TextView = dataBinding.durationView
        val releaseDateTextView: TextView = dataBinding.releaseDateView
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): EpisodeListAdapter.ViewHolder {
        return ViewHolder(
            EpisodeItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val episodeList = episodeList ?: return
        val episodeView = episodeList[position]
        holder.episodeData = episodeView
        holder.titleTextView.text = episodeView.title
        holder.descriptionTextView.text = HtmlUtils.htmlToSpannable(episodeView.description ?: "")
        holder.durationTextView.text = episodeView.duration
        holder.releaseDateTextView.text = episodeView.releaseDate?.let {
            DateUtils.dateToShortDate(it)
        }
    }

    override fun getItemCount(): Int {
        return episodeList?.size ?: 0
    }
}