package com.subranil_saha.placebook.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.os.Environment
import java.io.*
import java.text.SimpleDateFormat
import java.util.*

object ImageUtils {
    fun saveBitmapToFile(context: Context, bitmap: Bitmap, fileName: String) {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        val bytes = stream.toByteArray()
        saveBytesToFile(context, bytes, fileName)
    }

    private fun saveBytesToFile(context: Context, bytes: ByteArray, fileName: String) {
        val outputStream: FileOutputStream
        try {
            outputStream = context.openFileOutput(fileName, Context.MODE_PRIVATE)
            outputStream.write(bytes)
            outputStream.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun loadBitmapFromFile(context: Context, fileName: String): Bitmap? {
        val filePath = File(context.filesDir, fileName).absolutePath
        return BitmapFactory.decodeFile(filePath)
    }

    @Throws(IOException::class)
    fun createUniqueImageFile(context: Context): File {
        val timeStamp = SimpleDateFormat.getDateTimeInstance().format(Date())
        val fileName = "PlaceBook_" + timeStamp + "_"
        val filesDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(fileName, ".jpg", filesDir)
    }

    private fun calculateInSampleSize(width: Int, height: Int, reqWidth: Int, reqHeight: Int): Int {
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    fun decodeFileToSize(filePath: String, width: Int, height: Int): Bitmap {
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        BitmapFactory.decodeFile(filePath, options)
        options.inSampleSize =
            calculateInSampleSize(options.outWidth, options.outHeight, width, height)
        options.inJustDecodeBounds = false
        return BitmapFactory.decodeFile(filePath, options)
    }

    private fun rotateImage(image: Bitmap, degree: Float): Bitmap? {
        val matrix = Matrix()
        matrix.postRotate(degree)
        val rotatedImage = Bitmap.createBitmap(image, 0, 0, image.width, image.height, matrix, true)
        image.recycle()
        return rotatedImage
    }

    fun rotateImageIfRequired(context: Context, image: Bitmap, selectedImage: Uri): Bitmap {
        val input: InputStream? = context.contentResolver.openInputStream(selectedImage)
        val path = selectedImage.path
        val ei: ExifInterface = when {
            Build.VERSION.SDK_INT > 23 && input != null -> ExifInterface(input)
            path != null -> ExifInterface(path)
            else -> null
        } ?: return image
        return when (ei.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )) {
            ExifInterface.ORIENTATION_ROTATE_90 -> rotateImage(image, 90.0f) ?: image
            ExifInterface.ORIENTATION_ROTATE_180 -> rotateImage(image, 180.0f) ?: image
            ExifInterface.ORIENTATION_ROTATE_270 -> rotateImage(image, 270.0f) ?: image
            else -> image
        }
    }

    fun decodeUriStreamToSize(uri: Uri, width: Int, height: Int, context: Context): Bitmap? {
        var inputStream: InputStream? = null
        try {
            val options: BitmapFactory.Options
            inputStream = context.contentResolver.openInputStream(uri)
            if (inputStream != null) {
                options = BitmapFactory.Options()
                options.inJustDecodeBounds = false
                BitmapFactory.decodeStream(inputStream, null, options)
                inputStream.close()
                inputStream = context.contentResolver.openInputStream(uri)
                if (inputStream != null) {
                    options.inSampleSize =
                        calculateInSampleSize(options.outWidth, options.outHeight, width, height)
                    options.inJustDecodeBounds = false
                    val bitmap = BitmapFactory.decodeStream(inputStream, null, options)
                    inputStream.close()
                    return bitmap
                }
            }
            return null
        } catch (e: Exception) {
            return null
        } finally {
            inputStream?.close()
        }
    }
}