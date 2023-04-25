package com.example.happyplaces.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.happyplaces.R
import com.example.happyplaces.databinding.ActivityMapBinding
import com.example.happyplaces.models.HappyPlaceModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

class MapActivity : AppCompatActivity(), OnMapReadyCallback {
    private lateinit var binding: ActivityMapBinding
    private var happyPlace : HappyPlaceModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if(intent.hasExtra("Happy_place_details")){
            happyPlace = intent.getParcelableExtra("Happy_place_details")
        }

        if(happyPlace != null) {
            setSupportActionBar(binding.toolbarMap)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            supportActionBar?.title = happyPlace!!.title
            binding.toolbarMap.setNavigationOnClickListener {
                onBackPressed()
            }
            val supportMapFragment = supportFragmentManager.findFragmentById(
                R.id.map) as SupportMapFragment
            supportMapFragment.getMapAsync(this)
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
    override fun onMapReady(googleMap: GoogleMap?) {
        val placePosition = LatLng(happyPlace!!.latitude, happyPlace!!.longitude)
        googleMap!!.addMarker(MarkerOptions()
            .position(placePosition)
            .title(happyPlace!!.location)
        )
        val newLatLngZoom = CameraUpdateFactory.newLatLngZoom(placePosition,12f)
        googleMap.animateCamera(newLatLngZoom)
    }
}