package com.example.camera

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
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
import java.io.File
import java.util.UUID

class AddContactActivity : AppCompatActivity() {

    private lateinit var backButton: ImageButton
    private var existingContact: Contact? = null
    private lateinit var binding: ActivityAddContactBinding
    private var imageUri: Uri? = null
    private val firebaseManager = FirebaseManager()
    private val viewModel: ContactViewModel by viewModels {
        ContactViewModelFactory((application as MyApplication).contactRepository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddContactBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //return to previous activity
        backButton = findViewById(R.id.back_button)
        binding.backButton.setOnClickListener {
            onBackPressed()
        }

        existingContact = intent.getParcelableExtra("contact")
        if (existingContact != null) {
            // Update the header text to "Edit Contact"
            binding.header.text = "Edit Contact"

            // Update the text below the image to "Click To Update Picture"
            binding.textBelowImage.text = "Click To Update Picture"

            // Fetch and observe contact details using ViewModel
            existingContact?.let {
                viewModel.getContactById(it.id).observe(this) { contact ->
                    contact?.let {
                        preFillContactDetails(it)
                    }
                }
            }
        } else {
            // If it's a new contact, you can set the default header and image text if needed
            binding.header.text = "Add New Contact"
            binding.textBelowImage.text = "Click To Add Picture"
        }

        // Set click listener to open the image source dialog
        binding.cardview.setOnClickListener {
            showImageSourceDialog()
        }

        // Set click listener to save contact
        binding.saveContactButton.setOnClickListener {
            saveContact()
        }
    }

    private fun preFillContactDetails(contact: Contact) {
        binding.nameInput.setText(contact.name)
        val fullPhoneNumber = contact.phoneNumber ?: ""
        val phoneUtil = PhoneNumberUtil.getInstance()
        try {
            val parsedNumber = phoneUtil.parse(fullPhoneNumber, "IN")  // Adjust default region as needed
            binding.countryCodeInput.setText("+${parsedNumber.countryCode}")
            binding.phoneInput.setText(parsedNumber.nationalNumber.toString())
        } catch (e: NumberParseException) {
            e.printStackTrace()
            // Handle parse error, possibly set default or empty values
            binding.countryCodeInput.setText("")
            binding.phoneInput.setText(fullPhoneNumber)
        }

        binding.phoneInput2.setText(contact.phoneNumber2)
        binding.labelInput.setText(contact.label)
        binding.emailInput.setText(contact.email)
        binding.birthdayInput.setText(contact.birthday)
        // Load the contact image if available
        contact.imageUrl?.let {
            loadImage(Uri.parse(it), binding.contactImageView)
            binding.contactImageView.visibility = View.VISIBLE
            binding.placeholderImageView.visibility = View.GONE
        }
    }

    private fun saveContact() {
        val name = binding.nameInput.text.toString()
        val countryCode = binding.countryCodeInput.text.toString()
        val phone = binding.phoneInput.text.toString()
        val fullPhoneNumber = "$countryCode $phone"  // Concatenate country code and phone number
        val phoneNumber2 = binding.phoneInput2.text.toString()
        val email = binding.emailInput.text.toString()
        val label = binding.labelInput.text.toString()
        val birthday = binding.birthdayInput.text.toString()

        if (name.isNotEmpty() && phone.isNotEmpty()) {
            // Disable the button to prevent multiple clicks
            binding.saveContactButton.isEnabled = false

            // Show the custom progress dialog
            val savingDialog = showSavingDialog()

            lifecycleScope.launch {
                val imageUrl = withContext(Dispatchers.IO) {
                    imageUri?.let { uri ->
                        uploadImageToFirebase(uri)
                    } ?: existingContact?.imageUrl.orEmpty()
                }

                if (imageUrl.isNotEmpty()) {
                    val firebaseId = existingContact?.firebaseId ?: UUID.randomUUID().toString()

                    val contact = Contact(
                        id = firebaseId,
                        name = name,
                        phoneNumber = fullPhoneNumber,
                        imageUrl = imageUrl,
                        firebaseId = firebaseId,
                        phoneNumber2 = phoneNumber2,
                        email = email,
                        label = label,
                        birthday = birthday
                    )

                    // Save or update the contact in Firestore
                    val db = FirebaseFirestore.getInstance()
                    db.collection("contacts").document(firebaseId).set(contact)
                        .addOnSuccessListener {
                            // Insert into Room database after Firestore
                            viewModel.insertContacts(listOf(contact))
                            Toast.makeText(this@AddContactActivity, "Contact saved successfully", Toast.LENGTH_SHORT).show()
                            savingDialog.dismiss()

                            // Prepare result Intent and finish activity
                            val resultIntent = Intent().apply {
                                putExtra("contact", contact)
                            }
                            setResult(Activity.RESULT_OK, resultIntent)
                            finish()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this@AddContactActivity, "Error saving contact: ${e.message}", Toast.LENGTH_SHORT).show()
                            savingDialog.dismiss()
                        }
                } else {
                    Toast.makeText(this@AddContactActivity, "Failed to upload image. Try again.", Toast.LENGTH_SHORT).show()
                    savingDialog.dismiss()
                }

                // Re-enable the button after the operation is complete
                binding.saveContactButton.isEnabled = true
            }
        } else {
            Toast.makeText(this@AddContactActivity, "Please fill all fields", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showSavingDialog(): AlertDialog {
        val dialogView = layoutInflater.inflate(R.layout.loading_screen, null)
        val builder = AlertDialog.Builder(this,R.style.TransparentDialog)
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
            val downloadUrl = imageRef.downloadUrl.await()
            downloadUrl.toString()
        } catch (e: Exception) {
            ""
        }
    }

    private fun loadImage(imageUri: Uri?, imageView: ImageView) {
        if (imageUri != null) {
            Glide.with(this)
                .load(imageUri)
                .apply(RequestOptions()
                    .override(dpToPx(50), dpToPx(50))
                    .diskCacheStrategy(DiskCacheStrategy.ALL)  // Cache the image for faster loading
                    .skipMemoryCache(false)
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

        // Adjust the width of the dialog programmatically
        val window = alertDialog.window
        val params = window?.attributes
        params?.width = (resources.displayMetrics.widthPixels * 0.9).toInt()
        window?.attributes = params
    }


    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, REQUEST_IMAGE_PICK)
    }

    private fun startCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), CAMERA_REQUEST_CODE)
        } else {
            imageUri = createImageUri()
            imageUri?.let { uri ->
                val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                    putExtra(MediaStore.EXTRA_OUTPUT, uri)
                }
                startActivityForResult(intent, REQUEST_IMAGE_CAPTURE)
            }
        }
    }

    private fun createImageUri(): Uri {
        val imageFile = File(externalCacheDir, "photo.jpg")
        return FileProvider.getUriForFile(this, "${packageName}.fileprovider", imageFile)
    }


    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            CAMERA_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startCamera()
                } else {
                    Toast.makeText(this, "Camera permission is required", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                REQUEST_IMAGE_CAPTURE -> {
                    imageUri?.let { uri ->
                        startCrop(uri)
                    }
                }
                REQUEST_IMAGE_PICK -> {
                    val selectedImageUri = data?.data
                    selectedImageUri?.let { uri ->
                        startCrop(uri)
                    }
                }
                UCrop.REQUEST_CROP -> {
                    val resultUri = UCrop.getOutput(data!!)
                    resultUri?.let { uri ->
                        imageUri = uri
                        binding.contactImageView.setImageDrawable(null)
                        loadImage(uri, binding.contactImageView)
                        binding.contactImageView.visibility = View.VISIBLE
                        binding.placeholderImageView.visibility = View.GONE
                        imageUri = uri
                    }
                }
            }
        } else if (resultCode == UCrop.RESULT_ERROR) {
            val cropError = UCrop.getError(data!!)
            Toast.makeText(this, "Crop error: ${cropError?.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startCrop(uri: Uri) {
        val destinationUri = Uri.fromFile(createTempFile())

        UCrop.of(uri, destinationUri)
            .withOptions(UCrop.Options().apply {
                setToolbarTitle("Crop Image")
                setCompressionQuality(100)
            })
            .start(this)
    }

    private fun createTempFile(): File {
        val file = File(externalCacheDir, "cropped_image_${System.currentTimeMillis()}.jpg")
        file.createNewFile()
        return file
    }

    private fun dpToPx(dp: Int): Int {
        val density = resources.displayMetrics.density
        return (dp * density).toInt()
    }




    companion object {
        const val CAMERA_REQUEST_CODE = 1001
        const val REQUEST_IMAGE_CAPTURE = 1002
        const val REQUEST_IMAGE_PICK = 1003
    }
}