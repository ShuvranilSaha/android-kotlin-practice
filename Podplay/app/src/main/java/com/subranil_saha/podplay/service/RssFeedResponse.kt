package com.subranil_saha.podplay.service

import java.util.*

data class RssFeedResponse(
    var title: String = "",
    var description: String = "",
    var summary: String = "",
    var lastUpdated: Date = Date(),
    var episodes: MutableList<EpisodeResponse>? = null
) {
    data class EpisodeResponse(
        var title: String? = "",
        var link: String? = "", // url link of the media
        var description: String? = "",
        var guid: String? = "", // unique id of the episode
        var pubDate: String? = "", // publication date
        var duration: String? = "",
        var url: String? = "", // url of ethe Episodes
        var type: String? = "" // type of the media ('audio' or 'video')
    )
}
