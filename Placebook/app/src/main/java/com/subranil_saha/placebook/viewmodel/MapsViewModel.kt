package com.subranil_saha.placebook.viewmodel

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.model.Place
import com.subranil_saha.placebook.model.Bookmark
import com.subranil_saha.placebook.repository.BookmarkRepo
import com.subranil_saha.placebook.util.ImageUtils

class MapsViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "MapsViewModel"
    private val bookmarkRepo: BookmarkRepo = BookmarkRepo(getApplication())
    private var bookmarks: LiveData<List<BookmarkView>>? = null

    fun addBookmarkFromPlace(place: Place, image: Bitmap?) {
        val bookmark = bookmarkRepo.createBookmark()
        bookmark.placeId = place.id
        bookmark.name = place.name.toString()
        bookmark.latitude = place.latLng?.latitude ?: 0.0
        bookmark.longitude = place.latLng?.longitude ?: 0.0
        bookmark.phone = place.phoneNumber.toString()
        bookmark.address = place.address.toString()
        bookmark.category = getPlaceCategory(place)

        val newId = bookmarkRepo.addBookmark(bookmark)
        image?.let {
            bookmark.setImage(it, getApplication())
        }
        Log.i(TAG, "New bookmark $newId added to database")
    }

    fun getBookmarkViews(): LiveData<List<BookmarkView>>? {
        if (bookmarks == null) {
            mapBookmarkToBookmarkViews()
        }
        return bookmarks
    }

    private fun getPlaceCategory(place: Place): String {
        var category = "Other"
        val types = place.types

        types?.let {
            if (it.size > 0) {
                val placeType = it[0]
                category = bookmarkRepo.placeTypeToCategory(placeType)
            }
        }
        return category
    }

    private fun bookmarkToBookmarkView(bookmark: Bookmark) =
        BookmarkView(
            bookmark.id,
            LatLng(bookmark.latitude, bookmark.longitude),
            bookmark.name,
            bookmark.phone,
            bookmarkRepo.getCategoryResourceId(bookmark.category)
        )

    private fun mapBookmarkToBookmarkViews() {
        bookmarks = Transformations.map(bookmarkRepo.allBookmarks) {
            it.map {
                bookmarkToBookmarkView(it)
            }
        }
    }

    fun addBookmark(latLng: LatLng): Long? {
        val bookmark = bookmarkRepo.createBookmark()
        bookmark.name = "Untitled"
        bookmark.longitude = latLng.longitude
        bookmark.latitude = latLng.latitude
        bookmark.category = "Other"
        return bookmarkRepo.addBookmark(bookmark)
    }

    data class BookmarkView(
        var id: Long? = null,
        var location: LatLng = LatLng(0.0, 0.0),
        var name: String = "",
        var phone: String = "",
        var categoryResourceId: Int? = null
    ) {
        fun getImage(context: Context) = id?.let {
            ImageUtils.loadBitmapFromFile(context, Bookmark.generateImageFileName(it))
        }
    }
}