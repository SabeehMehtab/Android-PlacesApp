package com.example.happyplaces.adapters

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.happyplaces.R
import com.example.happyplaces.databinding.ItemHappyPlaceCardviewBinding
import com.example.happyplaces.models.HappyPlaceModel

class HappyPlacesAdapter(
    private val context: Context,
    private val placesList : ArrayList<HappyPlaceModel>,
    private val itemOCL : (position: Int, model: HappyPlaceModel) -> Unit) :
    RecyclerView.Adapter<HappyPlacesAdapter.PlaceViewHolder>() {

    class PlaceViewHolder(binding: ItemHappyPlaceCardviewBinding):
        RecyclerView.ViewHolder(binding.root) {
        val image = binding.civ
        val placeName = binding.title
        val placeDesc = binding.description
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaceViewHolder {
        return PlaceViewHolder(ItemHappyPlaceCardviewBinding.inflate(
            LayoutInflater.from(context), parent, false
        ))
    }

    override fun onBindViewHolder(holder: PlaceViewHolder, position: Int) {
        val place = placesList[position]
        holder.placeName.text = place.title
        holder.placeDesc.text = place.description
        if (place.image_uri == "") holder.image.setImageResource(R.mipmap.ic_launcher)
        else holder.image.setImageURI(Uri.parse(place.image_uri))
        holder.itemView.setOnClickListener {
            itemOCL.invoke(position, place)
        }
    }

    override fun getItemCount(): Int {
        return placesList.size
    }

    fun notifyItemUpdateInList(position: Int, happyPlaceModel: HappyPlaceModel) {
        placesList[position] = happyPlaceModel
    }

    fun removeAt(position: Int) {
        placesList.removeAt(position)
        notifyItemRemoved(position)
    }
}