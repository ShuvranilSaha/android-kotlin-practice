package com.subranil_saha.placebook.repository

import android.content.Context
import androidx.lifecycle.LiveData
import com.google.android.libraries.places.api.model.Place
import com.subranil_saha.placebook.R
import com.subranil_saha.placebook.db.BookmarkDao
import com.subranil_saha.placebook.db.PlaceBookDatabase
import com.subranil_saha.placebook.model.Bookmark

class BookmarkRepo(private val context: Context) {
    private val db = PlaceBookDatabase.getInstance(context)
    private val bookmarkDao: BookmarkDao = db.bookmarkDao()
    private var categoryMap: HashMap<Place.Type, String> = buildCategoryMap()
    private var allCategories: HashMap<String, Int> = buildCategories()

    val categories: List<String>
        get() = ArrayList(allCategories.keys)

    fun addBookmark(bookmark: Bookmark): Long {
        val newId = bookmarkDao.insertBookmark(bookmark)
        bookmark.id = newId;
        return newId
    }
    fun deleteBookmark(bookmark: Bookmark) {
        bookmark.deleteImage(context)
        bookmarkDao.deleteBookmark(bookmark)
    }

    fun getBookmark(bookmarkId: Long): Bookmark {
        return bookmarkDao.loadBookmark(bookmarkId)
    }

    fun updateBookmark(bookmark: Bookmark) {
        bookmarkDao.updateBookmark(bookmark)
    }

    fun createBookmark(): Bookmark {
        return Bookmark()
    }

    fun getLiveBookmark(bookmarkId: Long): LiveData<Bookmark> {
        return bookmarkDao.loadLiveBookmark(bookmarkId)
    }

    fun placeTypeToCategory(placeType: Place.Type): String {
        var category = "Other"
        if (categoryMap.containsKey(placeType)) {
            category = categoryMap[placeType].toString()
        }
        return category
    }

    fun getCategoryResourceId(placeCategory: String): Int? {
        return allCategories[placeCategory]
    }

    private fun buildCategories(): HashMap<String, Int> {
        return hashMapOf(
            "Gas" to R.drawable.ic_gas,
            "Lodging" to R.drawable.ic_lodging,
            "Other" to R.drawable.ic_other,
            "Restaurant" to R.drawable.ic_restaurant,
            "Shopping" to R.drawable.ic_shopping
        )
    }

    val allBookmarks: LiveData<List<Bookmark>>
        get() {
            return bookmarkDao.loadAll()
        }

    private fun buildCategoryMap(): HashMap<Place.Type, String> {
        return hashMapOf(
            Place.Type.BAKERY to "Restaurant",
            Place.Type.BAR to "Restaurant",
            Place.Type.CAFE to "Restaurant",
            Place.Type.FOOD to "Restaurant",
            Place.Type.RESTAURANT to "Restaurant",
            Place.Type.MEAL_DELIVERY to "Restaurant",
            Place.Type.MEAL_TAKEAWAY to "Restaurant",
            Place.Type.GAS_STATION to "Gas",
            Place.Type.CLOTHING_STORE to "Shopping",
            Place.Type.DEPARTMENT_STORE to "Shopping",
            Place.Type.FURNITURE_STORE to "Shopping",
            Place.Type.GROCERY_OR_SUPERMARKET to "Shopping",
            Place.Type.HARDWARE_STORE to "Shopping",
            Place.Type.HOME_GOODS_STORE to "Shopping",
            Place.Type.JEWELRY_STORE to "Shopping",
            Place.Type.SHOE_STORE to "Shopping",
            Place.Type.SHOPPING_MALL to "Shopping",
            Place.Type.STORE to "Shopping",
            Place.Type.LODGING to "Lodging",
            Place.Type.ROOM to "Lodging"
        )
    }
}