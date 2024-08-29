package com.example.camera

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class ContactViewModel(private val repository: ContactRepository) : ViewModel() {
    val allContacts: LiveData<List<Contact>> = repository.getAllContacts()
    private val _contactList = MutableLiveData<List<Contact>>()
    val contactList: LiveData<List<Contact>> get() = _contactList

    init {
        // Observe the LiveData from the repository and post it to _contactList
        repository.getAllContacts().observeForever { contacts ->
            _contactList.value = contacts
        }
    }

    // This function inserts multiple contacts into the database
    fun insertContacts(contacts: List<Contact>) = viewModelScope.launch {
        try {
            repository.insertContacts(contacts)
        } catch (e: Exception) {
            Log.e("ViewModelError", "Error inserting contacts: ${e.message}")
        }
    }

    // This function deletes a contact by its ID
    fun delete(contactId: String) {
        viewModelScope.launch {
            repository.deleteContactById(contactId)
        }
    }

    // This function fetches all contacts as a list (used for non-UI purposes)
    suspend fun getAllContactsList(): List<Contact> {
        return try {
            repository.getAllContactsList()
        } catch (e: Exception) {
            Log.e("ViewModelError", "Error fetching all contacts list: ${e.message}")
            emptyList()
        }
    }

    fun getContactById(id: String): LiveData<Contact> {
        return repository.getContactById(id)
    }

    fun refreshContactList() {
        // Refresh the contact list (this method can be customized based on how you want to trigger a refresh)
        repository.getAllContacts().observeForever { contacts ->
            _contactList.value = contacts
        }
    }



    fun updateContacts(contacts: List<Contact>) = viewModelScope.launch {
        try {
            repository.updateContacts(contacts)

        } catch (e: Exception) {
            Log.e("ViewModelError", "Error updating contacts: ${e.message}")
        }
    }



}

class ContactViewModelFactory(private val repository: ContactRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ContactViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ContactViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
