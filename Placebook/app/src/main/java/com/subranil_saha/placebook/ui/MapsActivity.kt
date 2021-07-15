package com.subranil_saha.placebook.ui

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.common.GooglePlayServicesNotAvailableException
import com.google.android.gms.common.GooglePlayServicesRepairableException
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.model.RectangularBounds
import com.google.android.libraries.places.api.net.FetchPhotoRequest
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import com.subranil_saha.placebook.R
import com.subranil_saha.placebook.adapter.BookmarkInfoAdapter
import com.subranil_saha.placebook.adapter.BookmarkListAdapter
import com.subranil_saha.placebook.databinding.ActivityMapsBinding
import com.subranil_saha.placebook.viewmodel.MapsViewModel
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import www.sanju.motiontoast.MotionToast
import java.util.*

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {
    private lateinit var map: GoogleMap
    private lateinit var databinding: ActivityMapsBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var placesClient: PlacesClient
    private lateinit var bookmarkListAdapter: BookmarkListAdapter
    private var markers = HashMap<Long, Marker>()

    private val mapsViewModel by viewModels<MapsViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        databinding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(databinding.root)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        setUpLocationClient()
        setupPlacesClient()
        setupToolbar()
        setupNavigationDrawer()
    }

    private fun setUpLocationClient() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
    }

    private fun setupToolbar() {
        setSupportActionBar(databinding.mainMapView.toolbar)
        val toogle =
            ActionBarDrawerToggle(
                this,
                databinding.drawerLayout,
                databinding.mainMapView.toolbar,
                R.string.open_drawer,
                R.string.close_drawer
            )
        toogle.syncState()
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_LOCATION) {
            if (grantResults.size == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                MotionToast.createToast(
                    this,
                    getString(R.string.toast_success),
                    getString(R.string.permission_granted),
                    MotionToast.TOAST_SUCCESS,
                    MotionToast.GRAVITY_BOTTOM,
                    MotionToast.LONG_DURATION,
                    null
                )
                fetchCurrentLocation()
            } else {
                MotionToast.createToast(
                    this,
                    getString(R.string.err_toast_heading),
                    getString(R.string.permission_denied),
                    MotionToast.TOAST_ERROR,
                    MotionToast.GRAVITY_BOTTOM,
                    MotionToast.LONG_DURATION,
                    null
                )
            }
        }
    }

    @DelicateCoroutinesApi
    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        fetchCurrentLocation()
        setupMapListeners()
        createBookmarkObserver()
    }

    @DelicateCoroutinesApi
    private fun setupMapListeners() {
        map.setInfoWindowAdapter(BookmarkInfoAdapter(this))
        map.setOnPoiClickListener {
            displayPoi(it)
        }
        map.setOnInfoWindowClickListener {
            handleInfoWindowClick(it)
        }
        databinding.mainMapView.fab.setOnClickListener {
            searchAtCurrentLocation()
        }

        map.setOnMapLongClickListener {
            newBookmark(it)
        }
    }

    private fun setupPlacesClient() {
        Places.initialize(applicationContext, getString(R.string.google_maps_key))
        placesClient = Places.createClient(this)
    }

    private fun displayPoi(pointOfInterest: PointOfInterest) {
        showProgress()
        displayPoiGetPlaceStep(pointOfInterest)
    }

    private fun displayPoiGetPlaceStep(pointOfInterest: PointOfInterest) {
        val placeId = pointOfInterest.placeId

        val placeFields = listOf(
            Place.Field.ID,
            Place.Field.NAME,
            Place.Field.PHONE_NUMBER,
            Place.Field.PHOTO_METADATAS,
            Place.Field.ADDRESS,
            Place.Field.LAT_LNG,
            Place.Field.TYPES
        )

        val request = FetchPlaceRequest.builder(placeId, placeFields)
            .build()
        placesClient.fetchPlace(request).addOnSuccessListener {
            val place = it.place
            displayPoiGetPhotoStep(place)
        }.addOnFailureListener {
            if (it is ApiException) {
                val statusCode = it.statusCode
                Log.e(TAG, "Places not found: " + it.message + ", " + "statusCode: " + statusCode)
                hideProgress()
            }
        }
    }

    private fun displayPoiGetPhotoStep(place: Place) {
        val photoMetadata = place.photoMetadatas?.get(0)
        if (photoMetadata == null) {
            displayPoiDisplayStep(place, null)
            return
        }

        val photoRequest = FetchPhotoRequest
            .builder(photoMetadata)
            .setMaxHeight(resources.getDimensionPixelSize(R.dimen.default_image_height))
            .setMaxWidth(resources.getDimensionPixelSize(R.dimen.default_image_width))
            .build()
        placesClient.fetchPhoto(photoRequest).addOnSuccessListener {
            val bitmap = it.bitmap
            displayPoiDisplayStep(place, bitmap)
        }.addOnFailureListener {
            if (it is ApiException) {
                val statusCode = it.statusCode
                Log.e(TAG, "Places not found: " + it.message + ", " + "statusCode: " + statusCode)
                hideProgress()
            }
        }
    }

    private fun displayPoiDisplayStep(place: Place, photo: Bitmap?) {
        hideProgress()
        val marker = map.addMarker(
            MarkerOptions()
                .position(place.latLng as LatLng)
                .title(place.name)
                .snippet(place.phoneNumber)
        )
        marker?.tag = PlaceInfo(place, photo)
        marker?.showInfoWindow()
    }

    @DelicateCoroutinesApi
    private fun handleInfoWindowClick(marker: Marker) {
        when (marker.tag) {
            is PlaceInfo -> {
                val placeInfo = (marker.tag as PlaceInfo)
                if (placeInfo.place != null && placeInfo.image != null) {
                    GlobalScope.launch {
                        mapsViewModel.addBookmarkFromPlace(placeInfo.place, placeInfo.image)
                    }
                }
                marker.remove()
            }
            is MapsViewModel.BookmarkView -> {
                val bookmarkView = (marker.tag as MapsViewModel.BookmarkView)
                marker.hideInfoWindow()
                bookmarkView.id?.let {
                    startBookmarkDetails(it)
                }
            }
        }
    }

    @DelicateCoroutinesApi
    private fun newBookmark(latLng: LatLng) {
        GlobalScope.launch {
            val bookmarkId = mapsViewModel.addBookmark(latLng)
            bookmarkId?.let {
                startBookmarkDetails(it)
            }
        }
    }

    private fun createBookmarkObserver() {
        mapsViewModel.getBookmarkViews()?.observe(this, {
            markers.clear()
            map.clear()
            it?.let {
                displayAllBookmarks(it)
                bookmarkListAdapter.setBookmarkData(it)
            }
        })
    }

    // a helper method to zoom the map to a specific location.
    private fun updateMapLocation(location: Location) {
        val latLang = LatLng(location.latitude, location.longitude)
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLang, 16.0f))
    }

    fun moveToBookmark(bookmark: MapsViewModel.BookmarkView) {
        databinding.drawerLayout.closeDrawer(databinding.drawerViewMaps.drawerView)
        val marker = markers[bookmark.id]
        marker?.showInfoWindow()
        val location = Location("")
        location.latitude = bookmark.location.latitude
        location.longitude = bookmark.location.longitude
        updateMapLocation(location)
    }

    private fun displayAllBookmarks(bookmarks: List<MapsViewModel.BookmarkView>) {
        bookmarks.forEach {
            addPlaceMarker(it)
        }
    }

    private fun addPlaceMarker(bookmark: MapsViewModel.BookmarkView): Marker? {
        val marker = map.addMarker(
            MarkerOptions()
                .position(bookmark.location)
                .title(bookmark.name)
                .snippet(bookmark.phone)
                .icon(bookmark.categoryResourceId?.let { BitmapDescriptorFactory.fromResource(it) })
                .alpha(0.8f)
        )
        marker.tag = bookmark
        bookmark.id?.let {
            markers.put(it, marker)
        }
        return marker
    }

    private fun fetchCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestLocationPermission()
        } else {
            map.isMyLocationEnabled = true
            fusedLocationClient.lastLocation.addOnCompleteListener {
                if (it.result != null) {
                    val latLang = LatLng(it.result.latitude, it.result.longitude)
                    val update = CameraUpdateFactory.newLatLngZoom(latLang, 16.0f)
                    map.moveCamera(update)
                } else {
                    Log.e(TAG, "location error")
                }
            }
        }
    }

    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            REQUEST_LOCATION
        )
    }

    private fun startBookmarkDetails(bookmarkId: Long) {
        val intent = Intent(this, BookmarkDetailsActivity::class.java)
        intent.putExtra(EXTRA_BOOKMARK_ID, bookmarkId)
        startActivity(intent)
    }

    private fun setupNavigationDrawer() {
        val layoutManager = LinearLayoutManager(this)
        databinding.drawerViewMaps.bookmarkRecyclerView.layoutManager = layoutManager
        bookmarkListAdapter = BookmarkListAdapter(null, this)
        databinding.drawerViewMaps.bookmarkRecyclerView.adapter = bookmarkListAdapter
    }

    private fun searchAtCurrentLocation() {
        val placeFields = listOf(
            Place.Field.ID,
            Place.Field.NAME,
            Place.Field.PHONE_NUMBER,
            Place.Field.PHOTO_METADATAS,
            Place.Field.LAT_LNG,
            Place.Field.ADDRESS,
            Place.Field.TYPES
        )

        val bounds = RectangularBounds.newInstance(map.projection.visibleRegion.latLngBounds)

        try {
            val intent = Autocomplete.IntentBuilder(AutocompleteActivityMode.OVERLAY, placeFields)
                .setLocationBias(bounds)
                .build(this)
            getData.launch(intent)
        } catch (e: GooglePlayServicesRepairableException) {
            Toast.makeText(this, "Problem Searching", Toast.LENGTH_LONG).show()
        } catch (e: GooglePlayServicesNotAvailableException) {
            Toast.makeText(this, "Problem Searching Google play not available", Toast.LENGTH_LONG)
                .show()
        }
    }

    private val getData =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK && it.data != null) {
                val place = Autocomplete.getPlaceFromIntent(it.data!!)
                val location = Location("")
                location.latitude = place.latLng?.latitude ?: 0.0
                location.longitude = place.latLng?.longitude ?: 0.0
                updateMapLocation(location)
                showProgress()
                displayPoiGetPhotoStep(place)
            }
        }

    private fun showProgress() {
        databinding.mainMapView.progressBar.visibility = ProgressBar.VISIBLE
        disableUserInteraction()
    }

    private fun hideProgress() {
        databinding.mainMapView.progressBar.visibility = ProgressBar.GONE
        enableUserInteraction()
    }

    private fun disableUserInteraction() {
        window.setFlags(
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        )
    }

    private fun enableUserInteraction() {
        window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }

    companion object {
        private const val REQUEST_LOCATION = 1
        private const val TAG = "MapsActivity"
        const val EXTRA_BOOKMARK_ID = "com.subranil_saha.placebook.EXTRA_BOOKMARK_ID"
    }

    class PlaceInfo(val place: Place? = null, val image: Bitmap? = null) {

    }
}