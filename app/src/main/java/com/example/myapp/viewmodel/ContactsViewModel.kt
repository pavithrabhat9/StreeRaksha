package com.example.myapp.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.myapp.R
import com.example.myapp.model.ContactData
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

private const val PREFS_NAME = "SosPreferences"
private const val CONTACTS_KEY = "contacts"

class ContactsViewModel(application: Application) : AndroidViewModel(application) {
    private val _contacts = MutableLiveData<List<ContactData>>()
    val contacts: LiveData<List<ContactData>> = _contacts
    
    private val gson = Gson()
    
    init {
        loadContacts()
    }

    fun loadContacts() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            _contacts.value = emptyList()
            return
        }

        // CHANGE THIS: Add user ID to SharedPreferences name
        val prefs = getApplication<Application>().getSharedPreferences(
            "${PREFS_NAME}_${currentUser.uid}",
            Context.MODE_PRIVATE
        )
        val contactsJson = prefs.getString(CONTACTS_KEY, "[]") ?: "[]"
        val type = object : TypeToken<List<ContactData>>() {}.type
        val contactsList = gson.fromJson<List<ContactData>>(contactsJson, type) ?: emptyList()
        _contacts.value = contactsList
    }
    
    fun hasContacts(): Boolean {
        return !_contacts.value.isNullOrEmpty()
    }
}
