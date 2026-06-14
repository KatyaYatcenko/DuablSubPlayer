package com.example.dualsubplayer.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.dualsubplayer.data.DictionarySummary
import com.example.dualsubplayer.R

class DictionaryAdapter(
    private var items: List<DictionarySummary>,
    private val onVideoClick: (String) -> Unit
) : RecyclerView.Adapter<DictionaryAdapter.DictionaryViewHolder>() {

    class DictionaryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvVideoTitle: TextView = view.findViewById(R.id.tvVideoTitle)
        val tvWordCountLabel: TextView = view.findViewById(R.id.tvWordCountLabel)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DictionaryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_dictionary_video, parent, false)
        return DictionaryViewHolder(view)
    }

    override fun onBindViewHolder(holder: DictionaryViewHolder, position: Int) {
        val item = items[position]
        holder.tvVideoTitle.text = item.videoTitle

        val context = holder.itemView.context
        holder.tvWordCountLabel.text = context.getString(R.string.words_count, item.wordCount)

        // Обробка кліку по картці
        holder.itemView.setOnClickListener {
            onVideoClick(item.videoTitle)
        }
    }

    override fun getItemCount(): Int = items.size

    fun updateData(newItems: List<DictionarySummary>) {
        items = newItems
        notifyDataSetChanged()
    }
}