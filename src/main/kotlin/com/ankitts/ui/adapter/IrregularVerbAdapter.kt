package com.ankitts.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ankitts.exltotts.databinding.ItemIrregularVerbBinding
import com.ankitts.model.IrregularVerb

class IrregularVerbAdapter : RecyclerView.Adapter<IrregularVerbAdapter.ViewHolder>() {

    private var items: List<IrregularVerb> = emptyList()

    fun submitList(newItems: List<IrregularVerb>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemIrregularVerbBinding.inflate(
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

    inner class ViewHolder(private val binding: ItemIrregularVerbBinding) : 
        RecyclerView.ViewHolder(binding.root) {

        fun bind(verb: IrregularVerb) {
            binding.tvWord.text = verb.word
            
            // Handle meaning visibility
            if (!verb.meaning.isNullOrBlank()) {
                binding.tvMeaning.text = verb.meaning.trim()
                binding.layoutMeaning.visibility = View.VISIBLE
            } else {
                binding.layoutMeaning.visibility = View.GONE
            }
            
            // Handle example visibility
            if (!verb.example.isNullOrBlank()) {
                binding.tvExample.text = verb.example.trim()
                binding.layoutExample.visibility = View.VISIBLE
            } else {
                binding.layoutExample.visibility = View.GONE
            }
            
            // Handle past simple visibility
            if (!verb.pastSimple.isNullOrBlank()) {
                binding.tvPastSimple.text = verb.pastSimple.trim()
                binding.layoutPastSimple.visibility = View.VISIBLE
            } else {
                binding.layoutPastSimple.visibility = View.GONE
            }
            
            // Handle past participle visibility
            if (!verb.pastParticiple.isNullOrBlank()) {
                binding.tvPastParticiple.text = verb.pastParticiple.trim()
                binding.layoutPastParticiple.visibility = View.VISIBLE
            } else {
                binding.layoutPastParticiple.visibility = View.GONE
            }
        }
    }
}
