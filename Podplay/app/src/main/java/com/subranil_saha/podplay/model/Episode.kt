package com.subranil_saha.podplay.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.*

@Entity(
    foreignKeys = [
        ForeignKey(
            entity = Podcast::class,
            parentColumns = ["id"],
            childColumns = ["podcastId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("podcastId")]
)
data class Episode(
    @PrimaryKey var guid: String = "", // unique identifier in RSS feed
    var podcastId: Long? = null, // foreign key defines the podcastId
    var title: String, // name of the episode
    var description: String, // description of the episode
    var mediaUrl: String, // location of the episode media, can be audio or video file
    var mimeType: String, // determines the type of file located at mediaUrl
    var releaseDate: Date = Date(), // release date of the episode
    var duration: String // duration of the episode provided in RSS feed
)
