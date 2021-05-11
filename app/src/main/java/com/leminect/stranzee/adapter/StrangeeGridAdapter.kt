package com.leminect.stranzee.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.leminect.stranzee.databinding.StrangeeListItemBinding
import com.leminect.stranzee.model.Strangee

class StrangeeGridAdapter(private val clickListener: StrangeeClickListener, private val showCross: Boolean = false): ListAdapter<Strangee, StrangeeGridAdapter.StrangeeViewHolder>(StrangeeDiffUtil()) {
    class StrangeeDiffUtil: DiffUtil.ItemCallback<Strangee>() {
        override fun areItemsTheSame(oldItem: Strangee, newItem: Strangee): Boolean {
            return oldItem.userId == newItem.userId
        }

        override fun areContentsTheSame(oldItem: Strangee, newItem: Strangee): Boolean {
            return oldItem == newItem
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StrangeeViewHolder {
        return StrangeeViewHolder.from(parent)
    }

    override fun onBindViewHolder(holder: StrangeeViewHolder, position: Int) {
        val item = getItem(position)
        holder.binding(item, clickListener, showCross)
    }

    class StrangeeViewHolder(val binding: StrangeeListItemBinding): RecyclerView.ViewHolder(binding.root) {
        fun binding(item: Strangee, clickListener: StrangeeClickListener, showCross: Boolean) {
            binding.strangee = item
            binding.clickListener = clickListener
            binding.showCross = showCross
            binding.executePendingBindings()
        }

        companion object {
            fun from(parent: ViewGroup) : StrangeeViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = StrangeeListItemBinding.inflate(layoutInflater, parent, false)
                return StrangeeViewHolder((binding))
            }
        }
    }
}

class StrangeeClickListener(val clickListener: (Strangee) -> Unit,
                            val saveOrUnSaveListener: (Strangee) -> Unit,
                            val crossClickListener: (Strangee) -> Unit) {
    fun onClick(strangee: Strangee) = clickListener(strangee)
    fun onSaveOrUnSaveClick(strangee: Strangee) = saveOrUnSaveListener(strangee)
    fun onCrossClick(strangee: Strangee) = crossClickListener(strangee)
}