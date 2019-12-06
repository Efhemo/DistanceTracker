package com.efhem.distancetracker

import android.app.Application
import androidx.lifecycle.*
import com.efhem.distancetracker.database.TrackerLocation
import com.efhem.distancetracker.database.database
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LocationViewModel (application: Application) : AndroidViewModel(application) {

    private val database = database(application)
    private val dao = database.daoTracker
    val locations: LiveData<List<TrackerLocation>> = dao.getLocations()

    init {

    }

    fun saveLocation(location: TrackerLocation){
        viewModelScope.launch {
            withContext(Dispatchers.IO){
                dao.insertLocation(location)
            }
        }
    }

    fun getAllLocation(){
        viewModelScope.launch {
            withContext(Dispatchers.IO){
                dao.getLocations()
            }
        }
    }

    fun clearAllTables(){
        viewModelScope.launch {
            withContext(Dispatchers.IO){
                database.clearAllTables()
            }
        }
    }

    class Factory(val app: Application) : ViewModelProvider.Factory {
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(LocationViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return LocationViewModel(app) as T
            }
            throw IllegalArgumentException("Unable to construct viewmodel")
        }
    }
}