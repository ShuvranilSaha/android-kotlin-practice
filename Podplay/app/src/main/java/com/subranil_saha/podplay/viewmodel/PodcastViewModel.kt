package com.subranil_saha.podplay.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.subranil_saha.podplay.model.Episode
import com.subranil_saha.podplay.model.Podcast
import com.subranil_saha.podplay.repository.PodcastRepo
import java.util.*

class PodcastViewModel(application: Application) : AndroidViewModel(application) {
    var podcastRepo: PodcastRepo? = null
    var activePodcastViewData: PodcastViewData? = null

    data class PodcastViewData(
        var subscribed: Boolean = false,
        var feedTitle: String? = "",
        var feedUrl: String? = "",
        var feedDesc: String? = "",
        var imageUrl: String? = "",
        var episode: List<EpisodeViewData>
    )

    data class EpisodeViewData(
        var guid: String? = "",
        var title: String? = "",
        var description: String? = "",
        var mediaUrl: String? = "",
        var releaseData: Date? = null,
        var duration: String? = ""
    )

    private fun episodeToEpisodesView(episodes: List<Episode>): List<EpisodeViewData> {
        return episodes.map {
            EpisodeViewData(
                it.guid,
                it.title,
                it.description,
                it.mediaUrl,
                it.releaseDate,
                it.duration
            )
        }
    }

    private fun podcastToPodcastView(podcast: Podcast): PodcastViewData {
        return PodcastViewData(
            false,
            podcast.feedTitle,
            podcast.feedUrl,
            podcast.feedDescription,
            podcast.imageUrl,
            episodeToEpisodesView(podcast.episodes)
        )
    }

    fun getPodcast(podcastSummaryViewData: SearchViewModel.PodcastSummaryViewData): PodcastViewData? {
        val repo = podcastRepo ?: return null
        val feedUrl = podcastSummaryViewData.feedUrl ?: return null
        val podcast = repo.getPodcast(feedUrl)
        podcast?.let {
            it.feedTitle = podcastSummaryViewData.name ?: ""
            it.imageUrl = podcastSummaryViewData.imageUrl ?: ""
            activePodcastViewData = podcastToPodcastView(it)
            return activePodcastViewData
        }
        return null
    }
}