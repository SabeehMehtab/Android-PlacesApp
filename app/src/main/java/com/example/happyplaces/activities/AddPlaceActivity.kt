package com.example.happyplaces.activities

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.DatePickerDialog
import android.content.*
import android.location.LocationManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.os.Looper
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.FileProvider
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.lifecycleScope
import com.example.happyplaces.R
import com.example.happyplaces.database.DatabaseHandler
import com.example.happyplaces.databinding.ActivityAddPlaceBinding
import com.example.happyplaces.models.HappyPlaceModel
import com.example.happyplaces.utils.GetAddressFromLatLng
import com.google.android.gms.location.*
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.karumi.dexter.listener.single.PermissionListener
import com.yalantis.ucrop.UCrop
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*

class AddPlaceActivity : AppCompatActivity(), View.OnClickListener {
    private lateinit var binding: ActivityAddPlaceBinding
    private lateinit var galleryResultLauncher: ActivityResultLauncher<Intent>
    private lateinit var cameraResultLauncher: ActivityResultLauncher<Intent>
    private lateinit var cropResultLauncher: ActivityResultLauncher<Intent>
    private lateinit var googleMapsResultLauncher: ActivityResultLauncher<Intent>
    private lateinit var capturedImageUri: Uri
    private lateinit var capturedImagePath: String
    private lateinit var dateSetListener: DatePickerDialog.OnDateSetListener
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private var calendar = Calendar.getInstance()
    private var latitude: Double = 0.0
    private var longitude: Double = 0.0
    private var imageUri: String = ""
    private var editHappyPlaceModel: HappyPlaceModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddPlaceBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener {
            onBackPressed()
        }
        dateSetListener = DatePickerDialog.OnDateSetListener { datePicker, year, month, dayOfMonth ->
            // you could also use datePicker in the set method for year, month and dayOfMonth
            calendar.set(year,month,dayOfMonth)
            setDateInView()
        }

        setDateInView() // displays current date in the date edit text view

        if(!Places.isInitialized()) {
            Places.initialize(this@AddPlaceActivity,
                resources.getString(R.string.google_maps_key))
        }

