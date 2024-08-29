package com.example.camera

import android.app.Application
import androidx.room.Room
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MyApplication : Application() {

    // Initialize the database using by lazy
    val database: AppDatabase by lazy {
        AppDatabase.getDatabase(this)
    }

    // Initialize the ContactRepository using the database instance
    val contactRepository: ContactRepository by lazy {
        ContactRepository(database.contactDao())
    }

    override fun onCreate() {
        super.onCreate()
        // Initialize Firebase
        FirebaseFirestore.getInstance()  // Ensure Firestore is initialized
        FirebaseAuth.getInstance()
    }
}
