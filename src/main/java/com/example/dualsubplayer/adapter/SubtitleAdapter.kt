package com.example.dualsubplayer.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.dualsubplayer.R
import com.example.dualsubplayer.utils.SubtitleItem
import java.util.concurrent.TimeUnit

class SubtitleAdapter(
    private val subtitles: List<SubtitleItem>,
    private val onSubtitleClick: (Long) -> Unit,          // 1. Команда для перемотки
    private val onRetranslateClick: (Int) -> Unit,        // 2. Команда для перекладу
    private val onWordSelectClick: (SubtitleItem) -> Unit // 3. НОВА: виклик модалки зі словами
) : RecyclerView.Adapter<SubtitleAdapter.SubtitleViewHolder>() {

    private var activePosition: Int = -1

    fun setActivePosition(position: Int) {
        if (activePosition != position) {
            val oldPosition = activePosition
            activePosition = position
            notifyItemChanged(oldPosition)
            notifyItemChanged(activePosition)
        }
    }

    fun getActivePosition(): Int {
        return activePosition
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SubtitleViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_dual_subtitle, parent, false)
        return SubtitleViewHolder(view)
    }

    override fun onBindViewHolder(holder: SubtitleViewHolder, position: Int) {
        val item = subtitles[position]

        holder.tvOriginal.text = item.text

        val minutes = TimeUnit.MILLISECONDS.toMinutes(item.startTimeMs)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(item.startTimeMs) - TimeUnit.MINUTES.toSeconds(minutes)
        holder.tvTime.text = String.format("%02d:%02d", minutes, seconds)

        if (item.translatedText != null) {
            holder.tvTranslation.text = item.translatedText
        } else {
            holder.tvTranslation.text = holder.itemView.context.getString(R.string.translate_processing)
        }

        val cardView = holder.itemView as com.google.android.material.card.MaterialCardView
        if (position == activePosition) {
            cardView.setCardBackgroundColor(Color.parseColor("#4DFF9800"))
        } else {
            cardView.setCardBackgroundColor(Color.TRANSPARENT)
        }

        // --- МАГІЯ КЛІКІВ ДЛЯ НОВИХ ЗОН ---

        // 1. Короткий клік по лівій частині (Текст) -> Відкриваємо модалку словника
        holder.layoutLeftWords.setOnClickListener {
            onWordSelectClick(item)
        }

        // 1.1 Довгий клік по лівій частині -> Перемотуємо відео (щоб не втратити цю функцію)
        holder.layoutLeftWords.setOnLongClickListener {
            onSubtitleClick(item.startTimeMs)
            true // Повертаємо true, бо ми обробили довгий клік
        }

        // 2. Клік по правій частині (Час + Іконка) -> Оновлюємо переклад
        holder.layoutRightRefresh.setOnClickListener {
            onRetranslateClick(position)
        }
    }

    override fun getItemCount() = subtitles.size

    class SubtitleViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        val layoutLeftWords: LinearLayout = itemView.findViewById(R.id.layoutLeftWords)
        val layoutRightRefresh: LinearLayout = itemView.findViewById(R.id.layoutRightRefresh)

        val tvOriginal: TextView = itemView.findViewById(R.id.tvOriginal)
        val tvTranslation: TextView = itemView.findViewById(R.id.tvTranslation)
        val tvTime: TextView = itemView.findViewById(R.id.tvTime)
        val btnAction: ImageView = itemView.findViewById(R.id.btnAction)
    }
}