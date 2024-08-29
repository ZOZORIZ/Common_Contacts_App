package com.example.camera

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update

@Dao
interface ContactDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContacts(contacts: kotlin.collections.List<com.example.camera.Contact>)

    @Query("DELETE FROM contacts WHERE id = :contactId")
    suspend fun deleteById(contactId: String)

    @Query("SELECT * FROM contacts")
    fun getAllContacts(): LiveData<List<Contact>>

    @Query("SELECT * FROM contacts")
    suspend fun getAllContactsList(): List<Contact>

    @Query("SELECT * FROM contacts WHERE id = :id")
    fun getContactById(id: String): LiveData<Contact>

    @Update
    suspend fun updateAllContacts(contacts: kotlin.collections.List<com.example.camera.Contact>)

    // Insert a contact into the database
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(contact: Contact)

    // Update a contact in the database
    @Update
    suspend fun update(contact: Contact)

}






