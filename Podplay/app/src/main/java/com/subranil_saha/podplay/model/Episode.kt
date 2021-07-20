package com.subranil_saha.podplay.model

import java.util.*

data class Episode(
    var guid: String, // unique identifier in RSS feed
    var title: String, // name of the episode
    var description: String, // description of the episode
    var mediaUrl: String, // location of the episode media, can be audio or video file
    var mimeType: String, // determines the type of file located at mediaUrl
    var releaseDate: Date = Date(), // release date of the episode
    var duration: String // duration of the episode provided in RSS feed
)
