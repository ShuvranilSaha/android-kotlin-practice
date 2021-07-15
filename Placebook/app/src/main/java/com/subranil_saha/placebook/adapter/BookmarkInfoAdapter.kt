package com.subranil_saha.placebook.adapter

import android.app.Activity
import android.view.View
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.Marker
import com.subranil_saha.placebook.databinding.ContentBookmarkInfoBinding
import com.subranil_saha.placebook.ui.MapsActivity
import com.subranil_saha.placebook.viewmodel.MapsViewModel

class BookmarkInfoAdapter(val context: Activity) : GoogleMap.InfoWindowAdapter {
    private val binding = ContentBookmarkInfoBinding.inflate(context.layoutInflater)
    override fun getInfoWindow(p0: Marker): View? {
        return null
    }

    override fun getInfoContents(p0: Marker): View {
        binding.title.text = p0.title ?: ""
        binding.phone.text = p0.snippet ?: ""
        val imageView = binding.photo
        when (p0.tag) {
            is MapsActivity.PlaceInfo -> {
                imageView.setImageBitmap((p0.tag as MapsActivity.PlaceInfo).image)
            }
            is MapsViewModel.BookmarkView -> {
                val bookmarkView = p0.tag as MapsViewModel.BookmarkView
                imageView.setImageBitmap(bookmarkView.getImage(context))
            }
        }
        return binding.root
    }
}