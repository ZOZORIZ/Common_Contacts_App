package com.example.camera

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.camera.databinding.ActivityContactListBinding
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import android.net.Uri
import android.content.Intent


class ContactListActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ContactAdapter
    private lateinit var searchView: androidx.appcompat.widget.SearchView
    private lateinit var binding: ActivityContactListBinding
    private val contactViewModel: ContactViewModel by viewModels {
        ContactViewModelFactory((application as MyApplication).contactRepository)
    }
    private val firestoreDb = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityContactListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        if (auth.currentUser == null) {
            signInWithPredefinedCredentials()
        } else {
            setupUI()
        }
    }


    private fun signInWithPredefinedCredentials() {
        val predefinedEmail = "noahcherianjacob@gmail.com"
        val predefinedPassword = "12345678"
        auth.signInWithEmailAndPassword(predefinedEmail, predefinedPassword)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    setupUI()
                } else {
                    Log.w(
                        "AuthError",
                        "Sign in with predefined credentials failed.",
                        task.exception
                    )
                    Toast.makeText(this, "Authentication failed.", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun setupUI() {
        recyclerView = binding.recyclerView
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = ContactAdapter { contact ->
            onItemClick(contact)
        }.apply {
            setOnItemLongClickListener { contact ->
                showDeleteConfirmationDialog(contact)
            }
            setOnCallButtonClickListener { contact ->
                val intent = Intent(this@ContactListActivity, CallActivity::class.java).apply {
                    putExtra("CONTACT_NAME", contact.name)
                    putExtra("CONTACT_PHONE_NUMBER", contact.phoneNumber)
                }
                startActivity(intent)
            }
        }

        recyclerView.adapter = adapter

        contactViewModel.allContacts.observe(this) { contacts ->
            Log.d("ContactListActivity", "LiveData updated with contacts: $contacts")
            contacts?.let {
                adapter.submitList(it)
                Log.d("ContactListActivity", "Adapter list updated with contacts: $it")
            }
        }

        searchView = binding.searchView
        searchView.setOnClickListener {
            searchView.isIconified = false
        }

        searchView.setOnQueryTextListener(object :
            androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filterContacts(newText)
                return true
            }
        })

        val addContactButton: Button = binding.addContactButton
        addContactButton.setOnClickListener {
            val intent = Intent(this, AddContactActivity::class.java)
            startActivity(intent)
        }

        adapter.setOnQuickActionClickListener { contact, action ->
            when (action) {
                "Call Contact" -> initiateCall(contact)
                "Edit Contact" -> navigateToEditContact(contact)
                "View Contact" -> viewContactDetails(contact)
                "Delete Contact" -> showDeleteConfirmationDialog(contact)
                // Handle more actions here
            }
        }
    }

    private fun navigateToEditContact(contact: Contact) {
        val intent = Intent(this, AddContactActivity::class.java).apply {
            putExtra("contact", contact)
        }
        startActivity(intent)
    }

    private fun initiateCall(contact: Contact) {
        val intent = Intent(Intent.ACTION_DIAL).apply {
            data = Uri.parse("tel:${contact.phoneNumber}")
        }
        startActivity(intent)
    }


    private fun viewContactDetails(contact: Contact) {
        val intent = Intent(this, ContactDetailActivity::class.java).apply {
            putExtra("contact", contact)
        }
        startActivity(intent)
    }


    private fun filterContacts(query: String?) {
        val filteredContacts = contactViewModel.allContacts.value?.filter {
            it.name.contains(query ?: "", ignoreCase = true)
        }
        Log.d("ContactListActivity", "Filtering contacts with query: $query, Result: $filteredContacts")
        adapter.submitList(filteredContacts)
    }

    private fun showDeleteConfirmationDialog(contact: Contact) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_confirmation, null)
        val dialogBuilder = AlertDialog.Builder(this,R.style.TransparentDialog)
            .setView(dialogView)

        val alertDialog = dialogBuilder.create()

        val buttonYes: Button = dialogView.findViewById(R.id.buttonYes)
        val buttonNo: Button = dialogView.findViewById(R.id.buttonNo)

        buttonYes.setOnClickListener {
            Log.d("ContactListActivity", "Delete button clicked for contact: ${contact.id}")
            contactViewModel.delete(contact.id)  // Remove from Room database
            deleteContactFromFirestore(contact.firebaseId)  // Remove from Firestore
            Toast.makeText(this@ContactListActivity, "Contact deleted", Toast.LENGTH_SHORT)
                .show()
            alertDialog.dismiss()
        }

        buttonNo.setOnClickListener {
            alertDialog.dismiss()
        }

        alertDialog.show()
    }

    private fun deleteContactFromFirestore(contactId: String) {
        firestoreDb.collection("contacts").document(contactId)
            .delete()
            .addOnSuccessListener {
                Log.d("Firestore", "Contact successfully deleted from Firestore with ID: $contactId")
            }
            .addOnFailureListener { e ->
                Log.w("FirestoreError", "Error deleting contact from Firestore", e)
                Toast.makeText(this, "Error deleting contact: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun onItemClick(contact: Contact) {
        Log.d("ContactListActivity", "Contact clicked: $contact")
        val intent = Intent(this, ContactDetailActivity::class.java)
        intent.putExtra("contact", contact)
        startActivity(intent)
    }
}