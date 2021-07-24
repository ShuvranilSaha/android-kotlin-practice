package com.subranil_saha.podplay.model

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import java.util.*

@Entity
data class Podcast(
    @PrimaryKey(autoGenerate = true) var id: Long? = null,
    var feedUrl: String = "", // location of RSS feed
    var feedTitle: String = "", // tile of the podcast
    var feedDescription: String = "", // description of podcast
    var imageUrl: String = "", // location of the album art
    var lastUpdated: Date = Date(), // lastUpdated  of the podcast
    @Ignore
    var episodes: List<Episode> = listOf() // list of episodes
)
