package com.rafiq.app.ui.chat

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.rafiq.app.R

class ChatAdapter(
    private val messages: MutableList<ChatMessage>,
    private val onImageClick: (String) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_USER = 1
        private const val TYPE_RAFIQ = 2
    }

    override fun getItemViewType(position: Int): Int {
        return if (messages[position].isUser) TYPE_USER else TYPE_RAFIQ
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_USER) {
            val view = inflater.inflate(R.layout.item_message_user, parent, false)
            UserViewHolder(view)
        } else {
            val view = inflater.inflate(R.layout.item_message_rafiq, parent, false)
            RafiqViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = messages[position]
        when (holder) {
            is UserViewHolder -> holder.bind(message, onImageClick)
            is RafiqViewHolder -> holder.bind(message, onImageClick)
        }
    }

    override fun getItemCount(): Int = messages.size

    fun addMessage(message: ChatMessage) {
        messages.add(message)
        notifyItemInserted(messages.size - 1)
    }

    // ===== ViewHolders =====
    class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textView: TextView = itemView.findViewById(R.id.messageText)
        private val imageView: ImageView = itemView.findViewById(R.id.messageImage)

        fun bind(message: ChatMessage, onImageClick: (String) -> Unit) {
            // نص
            if (message.text.isNullOrBlank()) {
                textView.visibility = View.GONE
            } else {
                textView.visibility = View.VISIBLE
                textView.text = message.text
            }

            // صورة
            if (message.imageUri.isNullOrBlank()) {
                imageView.visibility = View.GONE
            } else {
                imageView.visibility = View.VISIBLE
                Glide.with(itemView.context)
                    .load(message.imageUri)
                    .into(imageView)

                imageView.setOnClickListener { onImageClick(message.imageUri) }
            }
        }
    }

    class RafiqViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textView: TextView = itemView.findViewById(R.id.messageText)
        private val imageView: ImageView = itemView.findViewById(R.id.messageImage)

        fun bind(message: ChatMessage, onImageClick: (String) -> Unit) {
            if (message.text.isNullOrBlank()) {
                textView.visibility = View.GONE
            } else {
                textView.visibility = View.VISIBLE
                textView.text = message.text
            }

            if (message.imageUri.isNullOrBlank()) {
                imageView.visibility = View.GONE
            } else {
                imageView.visibility = View.VISIBLE
                Glide.with(itemView.context)
                    .load(message.imageUri)
                    .into(imageView)

                imageView.setOnClickListener { onImageClick(message.imageUri) }
            }
        }
    }
}

// نموذج رسالة بسيط (إذا لم يكن موجوداً عندك، ضعه في نفس المجلد)
data class ChatMessage(
    val id: Long = System.currentTimeMillis(),
    val text: String? = null,
    val imageUri: String? = null,
    val isUser: Boolean = true
)
