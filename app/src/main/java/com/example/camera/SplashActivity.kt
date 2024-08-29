package com.example.camera

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.load.engine.GlideException
import android.graphics.drawable.Drawable


fun dpToPx(context: Context, dp: Int): Int {
    val density = context.resources.displayMetrics.density
    return (dp * density).toInt()
}

fun loadContactImage(context: Context, imageView: ImageView, imageUrl: String?) {
    val imageSize = dpToPx(context, 50) // Convert 50dp to pixels

    Glide.with(context)
        .load(imageUrl)
        .override(imageSize, imageSize) // Set the dimensions
        .diskCacheStrategy(DiskCacheStrategy.ALL) // Cache the image
        .into(imageView)
}

class SplashActivity : AppCompatActivity() {

    private lateinit var contactViewModel: ContactViewModel
    private lateinit var progressBar: ProgressBar
    private val firestoreDb = FirebaseFirestore.getInstance()
    private var isNavigated = false  // Flag to prevent multiple navigations

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Fullscreen UI settings
        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                )
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        setContentView(R.layout.activity_splash)

        // Initialize Firebase Auth (Ensure this is done before accessing Firestore)
        initializeFirebaseAuth()

        // Initialize ViewModel
        val contactRepository = (application as MyApplication).contactRepository
        val factory = ContactViewModelFactory(contactRepository)
        contactViewModel = ViewModelProvider(this, factory).get(ContactViewModel::class.java)

        // Initialize ProgressBar
        progressBar = findViewById(R.id.progressBar)
        progressBar.visibility = View.VISIBLE

        // Load contacts and update database
        fetchAndUpdateContacts()

        // Text animations
        val splashText: TextView = findViewById(R.id.splash_text1)
        val splashText2: TextView = findViewById(R.id.splash_credit)

        Handler(Looper.getMainLooper()).postDelayed({
            splashText.visibility = View.VISIBLE
            splashText2.visibility = View.VISIBLE

            val animation = AnimationUtils.loadAnimation(this, R.anim.popup_animation)
            splashText.startAnimation(animation)
            splashText2.startAnimation(animation)
        }, 500)
    }

    private fun initializeFirebaseAuth() {
        FirebaseAuth.getInstance()
            .signInWithEmailAndPassword("noahcherianjacob@gmail.com", "12345678")
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d("SplashActivity", "Authentication successful")
                } else {
                    Log.e("SplashActivity", "Authentication failed: ${task.exception?.message}")
                }
            }
    }

    private fun fetchAndUpdateContacts() {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val minLoadingTime = 3000L // Minimum loading time in milliseconds (e.g., 3 seconds)
                val startTime = System.currentTimeMillis()

                val firestoreContacts = async(Dispatchers.IO) {
                    fetchContactsFromFirestore()
                }.await()

                if (firestoreContacts.isNotEmpty()) {
                    val updateJob = async(Dispatchers.IO) {
                        updateRoomDatabase(firestoreContacts)
                    }

                    val preloadJob = async(Dispatchers.IO) {
                        preloadImages(firestoreContacts)
                    }

                    // Wait for all jobs to complete
                    awaitAll(updateJob, preloadJob)

                    // Calculate elapsed time
                    val elapsedTime = System.currentTimeMillis() - startTime
                    if (elapsedTime < minLoadingTime) {
                        delay(minLoadingTime - elapsedTime) // Delay if needed
                    }

                    // Now navigate after all operations and delay are complete
                    navigateToContactListActivity()
                } else {
                    Log.e("SplashActivity", "No contacts fetched from Firestore")
                    progressBar.visibility = View.GONE
                    navigateToContactListActivity()
                }
            } catch (e: Exception) {
                Log.e("SplashActivity", "Error during fetch/update: ${e.message}")
                progressBar.visibility = View.GONE
                navigateToContactListActivity()
            }
        }
    }


    private suspend fun fetchContactsFromFirestore(): List<Contact> {
        return try {
            val snapshot = firestoreDb.collection("contacts").get().await()
            snapshot.documents.mapNotNull { doc ->
                try {
                    val id = doc.getString("id") ?: return@mapNotNull null
                    val name = doc.getString("name") ?: ""
                    val phoneNumber = doc.getString("phoneNumber") ?: ""
                    val imageUrl = doc.getString("imageUrl") ?: ""
                    val phoneNumber2 = doc.getString("phoneNumber2") ?: ""
                    val email = doc.getString("email") ?: ""
                    val label = doc.getString("label") ?: ""
                    val birthday = doc.getString("birthday") ?: ""

                    Contact(
                        id = id,
                        name = name,
                        phoneNumber = phoneNumber,
                        imageUrl = imageUrl,
                        phoneNumber2 = phoneNumber2,
                        email = email,
                        label = label,
                        birthday = birthday,
                        firebaseId = doc.id // Use Firestore document ID directly
                    )
                } catch (e: Exception) {
                    Log.e("DocumentMappingError", "Error mapping document: ${e.message}")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e("FirestoreError", "Error fetching contacts: ${e.message}")
            emptyList()
        }
    }

    private suspend fun updateRoomDatabase(firestoreContacts: List<Contact>) {
        try {
            val existingContacts = contactViewModel.getAllContactsList()
            val existingContactIds = existingContacts.map { it.id }.toSet()

            val contactsToInsert = firestoreContacts.filter { it.id !in existingContactIds }
            val contactsToUpdate = firestoreContacts.filter { it.id in existingContactIds }
            val contactsToDelete = existingContacts.filter { existingContact ->
                firestoreContacts.none { firestoreContact -> firestoreContact.id == existingContact.id }
            }

            if (contactsToInsert.isNotEmpty()) {
                contactViewModel.insertContacts(contactsToInsert)
                Log.d("SplashActivity", "Inserted contacts: $contactsToInsert")
            }

            if (contactsToUpdate.isNotEmpty()) {
                contactViewModel.updateContacts(contactsToUpdate)
                Log.d("SplashActivity", "Updated contacts: $contactsToUpdate")
            }

            if (contactsToDelete.isNotEmpty()) {
                contactsToDelete.forEach { contactViewModel.delete(it.id) }
                Log.d("SplashActivity", "Deleted contacts: $contactsToDelete")
            }
        } catch (e: Exception) {
            Log.e("DatabaseUpdateError", "Error updating database: ${e.message}")
        }
    }

    private suspend fun preloadImages(contacts: List<Contact>) {

        val loadJobs = contacts.map { contact ->
            CoroutineScope(Dispatchers.IO).async {
                try {
                    val deferred = CompletableDeferred<Unit>()
                    Glide.with(this@SplashActivity)
                        .load(contact.imageUrl)
                        .override(100, 100)
                        .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                        .addListener(object : RequestListener<Drawable> {
                            override fun onLoadFailed(
                                e: GlideException?,
                                model: Any?,
                                target: Target<Drawable>?,
                                isFirstResource: Boolean
                            ): Boolean {
                                deferred.complete(Unit) // Complete even if it fails
                                return false
                            }

                            override fun onResourceReady(
                                resource: Drawable?,
                                model: Any?,
                                target: Target<Drawable>?,
                                dataSource: DataSource?,
                                isFirstResource: Boolean
                            ): Boolean {
                                deferred.complete(Unit) // Complete when ready
                                return false
                            }
                        })
                        .preload()
                    deferred.await() // Wait for image load to complete
                } catch (e: Exception) {
                    Log.e("ImagePreloadError", "Error preloading image for ${contact.name}: ${e.message}")
                }
            }
        }

        // Wait for all images to preload
        loadJobs.awaitAll()
    }



    private fun navigateToContactListActivity() {
        if (!isNavigated) {
            isNavigated = true
            progressBar.visibility = View.GONE
            val intent = Intent(this, ContactListActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}
