package com.example.lab_week_07

import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.content.pm.PackageManager
import android.health.connect.datatypes.ExerciseRoute
import android.location.Location
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat.requestLocationUpdates

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.example.lab_week_07.databinding.ActivityMapsBinding
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>


    private val fusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(this)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        requestPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) {
                    isGranted -> if (isGranted) {
                //If granted by the user, execute the necessary function
                getLastLocation()
            } else {
                //If not granted, show a rationale dialog
                //A rationale dialog is used for a warning to the user that the app will
//                now work without the required permission
                showPermissionRationale {
                    requestPermissionLauncher.launch(ACCESS_FINE_LOCATION)
                }
            }
            }
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
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        if (hasLocationPermission()) {
            mMap.isMyLocationEnabled = true  // Muncul tombol lokasi biru
            getLastLocation()
        } else {
            when {
                shouldShowRequestPermissionRationale(ACCESS_FINE_LOCATION) -> {
                    showPermissionRationale { requestPermissionLauncher.launch(ACCESS_FINE_LOCATION) }
                }
                else -> requestPermissionLauncher.launch(ACCESS_FINE_LOCATION)
            }
        }
    }
    //This is used to bring up a rationale dialog which will be used to ask the user
//    for permission again
//A rationale dialog is used for a warning to the user that the app will now work
//    without the required permission
//Usually it's brought up when the user denies the needed permission in the
//    previous permission request

    //This is used to check if the user already has the permission granted
    private fun hasLocationPermission() =
        ContextCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
    private fun showPermissionRationale(positiveAction: () -> Unit) {
        //Create a pop up alert dialog that's used to ask for the required permission again to the user
        AlertDialog.Builder(this)
            .setTitle("Location permission")
            .setMessage("This app will not work without knowing your current location")
            .setPositiveButton(android.R.string.ok) { _, _ -> positiveAction() }
            .setNegativeButton(android.R.string.cancel) { dialog, _ -> dialog.dismiss()
            }
            .create().show()
    }
    //Executed when the location permission has been granted by the user
    private fun getLastLocation() {
        if (hasLocationPermission()) {
            try {
                fusedLocationProviderClient.lastLocation
                    .addOnSuccessListener { location: Location? ->
                        if (location != null) {
                            val userLocation = LatLng(location.latitude, location.longitude)
                            updateMapLocation(userLocation)
                            addMarkerAtLocation(userLocation, "You")
                        } else {
                            // Jika lastLocation null, minta update aktif
                            requestLocationUpdates()
                        }
                    }
            } catch (e: SecurityException) {
                Log.e("MapsActivity", "SecurityException: ${e.message}")
            }
        } else {
            requestPermissionLauncher.launch(ACCESS_FINE_LOCATION)
        }
    }

    private fun requestLocationUpdates() {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
            .setWaitForAccurateLocation(false)
            .setMinUpdateIntervalMillis(3000)
            .build()

        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    val userLocation = LatLng(location.latitude, location.longitude)
                    updateMapLocation(userLocation)
                    addMarkerAtLocation(userLocation, "You")

                    // Hentikan update setelah dapat lokasi pertama
                    fusedLocationProviderClient.removeLocationUpdates(this)
                }
            }
        }

        try {
            fusedLocationProviderClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            Log.e("MapsActivity", "Permission not granted for location updates")
        }
    }

    private fun updateMapLocation(location: LatLng) {
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
            location, 7f))
    }

    private fun addMarkerAtLocation(location: LatLng, title: String) {
        mMap.addMarker(MarkerOptions().title(title)
            .position(location))
    }
}