package com.subranil_saha.podplay.util

import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

object DateUtils {
    fun jsonToShortDate(jsonDate: String?): String {
        if (jsonDate == null) {
            return "-"
        }
        val inFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        val date = inFormat.parse(jsonDate) ?: return "-"
        val outputFormat = DateFormat.getInstance()
        return outputFormat.format(date)
    }
}