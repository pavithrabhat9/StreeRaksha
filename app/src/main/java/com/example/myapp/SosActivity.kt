package com.example.myapp

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.ContactsContract
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapp.adapter.ContactAdapter
import com.example.myapp.helper.SwipeToDeleteCallback
import com.example.myapp.model.ContactData
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import androidx.lifecycle.ViewModelProvider
import com.example.myapp.viewmodel.ContactsViewModel
import com.example.myapp.utils.FeaturePermissionHelper
import com.google.firebase.auth.FirebaseAuth

class SosActivity : AppCompatActivity() {
    private val CONTACT_PICKER_REQUEST = 1
    private val PERMISSION_REQUEST_CONTACTS = 2
    private val PERMISSION_REQUEST_SMS = 3  // Added back for contact handling
    private lateinit var contactAdapter: ContactAdapter
    private lateinit var contactsViewModel: ContactsViewModel
    private val gson = Gson()
    private lateinit var emptyStateView: View
    private var pendingOpenContactPicker: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sos)
        
        // Initialize ViewModel
        contactsViewModel = ViewModelProvider(this).get(ContactsViewModel::class.java)

        val recyclerView = findViewById<RecyclerView>(R.id.rv_contacts)
        emptyStateView = findViewById(R.id.emptyStateView)
        
        contactAdapter = ContactAdapter()
        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@SosActivity)
            adapter = contactAdapter
            setHasFixedSize(true)

            val swipeHandler = SwipeToDeleteCallback(this@SosActivity, contactAdapter) {
                saveContacts() // Save after delete
                updateEmptyState()
            }
            val itemTouchHelper = ItemTouchHelper(swipeHandler)
            itemTouchHelper.attachToRecyclerView(this)
        }

        findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.fab_add_contact).setOnClickListener {
            handleContactSelection()
        }



        loadContacts()
        
        // Set up the toolbar
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        
        // Handle back button click
        toolbar.setNavigationOnClickListener {
            onBackPressed()
        }
    }

    override fun onResume() {
        super.onResume()
        
        // Recheck contacts permissions when returning from settings
        FeaturePermissionHelper.Companion.ContactsFeature.recheckPermissions(this) {
            if (pendingOpenContactPicker) {
                openContactPicker()
            }
        }
        pendingOpenContactPicker = false
        
        // Refresh contacts
        loadContacts()
    }

    private fun handleContactSelection() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            openContactPicker()
        } else {
            // Show custom dialog guiding user to Settings
            pendingOpenContactPicker = true
            FeaturePermissionHelper.Companion.ContactsFeature.checkPermissions(this) { }
        }
    }

    private fun openContactPicker() {
        val intent = Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)
        startActivityForResult(intent, CONTACT_PICKER_REQUEST)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CONTACTS && 
            grantResults.isNotEmpty() && 
            grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            openContactPicker()
        }
    }

    private fun saveContacts() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        val currentContacts = contactAdapter.contacts
        // CHANGE THIS: Add user ID to SharedPreferences name
        val prefs = getSharedPreferences("SosPreferences_${currentUser.uid}", MODE_PRIVATE)
        val editor = prefs.edit()
        val contactsJson = gson.toJson(currentContacts)
        editor.putString("contacts", contactsJson)
        editor.apply()

        // Update the ViewModel
        contactsViewModel.loadContacts()
    }

    private fun loadContacts() {
        contactsViewModel.contacts.observe(this) { contacts ->
            contactAdapter.updateContacts(contacts.toMutableList())
            updateEmptyState()
        }
        contactsViewModel.loadContacts()
    }
    
    private fun updateEmptyState() {
        if (::emptyStateView.isInitialized) {
            emptyStateView.visibility = if (contactAdapter.itemCount == 0) View.VISIBLE else View.GONE
        }
    }





    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CONTACT_PICKER_REQUEST && resultCode == RESULT_OK) {
            data?.data?.let { uri ->
                val cursor = contentResolver.query(uri, null, null, null, null)
                cursor?.use {
                    if (it.moveToFirst()) {
                        // Check if already have 5 contacts
                        if (contactAdapter.contacts.size >= 5) {
                            Toast.makeText(this, "Maximum 5 contacts can be added", Toast.LENGTH_LONG).show()
                            return@use
                        }

                        // Safely get column indices
                        val nameColumn = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                        val phoneColumn = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

                        // Check if columns exist before accessing them
                        if (nameColumn >= 0 && phoneColumn >= 0) {
                            val name = it.getString(nameColumn) ?: "Unknown"
                            val phone = it.getString(phoneColumn) ?: return@use

                            // Validate phone number is not empty
                            if (phone.isBlank()) {
                                Toast.makeText(this, "Invalid phone number", Toast.LENGTH_SHORT).show()
                                return@use
                            }

                            val currentContacts = contactAdapter.contacts.toMutableList()
                            val newContact = ContactData(name, phone)

                            val alreadyExists = currentContacts.any { contact -> contact.phone == phone }
                            if (!alreadyExists) {
                                currentContacts.add(newContact)
                                contactAdapter.updateContacts(currentContacts)
                                saveContacts()
                                updateEmptyState()
                                Toast.makeText(this, "Contact added successfully", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(this, "Contact already added", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(this, "Error: Could not read contact information", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(this, "No contact selected", Toast.LENGTH_SHORT).show()
                    }
                } ?: run {
                    Toast.makeText(this, "Error accessing contacts", Toast.LENGTH_SHORT).show()
                }
            } ?: run {
                Toast.makeText(this, "No contact selected", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
