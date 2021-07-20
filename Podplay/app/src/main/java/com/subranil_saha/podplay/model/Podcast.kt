package com.subranil_saha.podplay.model

import java.util.*

data class Podcast(
    var feedUrl: String = "", // location of RSS feed
    var feedTitle: String = "", // tile of the podcast
    var feedDescription: String = "", // description of podcast
    var imageUrl: String = "", // location of the album art
    var lastUpdated: Date = Date(), // lastUpdated  of the podcast
    var episodes: List<Episode> = listOf() // list of episodes
)
