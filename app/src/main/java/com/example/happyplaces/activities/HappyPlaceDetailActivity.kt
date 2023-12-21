package com.example.happyplaces.activities

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.happyplaces.databinding.ActivityHappyPlaceDetailBinding
import com.example.happyplaces.models.HappyPlaceModel

class HappyPlaceDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHappyPlaceDetailBinding
    private var happyPlaceModel: HappyPlaceModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHappyPlaceDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener {
            onBackPressed()
        }

        if(intent.hasExtra("Place_data_model")){
            happyPlaceModel = intent.getParcelableExtra("Place_data_model")
        }

        if (happyPlaceModel != null) {
            binding.Name.text = happyPlaceModel!!.title
            binding.image.setImageURI(Uri.parse(happyPlaceModel!!.image_uri))
            binding.location.text = happyPlaceModel!!.location
            binding.description.text = happyPlaceModel!!.description
            binding.date.text = happyPlaceModel!!.date
        }

        binding.btnViewOnMap.setOnClickListener {
            val mapIntent = Intent(this, MapActivity::class.java)
            mapIntent.putExtra("Happy_place_details", happyPlaceModel)
            startActivity(mapIntent)
        }
    }
}