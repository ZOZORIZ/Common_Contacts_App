package com.example.camera

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.request.RequestOptions
import com.example.camera.databinding.ActivityAddContactBinding
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.i18n.phonenumbers.NumberParseException
import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.yalantis.ucrop.UCrop
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import java.io.File
import java.net.SocketTimeoutException
import java.util.UUID
import okhttp3.MediaType.Companion.toMediaTypeOrNull



class AddContactActivity : AppCompatActivity() {

    private lateinit var backButton: ImageButton
    private var existingContact: Contact? = null
    private lateinit var binding: ActivityAddContactBinding
    private var imageUri: Uri? = null
    private val viewModel: ContactViewModel by viewModels {
        ContactViewModelFactory((application as MyApplication).contactRepository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddContactBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // return to previous activity
        backButton = findViewById<ImageButton>(R.id.back_button)
        binding.backButton.setOnClickListener {
            onBackPressed()
        }

        existingContact = intent.getParcelableExtra("contact")
        if (existingContact != null) {
            // Update the header text to "Edit Contact"
            binding.header.text = "Edit Contact"
            binding.textBelowImage.text = "Click To Update Picture"

            existingContact?.let {
                viewModel.getContactById(it.id).observe(this) { contact ->
                    contact?.let { preFillContactDetails(it) }
                }
            }
        } else {
            binding.header.text = "Add New Contact"
            binding.textBelowImage.text = "Click To Add Picture"
        }

        binding.cardview.setOnClickListener {
            showImageSourceDialog()
        }

        binding.saveContactButton.setOnClickListener {
            imageUri?.let { uri ->
                saveContact(uri)
            } ?: Toast.makeText(this, "Please select an image", Toast.LENGTH_SHORT).show()
        }
    }

    private fun preFillContactDetails(contact: Contact) {
        binding.nameInput.setText(contact.name)
        val fullPhoneNumber = contact.phoneNumber ?: ""
        val phoneUtil = PhoneNumberUtil.getInstance()
        try {
            val parsedNumber = phoneUtil.parse(fullPhoneNumber, "IN")
            binding.countryCodeInput.setText("+${parsedNumber.countryCode}")
            binding.phoneInput.setText(parsedNumber.nationalNumber.toString())
        } catch (e: NumberParseException) {
            e.printStackTrace()
            binding.countryCodeInput.setText("")
            binding.phoneInput.setText(fullPhoneNumber)
        }

        binding.phoneInput2.setText(contact.phoneNumber2)
        binding.labelInput.setText(contact.label)
        binding.emailInput.setText(contact.email)
        binding.birthdayInput.setText(contact.birthday)
        contact.imageUrl?.let {
            loadImage(Uri.parse(it), binding.contactImageView)
            binding.contactImageView.visibility = View.VISIBLE
            binding.placeholderImageView.visibility = View.GONE
        }
    }

    private fun saveContact(imageUri: Uri) {
        val name = binding.nameInput.text.toString()
        val countryCode = binding.countryCodeInput.text.toString()
        val phone = binding.phoneInput.text.toString()
        val fullPhoneNumber = "$countryCode $phone"
        val phoneNumber2 = binding.phoneInput2.text.toString()
        val email = binding.emailInput.text.toString()
        val label = binding.labelInput.text.toString()
        val birthday = binding.birthdayInput.text.toString()

        if (name.isNotEmpty() && phone.isNotEmpty()) {
            binding.saveContactButton.isEnabled = false
            val savingDialog = showSavingDialog()

            lifecycleScope.launch {
                val uploadedImageUrl = withContext(Dispatchers.IO) {
                    uploadImageToFirebase(imageUri)
                }
                if (uploadedImageUrl.isNotEmpty()) {
                    val contact = Contact(
                        id = existingContact?.id ?: UUID.randomUUID().toString(),
                        name = name,
                        phoneNumber = fullPhoneNumber,
                        imageUrl = uploadedImageUrl,
                        phoneNumber2 = phoneNumber2,
                        email = email,
                        label = label,
                        birthday = birthday
                    )

                    saveContactToFirestore(contact)
                    savingDialog.dismiss()
                } else {
                    Toast.makeText(this@AddContactActivity, "Failed to upload image", Toast.LENGTH_SHORT).show()
                    savingDialog.dismiss()
                    binding.saveContactButton.isEnabled = true
                }
            }
        } else {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showSavingDialog(): AlertDialog {
        val dialogView = layoutInflater.inflate(R.layout.loading_screen, null)
        val builder = AlertDialog.Builder(this, R.style.TransparentDialog)
            .setView(dialogView)
            .setCancelable(false)

        val dialog = builder.create()
        dialog.show()

        return dialog
    }

    private suspend fun uploadImageToFirebase(imageUri: Uri): String {
        val storageRef = FirebaseStorage.getInstance().reference
        val imageRef = storageRef.child("images/${UUID.randomUUID()}.jpg")

        return try {
            val uploadTask = imageRef.putFile(imageUri).await()
            imageRef.downloadUrl.await().toString()
        } catch (e: Exception) {
            ""
        }
    }

    private fun saveContactToFirestore(contact: Contact) {
        val db = FirebaseFirestore.getInstance()
        db.collection("contacts").document(contact.id).set(contact)
            .addOnSuccessListener {
                viewModel.insertContacts(listOf(contact))
                Toast.makeText(this@AddContactActivity, "Contact saved successfully", Toast.LENGTH_SHORT).show()
                setResult(Activity.RESULT_OK, Intent().apply { putExtra("contact", contact) })
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this@AddContactActivity, "Error saving contact: ${e.message}", Toast.LENGTH_SHORT).show()
                binding.saveContactButton.isEnabled = true
            }
    }

    private fun loadImage(imageUri: Uri?, imageView: ImageView) {
        if (imageUri != null) {
            Glide.with(this)
                .load(imageUri)
                .apply(
                    RequestOptions()
                        .override(50,50)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .transform(CircleCrop())
                )
                .into(imageView)
        } else {
            imageView.setImageResource(R.drawable.placeholder_image)
        }
    }

    private fun showImageSourceDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_image_source, null)
        val dialogBuilder = AlertDialog.Builder(this, R.style.TransparentDialog)
            .setView(dialogView)

        val alertDialog = dialogBuilder.create()

        val takePhotoButton: Button = dialogView.findViewById(R.id.takePhotoButton)
        val galleryButton: Button = dialogView.findViewById(R.id.galleryButton)

        takePhotoButton.setOnClickListener {
            alertDialog.dismiss()
            startCamera()
        }

        galleryButton.setOnClickListener {
            alertDialog.dismiss()
            openGallery()
        }

        alertDialog.show()

        alertDialog.window?.apply {
            val params = attributes
            params?.width = (resources.displayMetrics.widthPixels * 0.9).toInt()
            attributes = params
        }
    }

