package com.ankitts.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ankitts.exltotts.databinding.ItemRegularWordBinding
import com.ankitts.model.RegularWord

class RegularWordAdapter : RecyclerView.Adapter<RegularWordAdapter.ViewHolder>() {

    private var items: List<RegularWord> = emptyList()

    fun submitList(newItems: List<RegularWord>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemRegularWordBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class ViewHolder(private val binding: ItemRegularWordBinding) : 
        RecyclerView.ViewHolder(binding.root) {

        fun bind(word: RegularWord) {
            binding.tvWord.text = word.word
            
            // Handle meaning visibility
            if (!word.meaning.isNullOrBlank()) {
                binding.tvMeaning.text = word.meaning.trim()
                binding.layoutMeaning.visibility = View.VISIBLE
            } else {
                binding.layoutMeaning.visibility = View.GONE
            }
            
            // Handle example visibility
            if (!word.example.isNullOrBlank()) {
                binding.tvExample.text = word.example.trim()
                binding.layoutExample.visibility = View.VISIBLE
            } else {
                binding.layoutExample.visibility = View.GONE
            }
        }
    }
}
