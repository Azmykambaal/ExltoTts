package com.ankitts.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ankitts.exltotts.databinding.ItemIrregularPluralBinding
import com.ankitts.model.IrregularPlural

class IrregularPluralAdapter : RecyclerView.Adapter<IrregularPluralAdapter.ViewHolder>() {

    private var items: List<IrregularPlural> = emptyList()

    fun submitList(newItems: List<IrregularPlural>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemIrregularPluralBinding.inflate(
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

    inner class ViewHolder(private val binding: ItemIrregularPluralBinding) : 
        RecyclerView.ViewHolder(binding.root) {

        fun bind(plural: IrregularPlural) {
            binding.tvWord.text = plural.word
            
            // Handle meaning visibility
            if (!plural.meaning.isNullOrBlank()) {
                binding.tvMeaning.text = plural.meaning.trim()
                binding.layoutMeaning.visibility = View.VISIBLE
            } else {
                binding.layoutMeaning.visibility = View.GONE
            }
            
            // Handle example visibility
            if (!plural.example.isNullOrBlank()) {
                binding.tvExample.text = plural.example.trim()
                binding.layoutExample.visibility = View.VISIBLE
            } else {
                binding.layoutExample.visibility = View.GONE
            }
            
            // Handle plural form visibility
            if (!plural.pluralForm.isNullOrBlank()) {
                binding.tvPluralForm.text = plural.pluralForm.trim()
                binding.layoutPluralForm.visibility = View.VISIBLE
            } else {
                binding.layoutPluralForm.visibility = View.GONE
            }
        }
    }
}
