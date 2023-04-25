package com.example.happyplaces.activities

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.happyplaces.adapters.HappyPlacesAdapter
import com.example.happyplaces.database.DatabaseHandler
import com.example.happyplaces.databinding.ActivityMainBinding
import com.example.happyplaces.models.HappyPlaceModel
import com.example.happyplaces.utils.SwipeToDeleteCallback
import com.example.happyplaces.utils.SwipeToEditCallback
import java.text.FieldPosition

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var addPlaceResultLauncher: ActivityResultLauncher<Intent>
    private lateinit var editPlaceResultLauncher: ActivityResultLauncher<Intent>
    private lateinit var happyPlacesList: ArrayList<HappyPlaceModel>
    private var position: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        addPlaceResultLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()) {
            if(it.resultCode == Activity.RESULT_OK) {
                getHappyPlacesFromLocalDB()
            }
        }

        editPlaceResultLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()) {
            if(it.resultCode == Activity.RESULT_OK) {
                val model = it.data!!.getParcelableExtra<HappyPlaceModel>("Updated_model")
                val adapter = binding.rvHappyPlacesList.adapter as HappyPlacesAdapter
                adapter.notifyItemUpdateInList(position!!, model!!)
                adapter.notifyItemChanged(position!!)
            }
        }
        binding.fabAdd.setOnClickListener {
            val intent = Intent(this, AddPlaceActivity::class.java)
            addPlaceResultLauncher.launch(intent)
        }
        getHappyPlacesFromLocalDB()
    }

    private fun getHappyPlacesFromLocalDB() {
        val db = DatabaseHandler(this)
        happyPlacesList = db.getHappyPlacesListFromDatabase()
        if (happyPlacesList.size > 0) {
            binding.tvDefault.visibility = View.GONE
            binding.rvHappyPlacesList.visibility = View.VISIBLE
            setHappyPlacesOnScreen(happyPlacesList)
        } else {
            binding.tvDefault.visibility = View.VISIBLE
            binding.rvHappyPlacesList.visibility = View.GONE
        }
    }

    private fun setHappyPlacesOnScreen(placesList: ArrayList<HappyPlaceModel>) {
        binding.rvHappyPlacesList.layoutManager = LinearLayoutManager(this)
        val adapter = HappyPlacesAdapter(this, placesList) { position: Int, model: HappyPlaceModel ->
            val intent = Intent(this, HappyPlaceDetailActivity::class.java)
            intent.putExtra("Place_data_model", model)
            startActivity(intent)
        }
        binding.rvHappyPlacesList.adapter = adapter

        val editSwipeHandler =  object : SwipeToEditCallback(this) {
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val editIntent = Intent(this@MainActivity, AddPlaceActivity::class.java)
                position = viewHolder.adapterPosition
                editIntent.putExtra("Edit_place_details", happyPlacesList[position!!])
                editPlaceResultLauncher.launch(editIntent)
                binding.rvHappyPlacesList.adapter!!.notifyItemChanged(position!!)
            }
        }
        ItemTouchHelper(editSwipeHandler).attachToRecyclerView(binding.rvHappyPlacesList)

        val deleteSwipeHandler =  object : SwipeToDeleteCallback(this) {
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val db = DatabaseHandler(this@MainActivity)
                position = viewHolder.adapterPosition
                val result = db.deleteHappyPlace(happyPlacesList[position!!])
                if (result > 0) {
                    (binding.rvHappyPlacesList.adapter as HappyPlacesAdapter).removeAt(position!!)
                    happyPlacesList = db.getHappyPlacesListFromDatabase()
                    if (happyPlacesList.size == 0) {
                        binding.tvDefault.visibility = View.VISIBLE
                        binding.rvHappyPlacesList.visibility = View.GONE
                    }
                }
            }
        }
        ItemTouchHelper(deleteSwipeHandler).attachToRecyclerView(binding.rvHappyPlacesList)
    }
}