package com.example.happyplaces.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.happyplaces.models.HappyPlaceModel

class DatabaseHandler(context: Context):
    SQLiteOpenHelper(context, DATABASE_NAME,null, DATABASE_VERSION){

    companion object {
        private const val DATABASE_VERSION = 1 // Database version
        private const val DATABASE_NAME = "HappyPlacesDatabase" // Database name
        private const val TABLE_HAPPY_PLACE = "HappyPlacesTable" // Table Name

        //All the Columns names
        private const val ID_KEY = "id"
        private const val TITLE_KEY = "title"
        private const val DESCRIPTION_KEY = "description"
        private const val DATE_KEY = "date"
        private const val LOCATION_KEY = "location"
        private const val LATITUDE_KEY = "latitude"
        private const val LONGITUDE_KEY = "longitude"
        private const val IMAGE_URI_KEY = "image"
    }

    override fun onCreate(db: SQLiteDatabase?) {
        db?.execSQL(
            "CREATE TABLE $TABLE_HAPPY_PLACE ($ID_KEY INTEGER PRIMARY KEY, $TITLE_KEY TEXT, $DESCRIPTION_KEY TEXT, $DATE_KEY TEXT, $LOCATION_KEY TEXT, $LATITUDE_KEY TEXT, $LONGITUDE_KEY TEXT, $IMAGE_URI_KEY TEXT)"
        )
    }

    override fun onUpgrade(db: SQLiteDatabase?, p1: Int, p2: Int) {
        db!!.execSQL("DROP TABLE IF EXISTS $TABLE_HAPPY_PLACE")
        onCreate(db)
    }

    // Function to insert happy place into the database
    fun insertHappyPlace(happyPlaceModel: HappyPlaceModel):Long {
        val db = this.writableDatabase

        val values = ContentValues()
        values.put(TITLE_KEY, happyPlaceModel.title)
        values.put(DESCRIPTION_KEY, happyPlaceModel.description)
        values.put(DATE_KEY, happyPlaceModel.date)
        values.put(LOCATION_KEY, happyPlaceModel.location)
        values.put(LATITUDE_KEY, happyPlaceModel.latitude)
        values.put(LONGITUDE_KEY, happyPlaceModel.longitude)
        values.put(IMAGE_URI_KEY, happyPlaceModel.image_uri)

        val result = db.insert(TABLE_HAPPY_PLACE,null, values)
        db.close()
        return result
    }

    // Function to update happy place via ID in database
    fun updateHappyPlace(happyPlaceModel: HappyPlaceModel): Int {
        val db = this.writableDatabase

        val values = ContentValues()
        values.put(TITLE_KEY, happyPlaceModel.title)
        values.put(DESCRIPTION_KEY, happyPlaceModel.description)
        values.put(DATE_KEY, happyPlaceModel.date)
        values.put(LOCATION_KEY, happyPlaceModel.location)
        values.put(LATITUDE_KEY, happyPlaceModel.latitude)
        values.put(LONGITUDE_KEY, happyPlaceModel.longitude)
        values.put(IMAGE_URI_KEY, happyPlaceModel.image_uri)

        val updatedSuccessfully = db.update(TABLE_HAPPY_PLACE, values,
            ID_KEY + "=" + happyPlaceModel.id , null)
        db.close()
        return updatedSuccessfully
    }

    fun deleteHappyPlace(happyPlaceModel: HappyPlaceModel) : Int{
        val db = this.writableDatabase
        val deleteSuccess = db.delete(TABLE_HAPPY_PLACE,
            "$ID_KEY = ${happyPlaceModel.id}", null)
        db.close()
        return deleteSuccess
    }

    fun getHappyPlacesListFromDatabase(): ArrayList<HappyPlaceModel> {
        val happyPlaceList = ArrayList<HappyPlaceModel> ()
        val db  = this.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_HAPPY_PLACE", null)
        if (cursor.count == 0) {
            return happyPlaceList
        }
        if (cursor.moveToFirst()){
            do {
                val happyPlace = HappyPlaceModel(
                    cursor.getInt(cursor.getColumnIndexOrThrow(ID_KEY)),
                    cursor.getString(cursor.getColumnIndexOrThrow(TITLE_KEY)),
                    cursor.getString(cursor.getColumnIndexOrThrow(DESCRIPTION_KEY)),
                    cursor.getString(cursor.getColumnIndexOrThrow(DATE_KEY)),
                    cursor.getString(cursor.getColumnIndexOrThrow(LOCATION_KEY)),
                    cursor.getDouble(cursor.getColumnIndexOrThrow(LATITUDE_KEY)),
                    cursor.getDouble(cursor.getColumnIndexOrThrow(LONGITUDE_KEY)),
                    cursor.getString(cursor.getColumnIndexOrThrow(IMAGE_URI_KEY))
                )
                happyPlaceList.add(happyPlace)
            } while (cursor.moveToNext())
            cursor.close()
        }
        return happyPlaceList
    }
}