package com.example.myapp.helper

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.example.myapp.adapter.ContactAdapter
import com.example.myapp.R

class SwipeToDeleteCallback(
    private val context: Context,
    private val adapter: ContactAdapter,
    private val onSaveCallback: () -> Unit = {}
) : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {

    private val background = ColorDrawable(Color.parseColor("#D32F2F"))
    private val deleteIcon: Drawable = ContextCompat.getDrawable(context, R.drawable.ic_contact_delete)!!
    private val intrinsicWidth = deleteIcon.intrinsicWidth
    private val intrinsicHeight = deleteIcon.intrinsicHeight

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        return false
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        val position = viewHolder.adapterPosition
        val contact = adapter.contacts[position]
        
        val alertDialog = AlertDialog.Builder(context)
            .setTitle(context.getString(R.string.delete_contact))
            .setMessage(context.getString(R.string.delete_contact_confirmation, contact.name))
            .setPositiveButton(context.getString(R.string.yes)) { _, _ ->
                adapter.removeItem(position)
                onSaveCallback()
                Toast.makeText(context, context.getString(R.string.contact_removed), Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(context.getString(R.string.no)) { dialog, _ ->
                dialog.dismiss()
                // Restore the item back to its original position
                adapter.notifyItemChanged(position)
            }
            .create()
        
        alertDialog.show()
    }

    override fun onChildDraw(
        c: Canvas,
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        dX: Float,
        dY: Float,
        actionState: Int,
        isCurrentlyActive: Boolean
    ) {
        val itemView = viewHolder.itemView
        intrinsicHeight

        // Draw the background
        if (dX > 0) {
            // Swiping right
            background.setBounds(
                itemView.left,
                itemView.top,
                dX.toInt(),
                itemView.bottom
            )
        } else {
            // Swiping left
            background.setBounds(
                itemView.right + dX.toInt(),
                itemView.top,
                itemView.right,
                itemView.bottom
            )
        }
        background.draw(c)

        // Draw the delete icon
        val iconTop = itemView.top + (itemView.height - intrinsicHeight) / 2
        val iconMargin = (itemView.height - intrinsicHeight) / 2
        val iconLeft: Int
        val iconRight: Int

        if (dX > 0) {
            // Swiping right
            iconLeft = itemView.left + iconMargin
            iconRight = itemView.left + iconMargin + intrinsicWidth
        } else {
            // Swiping left
            iconLeft = itemView.right - iconMargin - intrinsicWidth
            iconRight = itemView.right - iconMargin
        }

        deleteIcon.setBounds(iconLeft, iconTop, iconRight, iconTop + intrinsicHeight)
        deleteIcon.draw(c)

        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
    }
}