    private fun startCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.resolveActivity(packageManager)?.let {
            val imageFile = createImageFile()
            imageFile?.let {
                val imageUri = FileProvider.getUriForFile(
                    this,
                    "com.example.camera.fileprovider",
                    it
                )
                this.imageUri = imageUri
                intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
                startActivityForResult(intent, REQUEST_CODE_CAMERA)
            }
        }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, REQUEST_CODE_GALLERY)
    }

    private fun createImageFile(): File? {
        return try {
            val storageDir = getExternalFilesDir(null)
            File.createTempFile(UUID.randomUUID().toString(), ".jpg", storageDir)
        } catch (ex: Exception) {
            Toast.makeText(this, "Error creating image file", Toast.LENGTH_SHORT).show()
            null
        }
    }

    private fun performCrop(imageUri: Uri) {
        val destinationUri = Uri.fromFile(File(cacheDir, "croppedImage_${UUID.randomUUID()}.jpg"))
        val options = UCrop.Options().apply {
            setCompressionQuality(80)
            setHideBottomControls(true)
            setFreeStyleCropEnabled(true)
            setCircleDimmedLayer(true)
        }

        UCrop.of(imageUri, destinationUri)
            .withAspectRatio(1f, 1f)
            .withOptions(options)
            .start(this)
    }

    private fun handleCropResult(resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK && data != null) {
            val resultUri = UCrop.getOutput(data)
            resultUri?.let { uri ->
                removeImageBackground(uri)
            }
        }
    }

    private fun removeImageBackground(imageUri: Uri) {
        val apiKey = "3a3bsxDjRFBBvB6VX2HEX7mL"
        val file = File(imageUri.path)

        val retrofit = RetrofitClient.instance
        val service = retrofit.create(RemoveBgService::class.java)

        val requestFile = RequestBody.create("image/*".toMediaTypeOrNull(), file)
        val body = MultipartBody.Part.createFormData("image_file", file.name, requestFile)

        val call = service.removeBackground(apiKey, body)
        call.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    response.body()?.let { responseBody ->
                        lifecycleScope.launch {
                            val croppedImageUri = withContext(Dispatchers.IO) {
                                saveCroppedImage(responseBody.bytes())
                            }
                            croppedImageUri?.let {
                                loadImage(it, binding.contactImageView)
                                this@AddContactActivity.imageUri = it
                                binding.contactImageView.visibility = View.VISIBLE
                                binding.placeholderImageView.visibility = View.GONE
                            } ?: run {
                                Toast.makeText(this@AddContactActivity, "Failed to process image", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                } else {
                    // Enhanced error logging
                    val errorBody = response.errorBody()?.string()
                    val errorCode = response.code()
                    Toast.makeText(this@AddContactActivity, "Failed to remove background: $errorBody", Toast.LENGTH_SHORT).show()
                    Log.e("RemoveBgService", "Failed to remove background. Error code: $errorCode, Error body: $errorBody")
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                // Enhanced failure logging
                if (t is SocketTimeoutException) {
                    Toast.makeText(this@AddContactActivity, "Request timed out. Please try again later.", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this@AddContactActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
                Log.e("RemoveBgService", "API call failed", t)
            }
        })
    }



    private fun saveCroppedImage(imageBytes: ByteArray): Uri? {
        return try {
            val file = File(externalCacheDir, "croppedImage_${UUID.randomUUID()}.jpg")
            file.writeBytes(imageBytes)
            Uri.fromFile(file)
        } catch (e: Exception) {
            null
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                REQUEST_CODE_GALLERY -> {
                    data?.data?.let { imageUri ->
                        performCrop(imageUri)
                    }
                }
                REQUEST_CODE_CAMERA -> {
                    imageUri?.let { uri ->
                        performCrop(uri)
                    }
                }
                UCrop.REQUEST_CROP -> {
                    handleCropResult(resultCode, data)
                }
            }
        } else if (resultCode == UCrop.RESULT_ERROR) {
            val cropError = data?.let { UCrop.getError(it) }
            Toast.makeText(this, "Crop error: ${cropError?.message}", Toast.LENGTH_SHORT).show()
        }
    }

    interface RemoveBgService {
        @Multipart
        @POST("v1.0/removebg")
        fun removeBackground(
            @Header("X-Api-Key") apiKey: String,
            @Part imageFile: MultipartBody.Part
        ): Call<ResponseBody>
    }

    companion object {
        private const val REQUEST_CODE_GALLERY = 1
        private const val REQUEST_CODE_CAMERA = 2
    }
}
