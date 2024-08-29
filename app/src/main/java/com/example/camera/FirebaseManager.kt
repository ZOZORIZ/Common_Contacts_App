package com.example.camera

import android.net.Uri
import android.util.Log
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID

class FirebaseManager {
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance().reference
    private val database: FirebaseDatabase = FirebaseDatabase.getInstance()

    private suspend fun uploadImageToFirebase(imageUri: Uri): String = withContext(Dispatchers.IO) {
        val storageRef = FirebaseStorage.getInstance().reference.child("images/${UUID.randomUUID()}")
        val uploadTask = storageRef.putFile(imageUri)

        // Await the completion of the upload task
        try {
            val snapshot = uploadTask.await()
            snapshot.metadata?.reference?.downloadUrl?.await().toString()
        } catch (e: Exception) {
            Log.e("UploadError", "Error uploading image: ${e.message}")
            ""
        }
    }

    fun saveContactToFirebase(contact: FirebaseContact) {
        firestore.collection("contacts")
            .document(contact.firebaseId) // Use Firebase's ID
            .set(contact)
            .addOnSuccessListener {
                Log.d("FirebaseManager", "Contact successfully uploaded!")
                // Optionally, notify the user that the save was successful
            }
            .addOnFailureListener { e ->
                Log.e("FirebaseManager", "Error uploading contact", e)
                // Optionally, notify the user that the save failed
            }
    }

    fun addContact(contact: FirebaseContact) {
        firestore.collection("contacts").document(contact.firebaseId).set(contact)
    }

    fun getCommonRoomContacts(): DatabaseReference {
        return database.getReference("commonRoomContacts")
    }

    fun ContactById(contactId: String, callback: (FirebaseContact?) -> Unit) {
        val contactRef = firestore.collection("contacts").document(contactId)
        contactRef.get().addOnSuccessListener { documentSnapshot ->
            if (documentSnapshot.exists()) {
                val contact = documentSnapshot.toObject(FirebaseContact::class.java)
                callback(contact)
            } else {
                callback(null)
            }
        }.addOnFailureListener { exception ->
            Log.e("FirebaseManager", "Error fetching contact by ID", exception)
            callback(null)
        }
    }
}
