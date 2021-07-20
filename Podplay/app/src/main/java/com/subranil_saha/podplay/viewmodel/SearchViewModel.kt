package com.subranil_saha.podplay.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.subranil_saha.podplay.repository.ItunesRepo
import com.subranil_saha.podplay.service.PodcastResponse
import com.subranil_saha.podplay.util.DateUtils

class SearchViewModel(application: Application) : AndroidViewModel(application) {
    var itunesRepo: ItunesRepo? = null

    suspend fun searchPodcasts(term: String): List<PodcastSummaryViewData> {
        val results = itunesRepo?.searchByTerm(term)
        if (results != null && results.isSuccessful) {
            val podcasts = results.body()?.results
            if (!podcasts.isNullOrEmpty()) {
                return podcasts.map {
                    itunesPodcastToPodcastSummaryView(it)
                }
            }
        }
        return emptyList()
    }

    private fun itunesPodcastToPodcastSummaryView(itunesPodcast: PodcastResponse.ItunesPodcast): PodcastSummaryViewData {
        return PodcastSummaryViewData(
            itunesPodcast.collectionCensoredName,
            DateUtils.jsonToShortDate(itunesPodcast.releaseDate),
            itunesPodcast.artworkUrl30,
            itunesPodcast.feedUrl
        )
    }

    data class PodcastSummaryViewData(
        var name: String? = "",
        var lastUpdated: String? = "",
        var imageUrl: String? = "",
        var feedUrl: String? = ""
    )
}