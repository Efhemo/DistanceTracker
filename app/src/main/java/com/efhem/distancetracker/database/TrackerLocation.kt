package com.efhem.distancetracker.database

import androidx.annotation.NonNull
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey

@Entity
data class TrackerLocation @Ignore constructor(
    @NonNull @PrimaryKey(autoGenerate = true) var id: Long = 0,
    var latitude: Double = 0.0,
    var longitude: Double = 0.0){

    constructor():this(id = 0)
}


