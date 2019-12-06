package com.efhem.distancetracker

import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface.BOLD_ITALIC
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.res.ResourcesCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import com.efhem.distancetracker.database.TrackerLocation
import com.efhem.distancetracker.databinding.ActivityMapsBinding
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import es.dmoral.toasty.Toasty
import kotlinx.android.synthetic.main.activity_maps.*


class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var map: GoogleMap
    private lateinit var bind: ActivityMapsBinding

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var lastLocation: Location
    //Receiving Location Updates
    private lateinit var locationCallback: LocationCallback
    private lateinit var locationRequest: LocationRequest
    private var locationUpdateState = false
    private var oldLocation: Location = Location(LocationManager.GPS_PROVIDER)
    var distanceCovered: Float = 0.toFloat()
    var time = "00:00"


    private val viewModel: LocationViewModel by lazy {
        ViewModelProvider(this, LocationViewModel.Factory(this.application))
            .get(LocationViewModel::class.java)
    }


    var startTime: Long = 0
    //runs without a timer by reposting this handler at the end of the runnable
    var timerHandler: Handler = Handler()
    private var timerRunnable: Runnable = object : Runnable {
        override fun run() {
            val millis = System.currentTimeMillis() - startTime
            var seconds = (millis / 1000).toInt()
            val minutes = seconds / 60
            seconds %= 60

            time = String.format("%d:%02d", minutes, seconds)
            val ss1 = SpannableString(time)
            ss1.setSpan(RelativeSizeSpan(2f), 0, 4, 0) // set size
            ss1.setSpan(ForegroundColorSpan(Color.RED), 0, 4, 0) //
            ss1.setSpan( StyleSpan(BOLD_ITALIC), 0, 4, 0)

            Toasty.normal(applicationContext, ss1 , ResourcesCompat.getDrawable(
                resources, R.drawable.ic_timer_black_24dp, null)).show()
            //timerTextView.setText(String.format("%d:%02d", minutes, seconds))
            timerHandler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //setContentView(R.layout.activity_maps)

        bind = DataBindingUtil.setContentView(this, R.layout.activity_maps )

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        indicateStart(bind)
        fab.text = getString(R.string.start)
        bind.fab.setOnClickListener {
            if (fab.text == getString(R.string.stop)) {
                stopTracking()
            } else {
                startTime = System.currentTimeMillis()
                startTracking()
                //indicateStop(bind)
            }
        }

        /**
         * Gives Location Update callback
         * */
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(p0: LocationResult) {
                super.onLocationResult(p0)

                lastLocation = p0.lastLocation
                Toast.makeText(applicationContext,"location is ${lastLocation.latitude} and ${lastLocation.longitude}", Toast.LENGTH_LONG ).show()
                val trackedLocation = TrackerLocation()
                trackedLocation.latitude = lastLocation.latitude
                trackedLocation.longitude = lastLocation.longitude
                viewModel.saveLocation(trackedLocation)

                map.moveCamera(CameraUpdateFactory.newLatLng(LatLng(lastLocation.latitude, lastLocation.longitude)))

                //Calculate covered DIstance
                val distance: Float = oldLocation.distanceTo(lastLocation)
                distanceCovered = distanceCovered.plus(distance)
                Log.d("MapsActivity", "location is ${lastLocation.latitude} and ${lastLocation.longitude} distace $distanceCovered")
                oldLocation = lastLocation
            }
        }
        //setUpLocationRequest()

    }

    private fun indicateStart(bind: ActivityMapsBinding) {
        bind.fab.text = resources.getString(R.string.start)
        bind.fab.icon = ResourcesCompat.getDrawable(
            resources, R.drawable.ic_transfer_within_a_station_black_24dp, null
        )
    }
    private fun indicateStop(bind: ActivityMapsBinding) {
        bind.fab.text = resources.getString(R.string.stop)
        bind.fab.icon = ResourcesCompat.getDrawable(
            resources, R.drawable.ic_stop, null
        )
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap

        //enable the zoom in/zoom out interface on the map
        map.uiSettings.isZoomControlsEnabled = true

        requestAccessUserLocation(this)

        usersLocation()

    }


    private fun setUpLocationRequest() {
        locationRequest = LocationRequest()
        locationRequest.interval = 10000
        locationRequest.fastestInterval = 5000
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY

        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)

        //Before you start requesting for location updates,
        //you need to check the state of the user’s location settings.
        val client = LocationServices.getSettingsClient(this)
        val task = client.checkLocationSettings(builder.build())

        task.addOnSuccessListener {
            locationUpdateState = true
            startLocationUpdates()
        }

        task.addOnFailureListener { e ->
            if (e is ResolvableApiException) {
                // Location settings are not satisfied, but this can be fixed
                // by showing the user a dialog.
                try {
                    // Show the dialog by calling startResolutionForResult(),
                    // and check the result in onActivityResult().
                    e.startResolutionForResult(
                        this,
                        REQUEST_CHECK_SETTINGS
                    )
                } catch (sendEx: IntentSender.SendIntentException) {
                    // Ignore the error.
                }
            }
        }
    }


    /**
     * Check if ACCESS_FINE_LOCATION is granted if not request it
     * */
    private fun requestAccessUserLocation(activity: MapsActivity) {
        if (ActivityCompat.checkSelfPermission(
                this.applicationContext,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
            return
        }
    }

    //Receiving Location Updates: From here to onResume + onCreate method
    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
            return
        }
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            null /* Looper */
        )
    }

    private fun usersLocation() {
        if (ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            //set my location enabled and draw a dot indication of my current location
            map.isMyLocationEnabled = true
            //The Android Maps API provides different map types: MAP_TYPE_NORMAL, MAP_TYPE_SATELLITE, MAP_TYPE_TERRAIN, MAP_TYPE_HYBRID
            map.mapType = GoogleMap.MAP_TYPE_TERRAIN

            //fusedLocationClient.lastLocation gives the most recent location currently available
            fusedLocationClient.lastLocation.addOnSuccessListener(this) { location ->
                // Got last known location. In some rare situations this can be null.
                location?.let {
                    lastLocation = it
                    oldLocation.latitude = it.latitude
                    oldLocation.longitude = it.longitude
                    Log.d("MapsActivity", "lastLocation is ${it.latitude} and ${it.longitude}")
                    //Animates the movement of the camera from the current position to the position defined in the update
                    map.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(it.latitude, it.longitude), 16f))
                }
            }

        }

    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CHECK_SETTINGS) {
            if (resultCode == Activity.RESULT_OK) {
                locationUpdateState = true
                startLocationUpdates()
            }
        }
    }

    fun startTracking(){

        //start location update
        //start inserting database
        //start timer
        //update button

        distanceCovered = 0.toFloat()
        setUpLocationRequest()
        timerHandler.postDelayed(timerRunnable, 0)
        indicateStop(bind)

    }

    private fun stopTracking(){
        //distanceCovered = 0.toFloat()
        //stop inserting database
        //stop location update
        //stop timer
        //update button
        //calculate distance covered and
        // show user result on bottom dialog
        //delete all data in database

        fusedLocationClient.removeLocationUpdates(locationCallback)
        timerHandler.removeCallbacks(timerRunnable)
        Log.d("Distaneq","dis "+ distanceCovered)

        val bottom: DistanceBottomSheetDialog? = DistanceBottomSheetDialog.newInstance(distanceCovered, time )
        bottom?.show(supportFragmentManager.beginTransaction(), "dialog_playback")
        viewModel.clearAllTables()

    }

    companion object {

        private const val LOCATION_PERMISSION_REQUEST_CODE = 1

        private const val REQUEST_CHECK_SETTINGS = 2

    }

    override fun onPause() {
        super.onPause()
        Toast.makeText(this, "Tracking stops when App not focus", Toast.LENGTH_LONG).show()
        stopTracking()
    }

}
