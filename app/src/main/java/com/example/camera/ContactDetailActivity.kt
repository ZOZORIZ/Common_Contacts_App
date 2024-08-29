package com.example.camera

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.google.i18n.phonenumbers.NumberParseException
import com.google.i18n.phonenumbers.Phonenumber


fun formatPhoneNumberWithLib(phoneNumber: String, defaultRegion: String = "IN"): String? {
    val phoneUtil = PhoneNumberUtil.getInstance()
    return try {
        val parsedNumber: Phonenumber.PhoneNumber = phoneUtil.parse(phoneNumber, defaultRegion)
        if (phoneUtil.isValidNumber(parsedNumber)) {
            phoneUtil.format(parsedNumber, PhoneNumberUtil.PhoneNumberFormat.E164)
        } else {
            null
        }
    } catch (e: NumberParseException) {
        e.printStackTrace()
        null
    }
}

class ContactDetailActivity : AppCompatActivity() {

    private lateinit var contactImageView: ImageView
    private lateinit var contactName: TextView
    private lateinit var contactPhone: TextView
    private lateinit var contactEmail: TextView
    private lateinit var contactBirthday: TextView
    private lateinit var whatsappButton: Button
    private lateinit var smsButton: Button
    private lateinit var contactPhone2: TextView
    private lateinit var label: TextView
    private lateinit var callButton: Button
    private var contact: Contact? = null // Ensure contact is nullable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contact_detail)

        // Initialize views
        contactImageView = findViewById(R.id.contactImageView)
        contactName = findViewById(R.id.contactName)
        contactPhone = findViewById(R.id.contactPhone)
        contactEmail = findViewById(R.id.contactEmail)
        contactBirthday = findViewById(R.id.contactBirthday)
        whatsappButton = findViewById(R.id.button_whatsapp)
        smsButton = findViewById(R.id.button_message)
        contactPhone2 = findViewById(R.id.contactPhone2)
        label = findViewById(R.id.contactLabel)
        callButton = findViewById(R.id.button_call)
        // Get contact from intent
        contact = intent.getParcelableExtra("contact")

        // Setup contact details
        contact?.let {
            setupContactDetails(it)
        } ?: run {
            Toast.makeText(this, "No contact data", Toast.LENGTH_SHORT).show()
        }

        val cardView = findViewById<CardView>(R.id.cardview) // Make sure the ID matches
        cardView.setOnClickListener {
            contact?.let {
                // Use contact.imageUrl and contact.name directly
                showImagePreview(it.imageUrl, it.name)
            }
        }

        findViewById<ImageButton>(R.id.back_button).setOnClickListener {
            onBackPressed()
        }

        findViewById<ImageButton>(R.id.edit_icon).setOnClickListener {
            val intent = Intent(this, AddContactActivity::class.java).apply {
                putExtra("contact", contact)
            }
            startActivityForResult(intent, EDIT_CONTACT_REQUEST)
        }

        callButton.setOnClickListener {
            contact?.let {
                val callIntent = Intent(this, CallActivity::class.java).apply {
                    putExtra("CONTACT_NAME", it.name)
                    putExtra("CONTACT_PHONE_NUMBER", it.phoneNumber)
                }
                startActivity(callIntent)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == EDIT_CONTACT_REQUEST && resultCode == Activity.RESULT_OK) {
            val updatedContact = data?.getParcelableExtra<Contact>("contact")
            updatedContact?.let {
                setupContactDetails(it)
            }
        }
    }




    companion object {
        private const val EDIT_CONTACT_REQUEST = 1
    }

    private fun setupContactDetails(contact: Contact) {
        contactName.text = contact.name.ifEmpty { "No Name" }
        contactPhone.text = contact.phoneNumber.ifEmpty { "No Phone Number Added" }
        contactPhone2.text = contact.phoneNumber2.ifEmpty { "No Telephone Number Added" }
        contactEmail.text = contact.email.ifEmpty { "No Email Added" }
        contactBirthday.text = contact.birthday.ifEmpty { "No Birthday Added" }
        label.text = contact.label.ifEmpty { "No Label" }

        if (contact.imageUrl.isNotEmpty()) {
            Glide.with(this)
                .load(contact.imageUrl)
                .apply(
                    RequestOptions()
                        .placeholder(R.drawable.placeholder_image) // Optional placeholder
                        .error(R.drawable.placeholder_image) // Optional error image
                )
                .into(contactImageView)
        } else {
            contactImageView.setImageResource(R.drawable.placeholder_image)
        }

        // Show or hide label
        label.visibility = if (contact.label.isNotEmpty()) View.VISIBLE else View.GONE

        // Set WhatsApp button click listener
        whatsappButton.setOnClickListener {
            val phoneNumber = contact.phoneNumber?.replace("+", "")?.replace(" ", "")?.replace("-", "")
            val formattedNumber = "$phoneNumber"
            Log.d("WhatsAppIntent", "Formatted Number: $formattedNumber")

            val uri = Uri.parse("https://wa.me/$formattedNumber")
            val intent = Intent(Intent.ACTION_VIEW, uri)
            if (intent.resolveActivity(packageManager) != null) {
                startActivity(intent)
            } else {
                Toast.makeText(this, "WhatsApp is not installed", Toast.LENGTH_SHORT).show()
            }
        }

        // Set SMS button click listener
        smsButton.setOnClickListener {
            val phoneNumber = contact.phoneNumber
            if (phoneNumber.isNullOrEmpty()) {
                Toast.makeText(this, "Phone number is missing", Toast.LENGTH_SHORT).show()
            } else {
                sendSMSMessage(phoneNumber)
            }
        }
    }




    private fun sendSMSMessage(phoneNumber: String) {
        val uri = Uri.parse("sms:$phoneNumber")
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            putExtra("sms_body", "Hello") // Optional: pre-fill the message body
        }
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        } else {
            Toast.makeText(this, "No SMS app found", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showImagePreview(imageUrl: String, name: String) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_image_preview, null)
        val imageView = dialogView.findViewById<ImageView>(R.id.imagePreview)
        val nameTextView = dialogView.findViewById<TextView>(R.id.contactNameTextView)

        // Load the image and set the name
        Glide.with(this)
            .asBitmap()
            .load(imageUrl)
            .into(object : com.bumptech.glide.request.target.CustomTarget<Bitmap>() {
                override fun onResourceReady(resource: Bitmap, transition: com.bumptech.glide.request.transition.Transition<in Bitmap>?) {
                    imageView.setImageBitmap(resource)
                    nameTextView.text = name // Set the contact name

                    // Create and show the dialog
                    val dialog = AlertDialog.Builder(this@ContactDetailActivity)
                        .setView(dialogView)
                        .setCancelable(true)
                        .create()

                    // Set the dialog background to transparent
                    dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

                    dialog.show()

                    // Adjust dialog dimensions based on the image size
                    val maxWidth = 1000  // Maximum width you want to allow
                    val maxHeight = 1600 // Maximum height you want to allow

                    val layoutParams = dialog.window?.attributes
                    val imageWidth = resource.width
                    val imageHeight = resource.height

                    // Calculate scaled width and height
                    val scale = Math.min(maxWidth / imageWidth.toFloat(), maxHeight / imageHeight.toFloat())
                    val width = (imageWidth * scale).toInt()
                    val height = (imageHeight * scale).toInt()

                    layoutParams?.width = width
                    layoutParams?.height = height
                    dialog.window?.attributes = layoutParams
                }

                override fun onLoadCleared(placeholder: Drawable?) {
                    // Handle cleanup if necessary
                }
            })
    }
}