        if(intent.hasExtra("Edit_place_details")) {
            editHappyPlaceModel = intent.getParcelableExtra("Edit_place_details")
            supportActionBar?.title = "Edit Happy Place"

            binding.etTitle.setText(editHappyPlaceModel!!.title)
            binding.etDescription.setText(editHappyPlaceModel!!.description)
            binding.etDate.setText(editHappyPlaceModel!!.date)
            binding.etLocation.setText(editHappyPlaceModel!!.location)
            latitude = editHappyPlaceModel!!.latitude
            longitude = editHappyPlaceModel!!.longitude
            if (editHappyPlaceModel!!.image_uri != "") {
                displayImageDelAction()
                binding.ivPlaceImage.setImageURI(Uri.parse(editHappyPlaceModel!!.image_uri))
                imageUri = editHappyPlaceModel!!.image_uri!!
            }
            binding.btnSave.text = "UPDATE"
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        registerActivityForGalleryResult()
        registerActivityForCameraResult()
        registerActivityForCropResult()
        registerActivityForGoogleMapsResult()

        binding.btnSave.setOnClickListener(this)
        binding.etDate.setOnClickListener(this)
        binding.tvAddImage.setOnClickListener(this)
        binding.etLocation.setOnClickListener(this)
        binding.ivCurrentLocation.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        when(v!!.id) {
            R.id.et_date -> {
                DatePickerDialog(
                    this@AddPlaceActivity,
                    dateSetListener,
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get((Calendar.DAY_OF_MONTH))).show()
            }
            R.id.tv_add_image -> {
                showImageDialog()
            }
            R.id.et_location -> {
                if(!isLocationEnabled()) {
                    Toast.makeText(
                        this,
                        "Your location provider is turned off. Please turn it on.",
                        Toast.LENGTH_SHORT
                    ).show()

                    // This will redirect you to settings from where you need to turn on the location provider.
                    val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                    startActivity(intent)
                } else {
                    try {
                        val fields = listOf(
                            Place.Field.ID, Place.Field.NAME,
                            Place.Field.LAT_LNG, Place.Field.ADDRESS)
                        val mapsIntent = Autocomplete.IntentBuilder(
                            AutocompleteActivityMode.FULLSCREEN, fields
                        ).build(this@AddPlaceActivity)
                        googleMapsResultLauncher.launch(mapsIntent)
                    } catch (e:Exception) {
                        e.printStackTrace()
                    }
                }
//                try {
//                    val fields = listOf(
//                        Place.Field.ID, Place.Field.NAME,
//                        Place.Field.LAT_LNG, Place.Field.ADDRESS)
//                    val mapsIntent = Autocomplete.IntentBuilder(
//                        AutocompleteActivityMode.FULLSCREEN, fields
//                    ).build(this@AddPlaceActivity)
//                    googleMapsResultLauncher.launch(mapsIntent)
//                } catch (e:Exception) {
//                    e.printStackTrace()
//                }
            }
            R.id.iv_current_location -> {
                if(!isLocationEnabled()) {
                    Toast.makeText(
                        this,
                        "Your location provider is turned off. Please turn it on.",
                        Toast.LENGTH_SHORT
                    ).show()

                    // This will redirect you to settings from where you need to turn on the location provider.
                    val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                    startActivity(intent)
                } else {
                    // For Getting current location of user please have a look
                    // at below link for better understanding
                    // https://www.androdocs.com/kotlin/getting-current-location-latitude-longitude-in-android-using-kotlin.html
                    checkLocationPermissionGranted()
                }
            }
            R.id.btnDelete -> {
                imageUri = ""
                binding.tvAddImage.visibility = View.VISIBLE
                binding.btnDelete.visibility = View.GONE
                // Set Image View holder to default settings
                binding.ivPlaceImage.updateLayoutParams<ConstraintLayout.LayoutParams>
                {
                    this.horizontalBias = 0f
                    this.width = 370
                    this.height = 370 }
                binding.ivPlaceImage.setImageResource(
                    R.drawable.add_image_placeholder
                )
                binding.ivPlaceImage.rotation = 0F
            }
            R.id.btn_save -> {
                when {
                    binding.etTitle.text.isNullOrEmpty() -> {
                        Toast.makeText(this, "Please enter Title", Toast.LENGTH_SHORT).show()
                    }
                    binding.etDescription.text.isNullOrEmpty() -> {
                        Toast.makeText(this, "Please enter Description", Toast.LENGTH_SHORT).show()
                    }
                    binding.etLocation.text.isNullOrEmpty() -> {
                        Toast.makeText(this, "Please enter Location", Toast.LENGTH_SHORT).show()
                    }
                    imageUri == "" -> {
                        Toast.makeText(this, "Please add an image", Toast.LENGTH_SHORT).show()
                    }
                    editHappyPlaceModel != null -> {
//                        fusedLocationClient.removeLocationUpdates(locationCallBack)
                        val db = DatabaseHandler(this)
                        val happyPlace = HappyPlaceModel(
                            editHappyPlaceModel!!.id,
                            binding.etTitle.text.toString(),
                            binding.etDescription.text.toString(),
                            binding.etDate.text.toString(),
                            binding.etLocation.text.toString(),
                            latitude,
                            longitude,
                            imageUri)
                        val result = db.updateHappyPlace(happyPlace)
                        if (result > 0) {
                            val data = Intent(this, MainActivity::class.java)
                            data.putExtra("Updated_model", happyPlace)
                            setResult(Activity.RESULT_OK, data)
                            finish()
                        }
                    }
                    else -> {
//                        fusedLocationClient.removeLocationUpdates(locationCallBack)
                        val db = DatabaseHandler(this)
                        val happyPlace = HappyPlaceModel(
                            0,
                            binding.etTitle.text.toString(),
                            binding.etDescription.text.toString(),
                            binding.etDate.text.toString(),
                            binding.etLocation.text.toString(),
                            latitude,
                            longitude,
                            imageUri)
                        val result = db.insertHappyPlace(happyPlace)
                        if (result > 0) {
                            Toast.makeText(this,
                                "Place added successfully",
                                Toast.LENGTH_SHORT).show()
                            setResult(Activity.RESULT_OK)
                            finish()
                        }
                    }
                }
            }
        }
    }

    private fun checkLocationPermissionGranted() {
        Dexter.withContext(this)
            .withPermissions(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION)
            .withListener(object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                    if (report!!.areAllPermissionsGranted()) {
                        getLocationData()
                        Toast.makeText(
                            this@AddPlaceActivity,
                            "Location permission is granted. Now you can request for a current location.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                override fun onPermissionRationaleShouldBeShown(
                    permissions: MutableList<PermissionRequest>?,
                    token: PermissionToken?
                ) {
                    showRationaleDialogForPermission("NO LOCATION ACCESS", token = token)
                }
            }).onSameThread().check()
    }

