package com.leminect.stranzee.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.leminect.stranzee.databinding.ChatMessageLayoutBinding
import com.leminect.stranzee.model.Message
import java.text.SimpleDateFormat
import java.util.*

class SingleChatAdapter(
    private val myUserId: String,
    private val clickListener: ChatMessageClickListener,
) :
    ListAdapter<Message, SingleChatAdapter.ChatMessageViewHolder>(ChatMessageDiffUtil()) {

    class ChatMessageDiffUtil : DiffUtil.ItemCallback<Message>() {
        override fun areItemsTheSame(oldItem: Message, newItem: Message): Boolean {
            return oldItem._id == newItem._id
        }

        override fun areContentsTheSame(oldItem: Message, newItem: Message): Boolean {
            return oldItem == newItem
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatMessageViewHolder {
        return ChatMessageViewHolder.from(parent)
    }

    override fun onBindViewHolder(holder: ChatMessageViewHolder, position: Int) {
        val item = getItem(position)
        val previousItem: Message? = if (position != 0) getItem(position - 1) else null
        holder.binding(item, previousItem, myUserId, clickListener)
    }

    class ChatMessageViewHolder(val binding: ChatMessageLayoutBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun binding(
            item: Message,
            previousItem: Message?,
            myUserId: String,
            clickListener: ChatMessageClickListener,
        ) {
            binding.message = item
            binding.amISender = item.userId == myUserId
            binding.clickListener = clickListener
            binding.timeText.visibility = View.GONE
            binding.messageSeparation.visibility = View.VISIBLE

            var timeDiff: Long = 600000
            previousItem?.let {
                timeDiff = item.timestamp - previousItem.timestamp
                if (timeDiff < 600000 && previousItem.userId == item.userId) {
                    binding.messageSeparation.visibility = View.GONE
                }
            }

            if (timeDiff >= 600000) {

                val actualDiff = Date().time - item.timestamp
                val sdf = SimpleDateFormat(when {
                    actualDiff > 31_536_000_000L -> "dd MMM, yyyy"
                    actualDiff > 86400_000L -> "dd MMM, hh:mm a"
                    else -> "hh:mm a"
                }, Locale.US)

                val localDateTime = sdf.format(item.timestamp)

                binding.timeText.text = localDateTime
                binding.timeText.visibility = View.VISIBLE
            }

            binding.executePendingBindings()
        }

        companion object {
            fun from(parent: ViewGroup): ChatMessageViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = ChatMessageLayoutBinding.inflate(layoutInflater, parent, false)
                return ChatMessageViewHolder(binding)
            }
        }
    }
}

class ChatMessageClickListener(val clickListener: (Message) -> Unit) {
    fun onClick(message: Message) = clickListener(message)
}