package com.example.camera

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.request.RequestOptions

class ContactAdapter(
    private val onItemClickListener: ((Contact) -> Unit)? = null
) : ListAdapter<Contact, ContactAdapter.ContactViewHolder>(ContactDiffCallback()) {

    private var onItemLongClickListener: ((Contact) -> Unit)? = null
    private var onCallButtonClickListener: ((Contact) -> Unit)? = null
    private var onQuickActionClickListener: ((Contact, String) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_contact, parent, false)
        return ContactViewHolder(view)
    }

    override fun onBindViewHolder(holder: ContactViewHolder, position: Int) {
        val contact = getItem(position)
        holder.bind(contact)
    }

    fun setOnQuickActionClickListener(listener: (Contact, String) -> Unit) {
        this.onQuickActionClickListener = listener

    }

    fun setOnItemLongClickListener(listener: (Contact) -> Unit) {
        onItemLongClickListener = listener
    }

    fun setOnCallButtonClickListener(listener: (Contact) -> Unit) {
        onCallButtonClickListener = listener
    }

    inner class ContactViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val contactImageView: ImageView = itemView.findViewById(R.id.contactImage)
        private val contactNameTextView: TextView = itemView.findViewById(R.id.contactName)
        private val contactPhoneTextView: TextView = itemView.findViewById(R.id.contactPhone)
        private val threeDotButton: ImageButton = itemView.findViewById(R.id.three_dots)

        @SuppressLint("CheckResult")
        fun bind(contact: Contact) {
            // Set contact name and phone number
            contactNameTextView.text = contact.name
            contactPhoneTextView.text = contact.phoneNumber

            // Load contact image using Glide with circular crop
            Glide.with(itemView.context)
                .load(contact.imageUrl)
                .apply(RequestOptions()
                    .diskCacheStrategy(DiskCacheStrategy.ALL) // Cache images for better performance
                    .skipMemoryCache(false) // Optional: Skip memory cache if needed
                    .transform(CircleCrop()) // Apply circular crop
                )
                .placeholder(R.drawable.placeholder_image) // Placeholder image while loading
                .error(R.drawable.placeholder_image) // Image to show on error
                .into(contactImageView)

            // Set long click listener
            itemView.setOnLongClickListener {
                onItemLongClickListener?.invoke(contact)
                true
            }

            // Set three-dot button click listener
            threeDotButton.setOnClickListener {
                showQuickActions(it, contact)
            }

            // Set item click listener
            itemView.setOnClickListener {
                onItemClickListener?.invoke(contact)
            }
        }

        private fun showQuickActions(view: View, contact: Contact) {
            val popupMenu = PopupMenu(view.context, view, 0, 0, R.style.CustomPopupMenu) // Apply the custom style here
            popupMenu.menuInflater.inflate(R.menu.menu_quick_actions, popupMenu.menu)
            popupMenu.setOnMenuItemClickListener { menuItem ->
                onQuickActionClickListener?.invoke(contact, menuItem.title.toString())
                true
            }
            popupMenu.show()
        }
    }
}



// DiffUtil callback for efficiently updating the list
class ContactDiffCallback : DiffUtil.ItemCallback<Contact>() {
    override fun areItemsTheSame(oldItem: Contact, newItem: Contact): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Contact, newItem: Contact): Boolean {
        return oldItem == newItem
    }
}
