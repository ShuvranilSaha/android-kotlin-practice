package com.subranil_saha.podplay.db

import android.content.Context
import androidx.room.*
import com.subranil_saha.podplay.model.Episode
import com.subranil_saha.podplay.model.Podcast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.internal.synchronized
import java.util.Date

class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return if (value == null) null else Date(value)
    }

    @TypeConverter
    fun toTimestamp(date: Date?): Long? {
        return (date?.time)
    }
}

@Database(entities = [Podcast::class, Episode::class], version = 1)
@TypeConverters(Converters::class)
abstract class PodPlayDatabase : RoomDatabase() {
    abstract fun podcastDao(): PodcastDao

    companion object {
        @Volatile
        private var INSTANCE: PodPlayDatabase? = null

        @InternalCoroutinesApi
        fun getInstance(context: Context, coroutineScope: CoroutineScope): PodPlayDatabase {
            val tempInstance = INSTANCE
            if (tempInstance != null) {
                return tempInstance
            }
            synchronized(this) {
                val instance =
                    Room.databaseBuilder(
                        context.applicationContext,
                        PodPlayDatabase::class.java,
                        "PodPlayer"
                    )
                        .build()
                INSTANCE = instance
                return instance
            }
        }
    }
}