package com.example.myapp.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.myapp.R
import com.example.myapp.model.ContactData
import com.example.myapp.domain.entities.EmergencyContact
import org.json.JSONArray

class ContactAdapter : RecyclerView.Adapter<ContactAdapter.ContactViewHolder>() {
    private val _contacts = mutableListOf<ContactData>()
    
    val contacts: List<ContactData>
        get() = _contacts

    class ContactViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvContactName: TextView = itemView.findViewById(R.id.tv_contact_name)
        private val tvContactPhone: TextView = itemView.findViewById(R.id.tv_contact_phone)

        fun bind(contact: ContactData) {
            tvContactName.text = contact.name
            tvContactPhone.text = contact.phone
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_contact, parent, false)
        return ContactViewHolder(view)
    }

    override fun onBindViewHolder(holder: ContactViewHolder, position: Int) {
        if (position in 0 until _contacts.size) {
            holder.bind(_contacts[position])
        }
        
        // Show divider for all items except the last one
        holder.itemView.findViewById<View>(R.id.divider).visibility = 
            if (position < _contacts.size - 1) View.VISIBLE else View.GONE
    }

    override fun getItemCount(): Int = _contacts.size

    fun updateContacts(newContacts: MutableList<ContactData>) {
        _contacts.clear()
        _contacts.addAll(newContacts)
        notifyDataSetChanged()
    }
    
    // Bridge method for Clean Architecture EmergencyContact
    fun updateEmergencyContacts(emergencyContacts: List<EmergencyContact>) {
        val contactDataList = emergencyContacts.map { contact ->
            ContactData(contact.name, contact.phoneNumber)
        }.toMutableList()
        updateContacts(contactDataList)
    }

    fun removeItem(position: Int) {
        if (position !in 0 until _contacts.size) return
        
        // Remove from list
        _contacts.removeAt(position)
        notifyItemRemoved(position)
        
        // Update UI
        notifyItemRangeChanged(position, _contacts.size)
    }
}
