package com.example.myapp.adapter

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.myapp.databinding.ItemEmergencyContactBinding
import com.example.myapp.model.Contact

class EmergencyContactAdapter : ListAdapter<Contact, EmergencyContactAdapter.ViewHolder>(ContactDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemEmergencyContactBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(
        private val binding: ItemEmergencyContactBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(contact: Contact) {
            binding.contact = contact
            
            // Set click listener for the call button
            binding.ivCall.setOnClickListener {
                val phoneNumber = contact.phoneNumber
                try {
                    val intent = Intent(Intent.ACTION_CALL).apply {
                        data = Uri.parse("tel:$phoneNumber")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    
                    val context = binding.root.context
                    if (context is android.app.Activity) {
                        // Check for CALL_PHONE permission at runtime
                        if (androidx.core.content.ContextCompat.checkSelfPermission(
                                context,
                                android.Manifest.permission.CALL_PHONE
                            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                        ) {
                            // Permission already granted, make the call
                            context.startActivity(intent)
                        } else {
                            // Request the permission
                            androidx.core.app.ActivityCompat.requestPermissions(
                                context,
                                arrayOf(android.Manifest.permission.CALL_PHONE),
                                123 // Arbitrary request code
                            )
                            showError("Please grant phone call permission in app settings")
                        }
                    } else {
                        context.startActivity(intent)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    showError("Error: ${e.message}")
                }
            }
            
            // Set click listener for the item
            binding.root.setOnClickListener {
                // You can add item click handling here if needed
            }
            
            binding.executePendingBindings()
        }
        
        private fun showError(message: String) {
            Toast.makeText(binding.root.context, message, Toast.LENGTH_SHORT).show()
        }
    }

    class ContactDiffCallback : DiffUtil.ItemCallback<Contact>() {
        override fun areItemsTheSame(oldItem: Contact, newItem: Contact): Boolean {
            return oldItem.name == newItem.name && oldItem.phoneNumber == newItem.phoneNumber
        }

        override fun areContentsTheSame(oldItem: Contact, newItem: Contact): Boolean {
            return oldItem == newItem
        }
    }
}
