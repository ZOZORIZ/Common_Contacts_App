package com.example.camera

import androidx.room.Entity
import androidx.room.PrimaryKey
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "contacts")
data class Contact(
    @PrimaryKey val id: String, // Use String for unique document ID
    var name: String = "",
    var phoneNumber: String = "",
    var imageUrl: String = "",
    var firebaseId: String = "",
    var phoneNumber2: String = "",
    var email: String = "",
    var label: String = "",
    var birthday: String = ""

) : Parcelable










