package com.ankitts.ui.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ankitts.exltotts.R
import com.ankitts.exltotts.databinding.ItemIrregularPluralBinding
import com.ankitts.exltotts.databinding.ItemIrregularVerbBinding
import com.ankitts.exltotts.databinding.ItemRegularWordBinding
import com.ankitts.exltotts.databinding.ItemSectionHeaderBinding
import com.ankitts.model.IrregularPlural
import com.ankitts.model.IrregularVerb
import com.ankitts.model.RegularWord

/**
 * Unified adapter for displaying preview data with section headers and items.
 * Uses multiple view types to handle section headers and different item types.
 */
class UnifiedPreviewAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_SECTION_HEADER = 0
        private const val VIEW_TYPE_REGULAR_WORD = 1
        private const val VIEW_TYPE_IRREGULAR_PLURAL = 2
        private const val VIEW_TYPE_IRREGULAR_VERB = 3
    }

    private val items = mutableListOf<Any>()
    private var context: Context? = null

    fun submitData(
        context: Context,
        regularWords: List<RegularWord>,
        irregularPlurals: List<IrregularPlural>,
        irregularVerbs: List<IrregularVerb>
    ) {
        this.context = context
        items.clear()
        
        // Add Regular Words section
        if (regularWords.isNotEmpty()) {
            items.add(SectionHeader(context.getString(R.string.regular_words_header)))
            items.addAll(regularWords)
        }
        
        // Add Irregular Plurals section
        if (irregularPlurals.isNotEmpty()) {
            items.add(SectionHeader(context.getString(R.string.irregular_plural_header)))
            items.addAll(irregularPlurals)
        }
        
        // Add Irregular Verbs section
        if (irregularVerbs.isNotEmpty()) {
            items.add(SectionHeader(context.getString(R.string.irregular_verbs_header)))
            items.addAll(irregularVerbs)
        }
        
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is SectionHeader -> VIEW_TYPE_SECTION_HEADER
            is RegularWord -> VIEW_TYPE_REGULAR_WORD
            is IrregularPlural -> VIEW_TYPE_IRREGULAR_PLURAL
            is IrregularVerb -> VIEW_TYPE_IRREGULAR_VERB
            else -> throw IllegalArgumentException("Unknown item type at position $position")
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_SECTION_HEADER -> {
                val binding = ItemSectionHeaderBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                SectionHeaderViewHolder(binding)
            }
            VIEW_TYPE_REGULAR_WORD -> {
                val binding = ItemRegularWordBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                RegularWordViewHolder(binding)
            }
            VIEW_TYPE_IRREGULAR_PLURAL -> {
                val binding = ItemIrregularPluralBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                IrregularPluralViewHolder(binding)
            }
            VIEW_TYPE_IRREGULAR_VERB -> {
                val binding = ItemIrregularVerbBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                IrregularVerbViewHolder(binding)
            }
            else -> throw IllegalArgumentException("Unknown view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is SectionHeader -> (holder as SectionHeaderViewHolder).bind(item)
            is RegularWord -> (holder as RegularWordViewHolder).bind(item)
            is IrregularPlural -> (holder as IrregularPluralViewHolder).bind(item)
            is IrregularVerb -> (holder as IrregularVerbViewHolder).bind(item)
        }
    }

    override fun getItemCount(): Int = items.size

    // Data class for section headers
    data class SectionHeader(val title: String)

    // ViewHolders
    class SectionHeaderViewHolder(private val binding: ItemSectionHeaderBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(header: SectionHeader) {
            binding.root.text = header.title
        }
    }

    class RegularWordViewHolder(private val binding: ItemRegularWordBinding) :
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

    class IrregularPluralViewHolder(private val binding: ItemIrregularPluralBinding) :
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

    class IrregularVerbViewHolder(private val binding: ItemIrregularVerbBinding) :
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