    @SuppressLint("MissingPermission")
    private fun getLocationData() {
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY, 1000)
            .setWaitForAccurateLocation(false)
            .setMaxUpdates(1)
            .build()
        fusedLocationClient.requestLocationUpdates(
            locationRequest, locationCallBack, Looper.myLooper())
    }

    private val locationCallBack = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            latitude = locationResult.lastLocation!!.latitude
            longitude = locationResult.lastLocation!!.longitude
            val addressTask = GetAddressFromLatLng(
                this@AddPlaceActivity, latitude, longitude)
            addressTask.setCustomAddressListener(
                object : GetAddressFromLatLng.AddressListener {
                override fun onAddressFound(address: String) {
                    binding.etLocation.setText(address)

                }
                override fun onError() {
                    Log.e("addr","null")
                }
            })
            lifecycleScope.launch(Dispatchers.IO) {
                addressTask.launchBackgroundProcessForRequest()
            }
        }
    }

    private fun registerActivityForGalleryResult() {
        galleryResultLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()) { result : ActivityResult ->
            if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                val contentUri = result.data!!.data!!
                var file = getDir("HappyPlacesImages", ContextWrapper.MODE_PRIVATE)
                file = File(file,"${UUID.randomUUID()}.jpg")
                val cropIntent = UCrop.of(contentUri,Uri.fromFile(file))
                    .withOptions(UCrop.Options())
                    .withMaxResultSize(1280,720)
                    .getIntent(this)
                cropResultLauncher.launch(cropIntent)
            }
        }
    }

    private fun registerActivityForCameraResult() {
        cameraResultLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()) { result : ActivityResult ->
            // result.data in not needed as it only provides the thumbnail for the image taken.
            // Instead, a uri is provided, when starting the camera activity, via the intent
            // to allow image taken by user to be stored in external/shared memory.
            if (result.resultCode == Activity.RESULT_OK) { //&& result.data != null) {
                var file = getDir("HappyPlacesImages", ContextWrapper.MODE_PRIVATE)
                file = File(file,"${UUID.randomUUID()}.jpg")
                val cropIntent = UCrop.of(capturedImageUri,Uri.fromFile(file))
                    .withOptions(UCrop.Options())
                    .withMaxResultSize(1280,720)
                    .getIntent(this)
                cropResultLauncher.launch(cropIntent)
            }
        }
    }

    private fun registerActivityForCropResult() {
        cropResultLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()){ result: ActivityResult ->
            if (result.resultCode == Activity.RESULT_OK && result.data != null){
                val croppedImageUri = UCrop.getOutput(result.data!!)
                imageUri = croppedImageUri.toString()
//                binding.etLocation.text = Editable.Factory.getInstance()
//                    .newEditable(""+croppedImageUri)
                try {
                    binding.ivPlaceImage.setImageURI(croppedImageUri)
                    displayImageDelAction()
                } catch (e: IOException) {
                    e.printStackTrace()
                    Toast.makeText(this,
                        "Failed to load image",
                        Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun registerActivityForGoogleMapsResult() {
        googleMapsResultLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()) {
            if(it.resultCode == Activity.RESULT_OK) {
                val place: Place = Autocomplete.getPlaceFromIntent(it.data!!)
                binding.etLocation.setText(place.address)
                latitude = place.latLng!!.latitude
                longitude = place.latLng!!.longitude
            }
        }
    }

    private fun showImageDialog() {
        val dialog = AlertDialog.Builder(this@AddPlaceActivity).setTitle("SELECT ACTION")
            .setItems(
                arrayOf("SELECT IMAGE FROM GALLERY",
                    "TAKE PHOTO FROM CAMERA")) { _ , id ->
                if(id == 0) {
                    checkGalleryPermissionGranted()
                } else {
                    checkCameraPermissionGranted()
                }
            }.create()
        dialog.show()
    }

    private fun checkGalleryPermissionGranted() {
        Dexter.withContext(this@AddPlaceActivity).withPermissions(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE)
            .withListener(object : MultiplePermissionsListener {
                // The MultiplePermissionsReport contains all the details
                // of the permission request like the list of denied/granted permissions
                // or utility methods like areAllPermissionsGranted
                // and isAnyPermissionPermanentlyDenied in the report.
                override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                    // will be called after the user has granted the permission.
                    if (report!!.areAllPermissionsGranted()) {
                        val galleryIntent = Intent(Intent.ACTION_PICK,
                            MediaStore.Images.Media.INTERNAL_CONTENT_URI)
                        galleryResultLauncher.launch(galleryIntent)
                    }
                    //if any of them are permanently disabled(i.e. "deny and don't ask again" etc..)
                    else if (report.isAnyPermissionPermanentlyDenied) {
                        showRationaleDialogForPermission("NO GALLERY ACCESS", null)
                    }
                    //if it's a simple "deny" from the user
                    else {
                        Toast.makeText(this@AddPlaceActivity,
                            "This permission is required to access your gallery",
                            Toast.LENGTH_LONG).show()
                    }
                }
                // Rationale dialog to be shown when user denies permission
                override fun onPermissionRationaleShouldBeShown(
                    p0: MutableList<PermissionRequest>?, token: PermissionToken?
                ) {
                    showRationaleDialogForPermission("NO GALLERY ACCESS", token = token)
                }
            }).onSameThread().check()
    }

    private fun checkCameraPermissionGranted() {
        Dexter.withContext(this@AddPlaceActivity).withPermission(
            Manifest.permission.CAMERA)
            .withListener(object: PermissionListener{
                override fun onPermissionGranted(response: PermissionGrantedResponse?) {
                    val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                    uriForPictureTaken()
                    cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, capturedImageUri)
                    cameraResultLauncher.launch(cameraIntent)
                }

                override fun onPermissionDenied(p0: PermissionDeniedResponse?) {
                    Toast.makeText(this@AddPlaceActivity,
                        "Permission is required to access your camera module",
                        Toast.LENGTH_LONG).show()
                }

                override fun onPermissionRationaleShouldBeShown(p0: PermissionRequest?,
                                                                token: PermissionToken?) {
                    showRationaleDialogForPermission("NO CAMERA ACCESS", token = token)
                }
            }).onSameThread().check()
    }

    private fun showRationaleDialogForPermission(title: String, token: PermissionToken?) {
        val alertMessage = AlertDialog.Builder(this@AddPlaceActivity)
            .setTitle(title)
            .setMessage("It looks like you have denied " +
                    "permission access required for this feature. " +
                    "It can be enabled under the Application Settings")
            .setPositiveButton("GO TO SETTINGS") { _, _ ->
                try {
                    // takes the user to the application's permission settings
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    val uri = Uri.fromParts("package",packageName,null)
                    intent.data = uri
                    startActivity(intent)
                } catch (e: ActivityNotFoundException){
                    e.printStackTrace()
                }
            }.setNegativeButton("CANCEL") {dialogInterface, _ ->
                dialogInterface.dismiss()
                token?.cancelPermissionRequest()
            }.setCancelable(false)
        alertMessage.show()
    }

    private fun uriForPictureTaken() {
        // getExternalFilesDir refers to External/Shared storage
        // which can be checked on computer/laptop via usb cable
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val imageName = "jpg_"+ SimpleDateFormat("yyyyMMdd_HHmmss",
            Locale.getDefault()).format(calendar.time)
        val imageFile = File.createTempFile(imageName,".jpg",storageDir)
        capturedImagePath = imageFile.absolutePath
        capturedImageUri = FileProvider.getUriForFile(this,
            "com.example.happyplaces.fileprovider",imageFile)
    }

    private fun isLocationEnabled(): Boolean {
        val locationManager: LocationManager =
            getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER
        )
    }

    private fun displayImageDelAction(){
        binding.ivPlaceImage.updateLayoutParams<ConstraintLayout.LayoutParams> {
            this.horizontalBias = 0.5f
            this.width = ViewGroup.LayoutParams.WRAP_CONTENT
            this.height = ViewGroup.LayoutParams.WRAP_CONTENT
        }
        binding.tvAddImage.visibility = View.GONE
        binding.btnDelete.visibility = View.VISIBLE
        binding.btnDelete.setOnClickListener(this)
    }

    private fun setDateInView() {
        val date = SimpleDateFormat(
            "dd MMM yyyy",
            Locale.getDefault()).format(calendar.time)
        binding.etDate.setText(date.toString())
    }
}


//private fun saveImageToGetUri(image: Bitmap): Uri {
//        In order to preserve the quality of bitmap apply the following code
//        val qualityImage = WeakReference<Bitmap>(Bitmap.createScaledBitmap(
//            image,image.width,image.height,false).copy(
//            Bitmap.Config.RGB_565,true)).get()
//    var file = getDir("HappyPlacesImages", ContextWrapper.MODE_PRIVATE)
//    file = File(file,"${UUID.randomUUID()}.jpg")
//    try {
//        // Get the file output stream
//        val stream = FileOutputStream(file)
//
//        // Compress bitmap
//        // qualityImage!!.compress(Bitmap.CompressFormat.JPEG, 100, stream)
//
//        // Flush the stream
//        stream.flush()
//
//        // Close stream
//        stream.close()
//    } catch (e: IOException) { // Catch the exception
//        e.printStackTrace()
//    }
//
//    return Uri.fromFile(file)
//}
