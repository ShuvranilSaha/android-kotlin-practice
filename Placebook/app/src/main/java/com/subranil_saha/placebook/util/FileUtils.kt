package com.subranil_saha.placebook.util

import android.content.Context
import java.io.File

object FileUtils {
    fun deleteFile(context: Context, fileName: String) {
        val dir = context.filesDir
        val file = File(dir, fileName)
        file.delete()
    }
}