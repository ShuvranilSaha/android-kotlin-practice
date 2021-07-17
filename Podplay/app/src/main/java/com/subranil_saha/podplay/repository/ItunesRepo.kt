package com.subranil_saha.podplay.repository

import com.subranil_saha.podplay.service.ItunesService

class ItunesRepo(private val itunesService: ItunesService) {
    suspend fun searchByTerm(term: String) = itunesService.searchPodcastByTerm(term)
}