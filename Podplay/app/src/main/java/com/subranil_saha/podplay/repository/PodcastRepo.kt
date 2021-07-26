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

    private suspend fun getNewEpisodes(localPodcast: Podcast): List<Episode> {
        val response = feedService.getFeed(localPodcast.feedUrl)
        if (response != null) {
            val remotePodcast =
                rssResponseToPodcast(localPodcast.feedUrl, localPodcast.imageUrl, response)
            remotePodcast?.let {
                val localEpisode = podcastDao.loadEpisodes(localPodcast.id!!)
                return remotePodcast.episodes.filter { episode ->
                    localEpisode.find {
                        episode.guid == it.guid
                    } == null
                }
            }
        }
        return listOf()
    }

    @DelicateCoroutinesApi
    private fun saveNewEpisode(podcastId: Long, episodes: List<Episode>) {
        GlobalScope.launch {
            for (episode in episodes) {
                episode.podcastId = podcastId
                podcastDao.insertEpisode(episode)
            }
        }
    }

    class PodcastUpdateInfo(
        val feedUrl: String,
        val name: String,
        val newCount: Int
    )

    @DelicateCoroutinesApi
    suspend fun updatePodcastEpisodes(): MutableList<PodcastUpdateInfo> {
        val updatedPodcast: MutableList<PodcastUpdateInfo> = mutableListOf()
        val podcasts = podcastDao.loadPodcastsStatic()
        for (podcast in podcasts) {
            val newEpisodes = getNewEpisodes(podcast)
            if (newEpisodes.count() > 0) {
                podcast.id?.let {
                    saveNewEpisode(it, newEpisodes)
                    updatedPodcast.add(
                        PodcastUpdateInfo(
                            podcast.feedUrl,
                            podcast.feedTitle,
                            newEpisodes.count()
                        )
                    )
                }
            }
        }
        return updatedPodcast
    }
}