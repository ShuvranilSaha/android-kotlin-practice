package com.subranil_saha.podplay.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.subranil_saha.podplay.databinding.EpisodeItemBinding
import com.subranil_saha.podplay.util.DateUtils
import com.subranil_saha.podplay.util.HtmlUtils
import com.subranil_saha.podplay.viewmodel.PodcastViewModel.EpisodeViewData

class EpisodeListAdapter(
    private var episodeList: List<EpisodeViewData>?,
    private val episodeListAdapterListener: EpisodeListAdapterListener
) :
    RecyclerView.Adapter<EpisodeListAdapter.ViewHolder>() {
    interface EpisodeListAdapterListener {
        fun onSelectedEpisode(episodeViewData: EpisodeViewData)
    }

    inner class ViewHolder(
        dataBinding: EpisodeItemBinding,
        val episodeListAdapterListener: EpisodeListAdapterListener
    ) : RecyclerView.ViewHolder(dataBinding.root) {

        init {
            dataBinding.root.setOnClickListener {
                episodeViewData?.let {
                    episodeListAdapterListener.onSelectedEpisode(it)
                }
            }
        }

        var episodeViewData: EpisodeViewData? = null
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
            ),
            episodeListAdapterListener
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val episodeList = episodeList ?: return
        val episodeView = episodeList[position]
        holder.episodeViewData = episodeView
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