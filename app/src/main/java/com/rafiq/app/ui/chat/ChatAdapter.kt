package com.rafiq.app.ui.chat

import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.rafiq.app.R
import com.rafiq.app.data.local.MessageEntity
import com.rafiq.app.face.Emotion
import java.text.DateFormat
import java.util.Date
import java.util.Locale

class ChatAdapter : RecyclerView.Adapter<ChatAdapter.Holder>() {

    private val items = mutableListOf<MessageEntity>()
    private var liveText: String? = null

    var onLongSpeak: ((String) -> Unit)? = null
    var textScale: Float = 1f

    override fun getItemCount() = items.size + if (liveText != null) 1 else 0

    override fun getItemViewType(position: Int): Int =
        if (position < items.size && items[position].role == MessageEntity.ROLE_USER) 1 else 2

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val layout = if (viewType == 1) R.layout.item_message_user else R.layout.item_message_rafiq
        return Holder(LayoutInflater.from(parent.context).inflate(layout, parent, false))
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val isLive = position >= items.size
        val content = if (isLive) liveText.orEmpty() else items[position].content
        val time = if (isLive) System.currentTimeMillis() else items[position].createdAt

        val text = if (!isLive && items[position].role == MessageEntity.ROLE_RAFIQ) {
            Emotion.fromTag(items[position].emotion)?.let { "${it.emoji} $content" } ?: content
        } else content

        holder.tvText.text = text
        holder.tvText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f * textScale)
        holder.tvTime.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f * textScale)
        holder.tvTime.text = DateFormat.getTimeInstance(DateFormat.SHORT, Locale.getDefault()).format(Date(time))
        holder.tvText.setOnLongClickListener {
            if (!isLive) onLongSpeak?.invoke(content)
            true
        }
    }

    fun submit(list: List<MessageEntity>) {
        items.clear(); items.addAll(list)
        notifyDataSetChanged()
    }

    fun showLive(text: String?) {
        val had = liveText != null
        val has = text != null
        liveText = text
        when {
            !had && has -> notifyItemInserted(items.size)
            had && has -> notifyItemChanged(items.size)
            had && !has -> notifyItemRemoved(items.size)
        }
    }

    class Holder(v: View) : RecyclerView.ViewHolder(v) {
        val tvText: TextView = v.findViewById(R.id.tvText)
        val tvTime: TextView = v.findViewById(R.id.tvTime)
    }
}