package com.efhem.distancetracker.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase


@Database(entities = [TrackerLocation::class], version = 1, exportSchema = false)
//@TypeConverters(TypeConverter::class)
abstract class DistanceTrackerDatabase : RoomDatabase() {
    abstract val daoTracker: DaoTracker
}

private lateinit var INSTANCE: DistanceTrackerDatabase

fun database(context: Context): DistanceTrackerDatabase {
    synchronized(DistanceTrackerDatabase::class.java) {
        if (!::INSTANCE.isInitialized) {
            INSTANCE = Room.databaseBuilder(
                context.applicationContext,
                DistanceTrackerDatabase::class.java, "DistanceTrackerDatabase"
            ).build()
        }
    }
    return INSTANCE
}

