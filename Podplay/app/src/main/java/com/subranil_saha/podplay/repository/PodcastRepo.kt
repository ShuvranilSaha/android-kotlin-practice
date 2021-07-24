package com.subranil_saha.podplay.repository

import androidx.lifecycle.LiveData
import com.subranil_saha.podplay.db.PodcastDao
import com.subranil_saha.podplay.model.Episode
import com.subranil_saha.podplay.model.Podcast
import com.subranil_saha.podplay.service.RssFeedResponse
import com.subranil_saha.podplay.service.RssFeedService
import com.subranil_saha.podplay.util.DateUtils
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class PodcastRepo(private var feedService: RssFeedService, private var podcastDao: PodcastDao) {
    suspend fun getPodcast(feedUrl: String): Podcast? {
        val podcastLocal = podcastDao.loadPodcast(feedUrl)
        if (podcastLocal != null) {
            podcastLocal.id?.let {
                podcastLocal.episodes = podcastDao.loadEpisodes(it)
                return podcastLocal
            }
        }
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
                null,
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
            null,
            feedUrl,
            rssFeedResponse.title,
            description,
            imageUrl,
            rssFeedResponse.lastUpdated,
            episodes = rssItemsToEpisodes(items)
        )
    }

    @DelicateCoroutinesApi
    fun save(podcast: Podcast) {
        GlobalScope.launch {
            val podcastId = podcastDao.insertPodcast(podcast)
            for (episode in podcast.episodes) {
                episode.podcastId = podcastId
                podcastDao.insertEpisode(episode)
            }
        }
    }

    fun getAll(): LiveData<List<Podcast>> {
        return podcastDao.loadPodcasts()
    }

    @DelicateCoroutinesApi
    fun delete(podcast: Podcast) {
        GlobalScope.launch {
            podcastDao.deletePodcast(podcast)
        }
    }
}