package com.leminect.strangee.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.leminect.strangee.databinding.ChatListItemBinding
import com.leminect.strangee.model.ChatData

class ChatListAdapter(private val clickListener: ChatListClickListener): ListAdapter<ChatData, ChatListAdapter.ChatListViewHolder>(ChatListDiffUtil()) {
    class ChatListDiffUtil: DiffUtil.ItemCallback<ChatData>() {
        override fun areItemsTheSame(oldItem: ChatData, newItem: ChatData): Boolean {
            return oldItem.userId == newItem.userId
        }

        override fun areContentsTheSame(oldItem: ChatData, newItem: ChatData): Boolean {
            return oldItem == newItem
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatListViewHolder {
        return ChatListViewHolder.from(parent)
    }

    override fun onBindViewHolder(holder: ChatListViewHolder, position: Int) {
        val item = getItem(position)
        holder.binding(item, clickListener)
    }

    class ChatListViewHolder(val binding: ChatListItemBinding): RecyclerView.ViewHolder(binding.root) {
        fun binding(item: ChatData, clickListener: ChatListClickListener) {
            binding.chatData = item
            binding.clickListener = clickListener
            binding.executePendingBindings()
        }

        companion object {
            fun from(parent: ViewGroup) : ChatListViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = ChatListItemBinding.inflate(layoutInflater, parent, false)
                return ChatListViewHolder(binding)
            }
        }
    }
}

class ChatListClickListener(val clickListener: (ChatData) -> Unit) {
    fun onClick(chatData: ChatData) = clickListener(chatData)
}