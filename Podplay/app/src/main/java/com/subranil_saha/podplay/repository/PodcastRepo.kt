package com.subranil_saha.podplay.repository

import com.subranil_saha.podplay.model.Episode
import com.subranil_saha.podplay.model.Podcast
import com.subranil_saha.podplay.service.RssFeedResponse
import com.subranil_saha.podplay.service.RssFeedService
import com.subranil_saha.podplay.util.DateUtils

class PodcastRepo(private var feedService: RssFeedService) {
    suspend fun getPodcast(feedUrl: String): Podcast? {
        var podcast: Podcast? = null
        val feedResponse = feedService.getFeed(feedUrl)
        if (feedResponse != null) {
            podcast = rssResponseToPodcast(feedUrl, "", feedResponse)
        }
        return podcast
    }

    private fun rssItemsToEpisodes(episodeResponse: List<RssFeedResponse.EpisodeResponse>): List<Episode> {
        return episodeResponse.map {
            Episode(
                it.guid ?: "",
                it.title ?: "",
                it.description ?: "",
                it.url ?: "",
                it.type ?: "",
                DateUtils.xmlDateToDate(it.pubDate),
                it.duration ?: ""
            )
        }
    }

    private fun rssResponseToPodcast(
        feedUrl: String,
        imageUrl: String,
        rssFeedResponse: RssFeedResponse
    ): Podcast? {
        val items = rssFeedResponse.episodes ?: return null

        val description = if (rssFeedResponse.description == "") {
            rssFeedResponse.summary
        } else {
            rssFeedResponse.description
        }
        return Podcast(
            feedUrl,
            rssFeedResponse.title,
            description,
            imageUrl,
            rssFeedResponse.lastUpdated,
            episodes = rssItemsToEpisodes(items)
        )
    }
}