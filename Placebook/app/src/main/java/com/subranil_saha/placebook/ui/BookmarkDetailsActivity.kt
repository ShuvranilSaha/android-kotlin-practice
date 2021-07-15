package com.subranil_saha.placebook.ui

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.databinding.DataBindingUtil
import com.maxkeppeler.sheets.info.InfoSheet
import com.subranil_saha.placebook.R
import com.subranil_saha.placebook.databinding.ActivityBookmarkDetailBinding
import com.subranil_saha.placebook.util.ImageUtils
import com.subranil_saha.placebook.viewmodel.BookmarkDetailViewModel
import kotlinx.coroutines.DelicateCoroutinesApi
import java.io.File
import java.io.IOException
import java.net.URLEncoder

class BookmarkDetailsActivity : AppCompatActivity(),
    PhotoOptionsDialogFragment.PhotoOptionsDialogListener {
    private val bookmarkDetailViewModel by viewModels<BookmarkDetailViewModel>()
    private var bookmarkDetailsView: BookmarkDetailViewModel.BookmarkDetailsView? = null
    private lateinit var dataBinding: ActivityBookmarkDetailBinding
    private var photoFile: File? = null
    @DelicateCoroutinesApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dataBinding = DataBindingUtil.setContentView(this, R.layout.activity_bookmark_detail)
        setupToolbar()
        getIntentData()
        setupSharePlace()
    }

    private fun getIntentData() {
        val bookmarkId = intent.getLongExtra(MapsActivity.EXTRA_BOOKMARK_ID, 0)
        bookmarkDetailViewModel.getBookmark(bookmarkId)?.observe(this, {
            it?.let {
                bookmarkDetailsView = it
                dataBinding.bookmarkDetailsView = it
                populateImageView()
                populateCategoryList()
            }
        })
    }

    private fun populateCategoryList() {
        val bookmarkView = bookmarkDetailsView ?: return
        val resourceId = bookmarkDetailViewModel.getCategoryResourceId(bookmarkView.category)
        resourceId?.let {
            dataBinding.imageViewCategory.setImageResource(it)
        }
        val categories = bookmarkDetailViewModel.getCategories()

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        dataBinding.spinnerCategory.adapter = adapter
        val placeCategory = bookmarkView.category
        dataBinding.spinnerCategory.setSelection(adapter.getPosition(placeCategory))

        dataBinding.spinnerCategory.post {
            dataBinding.spinnerCategory.onItemSelectedListener =
                object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(
                        parent: AdapterView<*>?,
                        view: View?,
                        position: Int,
                        id: Long
                    ) {
                        val category = parent?.getItemAtPosition(position) as String
                        val rId = bookmarkDetailViewModel.getCategoryResourceId(category)
                        rId?.let {
                            dataBinding.imageViewCategory.setImageResource(it)
                        }
                    }

                    override fun onNothingSelected(parent: AdapterView<*>?) {
                        TODO("Not yet implemented")
                    }
                }
        }
    }

    private fun populateImageView() {
        bookmarkDetailsView?.let {
            val placeImage = it.getImage(this)
            placeImage?.let {
                dataBinding.imageViewPlace.setImageBitmap(placeImage)
            }
        }
        dataBinding.imageViewPlace.setOnClickListener {
            replaceImage()
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(dataBinding.toolbar)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_bookmark_details, menu)
        return true
    }

    @DelicateCoroutinesApi
    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.action_save -> {
            saveChanges()
            true
        }
        R.id.action_delete -> {
            deleteBookmark()
            true
        }
        else -> {
            super.onOptionsItemSelected(item)
        }
    }

    @DelicateCoroutinesApi
    private fun saveChanges() {
        val name = dataBinding.editTextName.text.toString()
        if (name.isEmpty()) {
            return
        }
        bookmarkDetailsView?.let {
            it.name = dataBinding.editTextName.text.toString()
            it.address = dataBinding.editTextAddress.text.toString()
            it.phone = dataBinding.editTextPhone.text.toString()
            it.notes = dataBinding.editTextNotes.text.toString()
            it.category = dataBinding.spinnerCategory.selectedItem as String

            bookmarkDetailViewModel.updateBookmark(it)
        }
        finish()
    }

    override fun onCaptureClick() {
        photoFile = null
        try {
            photoFile = ImageUtils.createUniqueImageFile(this)
        } catch (ex: IOException) {
            return
        }
        photoFile?.let { file ->
            val photoUri = FileProvider.getUriForFile(
                this,
                "com.subranil_saha.placebook.fileprovider",
                file
            )
            val captureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            captureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
            val intentActivities = packageManager.queryIntentActivities(
                captureIntent,
                PackageManager.MATCH_DEFAULT_ONLY
            )
            intentActivities.map { it.activityInfo.packageName }
                .forEach {
                    grantUriPermission(
                        it,
                        photoUri,
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    )
                }
            getData.launch(captureIntent)
        }
    }

    private val getData =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                val photoFile = photoFile ?: return@registerForActivityResult
                val uri = FileProvider.getUriForFile(
                    this,
                    "com.subranil_saha.placebook.fileprovider",
                    photoFile
                )
                revokeUriPermission(uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                val image = getImageWithPath(photoFile.absolutePath)
                val bitmap = ImageUtils.rotateImageIfRequired(this, image, uri)
                updateImage(bitmap)
            }
        }

    private fun updateImage(image: Bitmap) {
        bookmarkDetailsView?.let {
            dataBinding.imageViewPlace.setImageBitmap(image)
            it.setImage(this, image)
        }
    }

    @DelicateCoroutinesApi
    private fun deleteBookmark() {
        val bookmarkView = bookmarkDetailsView ?: return

        InfoSheet().show(this) {
            title("Do you want to Delete ?")
            onNegative("Cancel", null)
            onPositive("Okay") {
                bookmarkDetailViewModel.deleteBookmark(bookmarkView)
                finish()
            }
        }

//        AlertDialog.Builder(this)
//            .setMessage("Delete ?")
//            .setPositiveButton("Okay") { _, _ ->
//                bookmarkDetailViewModel.deleteBookmark(bookmarkView)
//                finish()
//            }
//            .setNegativeButton("Cancel", null)
//            .create().show()
    }

    private fun getImageWithPath(filePath: String) = ImageUtils.decodeFileToSize(
        filePath,
        resources.getDimensionPixelSize(R.dimen.default_image_width),
        resources.getDimensionPixelSize(R.dimen.default_image_height)
    )

    override fun onPickClick() {
        val pickClickIntent =
            Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        onPickClickIntent.launch(pickClickIntent)
    }

    private val onPickClickIntent =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                if (it.data != null) {
                    val imageUri = it.data!!.data as Uri
                    val image = getImageWithAuthority(imageUri)
                    image?.let { img ->
                        val bitmap = ImageUtils.rotateImageIfRequired(this, img, imageUri)
                        updateImage(bitmap)
                    }
                }
            }
        }


    private fun getImageWithAuthority(uri: Uri) = ImageUtils.decodeUriStreamToSize(
        uri,
        resources.getDimensionPixelSize(R.dimen.default_image_width),
        resources.getDimensionPixelSize(R.dimen.default_image_height),
        this
    )

    private fun replaceImage() {
        val newFragment = PhotoOptionsDialogFragment.newInstance(this)
        newFragment?.show(supportFragmentManager, "PhotoOptionDialog")
    }

    private fun setupSharePlace() {
        dataBinding.fab.setOnClickListener {
            sharePlace()
        }
    }

    private fun sharePlace() {
        val bookmarkView = bookmarkDetailsView ?: return
        var mapUrl = ""
        mapUrl = if (bookmarkView.placeId == null) {
            val location =
                URLEncoder.encode(
                    "${bookmarkView.latitude}," + "${bookmarkView.longitude}",
                    "utf-8"
                )
            "https://www.google.com/maps/dir/?api=1&destination=$location"
        } else {
            val name = URLEncoder.encode(bookmarkView.name, "utf-8")
            "https://www.google.com/maps/dir/?api=1" +
                    "&destination=$name&destination_place_id=" +
                    "${bookmarkView.placeId}"
        }
        val sendIntent = Intent()
        sendIntent.action = Intent.ACTION_SEND
        sendIntent.putExtra(Intent.EXTRA_TEXT, "Check out ${bookmarkView.name} at:\n$mapUrl")
        sendIntent.putExtra(Intent.EXTRA_SUBJECT, "Sharing ${bookmarkView.name}")
        sendIntent.type = "text/plain"
        startActivity(sendIntent)
    }
}