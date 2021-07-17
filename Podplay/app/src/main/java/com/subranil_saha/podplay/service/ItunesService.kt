package com.subranil_saha.podplay.service

import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

interface ItunesService {
    @GET("/search?media=podcast")
    suspend fun searchPodcastByTerm(@Query("term") term: String): Response<PodcastResponse>

    companion object {
        val instance: ItunesService by lazy {
            val baseUrl = "https://itunes.apple.com"
            val retrofit = Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
            retrofit.create(ItunesService::class.java)
        }
    }
}