package com.subranil_saha.podplay.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.subranil_saha.podplay.db.PodPlayDatabase
import com.subranil_saha.podplay.db.PodcastDao
import com.subranil_saha.podplay.model.Episode
import com.subranil_saha.podplay.model.Podcast
import com.subranil_saha.podplay.repository.PodcastRepo
import com.subranil_saha.podplay.util.DateUtils
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.launch
import java.util.*

class PodcastViewModel(application: Application) : AndroidViewModel(application) {
    var podcastRepo: PodcastRepo? = null
    var activePodcast: Podcast? = null
    var activeEpisodeViewData: EpisodeViewData? = null

    @InternalCoroutinesApi
    val podcastDao: PodcastDao = PodPlayDatabase.getInstance(application, viewModelScope)
        .podcastDao()
    private val _podcastLiveData = MutableLiveData<PodcastViewData?>()
    val podcastLiveData: LiveData<PodcastViewData?> = _podcastLiveData
    private var livePodcastSummaryData: LiveData<List<SearchViewModel.PodcastSummaryViewData>>? =
        null

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
        var releaseDate: Date? = null,
        var duration: String? = "",
        var isVideo: Boolean = false
    )

    private fun episodeToEpisodesView(episodes: List<Episode>): List<EpisodeViewData> {
        return episodes.map {
            val isVideo = it.mimeType.startsWith("video")
            EpisodeViewData(
                it.guid,
                it.title,
                it.description,
                it.mediaUrl,
                it.releaseDate,
                it.duration,
                isVideo
            )
        }
    }

    private fun podcastToPodcastView(podcast: Podcast): PodcastViewData {
        return PodcastViewData(
            podcast.id != null,
            podcast.feedTitle,
            podcast.feedUrl,
            podcast.feedDescription,
            podcast.imageUrl,
            episodeToEpisodesView(podcast.episodes)
        )
    }

    suspend fun getPodcast(podcastSummaryViewData: SearchViewModel.PodcastSummaryViewData) {
        podcastSummaryViewData.feedUrl?.let {
            viewModelScope.launch {
                podcastRepo?.getPodcast(it)?.let {
                    it.feedTitle = podcastSummaryViewData.name ?: ""
                    it.imageUrl = podcastSummaryViewData.imageUrl ?: ""
                    _podcastLiveData.value = podcastToPodcastView(it)
                    activePodcast = it
                } ?: run {
                    _podcastLiveData.value = null
                }
            }
        } ?: run {
            _podcastLiveData.value = null
        }
    }

    @DelicateCoroutinesApi
    fun saveActivePodcast() {
        val repo = podcastRepo ?: return
        activePodcast?.let {
            repo.save(it)
        }
    }

    private fun podcastToSummaryView(podcast: Podcast): SearchViewModel.PodcastSummaryViewData {
        return SearchViewModel.PodcastSummaryViewData(
            podcast.feedTitle,
            DateUtils.dateToShortDate(podcast.lastUpdated),
            podcast.imageUrl,
            podcast.feedUrl
        )
    }

    fun getPodcasts(): LiveData<List<SearchViewModel.PodcastSummaryViewData>>? {
        val repo = podcastRepo ?: return null
        if (livePodcastSummaryData == null) {
            val liveData = repo.getAll()
            livePodcastSummaryData = Transformations.map(liveData) { podcastList ->
                podcastList.map {
                    podcastToSummaryView(it)
                }
            }
        }
        return livePodcastSummaryData
    }

    @DelicateCoroutinesApi
    fun deleteActivePodcast() {
        val repo = podcastRepo ?: return
        activePodcast?.let {
            repo.delete(it)
        }
    }

    suspend fun setActivePodcast(feedUrl: String): SearchViewModel.PodcastSummaryViewData? {
        val repo = podcastRepo ?: return null
        val podcast = repo.getPodcast(feedUrl)

        return if (podcast == null) {
            null
        } else {
            _podcastLiveData.value = podcastToPodcastView(podcast)
            activePodcast = podcast
            podcastToSummaryView(podcast)
        }
    }
}