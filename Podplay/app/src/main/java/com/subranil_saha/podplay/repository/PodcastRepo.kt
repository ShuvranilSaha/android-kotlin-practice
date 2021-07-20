package com.subranil_saha.podplay.repository

import com.subranil_saha.podplay.model.Podcast

class PodcastRepo {
    fun getPodcast(feedUrl: String): Podcast? {
        return Podcast(
            feedUrl, "No Name", "No description", "No Image"
        )
    }
}