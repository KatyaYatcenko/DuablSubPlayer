package com.example.dualsubplayer.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.dualsubplayer.R
import com.example.dualsubplayer.data.DictionaryWord


class DictionaryWordAdapter(
    private val words: List<DictionaryWord>,
    private val onSpeakClick: (String) -> Unit // Команда для озвучки (динамік)
) : RecyclerView.Adapter<DictionaryWordAdapter.WordViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WordViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_dictionary_word, parent, false)
        return WordViewHolder(view)
    }

    override fun onBindViewHolder(holder: WordViewHolder, position: Int) {
        val word = words[position]
        holder.tvOriginal.text = word.originalWord
        holder.tvTranslation.text = word.translation

        // При натисканні на динамік передаємо англійське слово для озвучки
        holder.btnSpeak.setOnClickListener {
            onSpeakClick(word.originalWord)
        }
    }

    override fun getItemCount() = words.size

    class WordViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvOriginal: TextView = itemView.findViewById(R.id.tvOriginalWord)
        val tvTranslation: TextView = itemView.findViewById(R.id.tvTranslationWord)
        val btnSpeak: ImageButton = itemView.findViewById(R.id.btnSpeak)
    }
}