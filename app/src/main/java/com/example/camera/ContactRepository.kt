package com.example.camera

import android.util.Log
import androidx.lifecycle.LiveData

class ContactRepository(private val contactDao: ContactDao) {

    fun getAllContacts(): LiveData<List<Contact>> {
        return contactDao.getAllContacts()
    }

    suspend fun getAllContactsList(): List<Contact> {
        return contactDao.getAllContactsList()
    }

    fun getContactById(id: String): LiveData<Contact> {
        return contactDao.getContactById(id)
    }



    suspend fun insertContacts(contacts: List<Contact>) {
        Log.d("ContactRepository", "Inserting contacts: $contacts")
        contactDao.insertContacts(contacts)
    }

    suspend fun deleteContactById(contactId: String) {
        contactDao.deleteById(contactId)
    }

    suspend fun updateContacts(contacts: List<Contact>) {
        Log.d("ContactRepository", "Updating contacts: $contacts")
        contactDao.updateAllContacts(contacts)
    }

    suspend fun insert(contact: Contact) {
        contactDao.insert(contact)
    }

    // Update a contact in the database
    suspend fun update(contact: Contact) {
        contactDao.update(contact)
    }
}
