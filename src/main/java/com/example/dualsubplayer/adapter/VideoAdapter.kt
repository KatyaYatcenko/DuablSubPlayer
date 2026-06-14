package com.example.dualsubplayer.adapter

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.dualsubplayer.R


data class VideoItem(val name: String,
                     val uri: Uri,
                     var externalSubtitleUri: Uri? = null,
                     val embeddedSubtitleLang: String? = null,
                     val thumbnailUrl: String? = null,
                     var isLoadingSubtitles: Boolean = false)

class VideoAdapter(private val onVideoClick: (VideoItem) -> Unit) :
    RecyclerView.Adapter<VideoAdapter.VideoViewHolder>() {

    private val videoList = mutableListOf<VideoItem>()

    fun getVideos(): List<VideoItem> {
        return videoList
    }
    fun addVideo(video: VideoItem) {
        videoList.add(video)
        notifyItemInserted(videoList.size - 1)
    }

    fun removeVideo(position: Int) {
        videoList.removeAt(position)
        notifyItemRemoved(position)
    }

    fun isEmpty() = videoList.isEmpty()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_video, parent, false)
        return VideoViewHolder(view)
    }

    override fun onBindViewHolder(holder: VideoViewHolder, position: Int) {
        val video = videoList[position]
        holder.tvName.text = video.name

        // БЛОКУВАННЯ КЛІКІВ ПІД ЧАС ЗАВАНТАЖЕННЯ
        if (video.isLoadingSubtitles) {
            holder.itemView.alpha = 0.5f // Робимо напівпрозорим
            val loadingClickListener = View.OnClickListener {
                Toast.makeText(holder.itemView.context, "⏳ Завантажуємо субтитри, зачекайте...", Toast.LENGTH_SHORT).show()
            }
            holder.itemView.setOnClickListener(loadingClickListener)
            holder.btnPlay.setOnClickListener(loadingClickListener)
        } else {
            holder.itemView.alpha = 1.0f // Нормальний вигляд
            holder.itemView.setOnClickListener { onVideoClick(video) }
            holder.btnPlay.setOnClickListener { onVideoClick(video) }
        }

        val isYoutube = video.uri.scheme == "youtube"

        if (isYoutube) {
            val videoId = video.uri.host ?: ""

            if (videoId.isNotEmpty()) {
                val thumbnailUrl = "https://img.youtube.com/vi/$videoId/hqdefault.jpg"

                Glide.with(holder.itemView.context)
                    .load(thumbnailUrl)
                    .placeholder(R.drawable.ic_video_placeholder)
                    .centerCrop()
                    .into(holder.ivThumbnail)
            } else {
                holder.ivThumbnail.setImageResource(R.drawable.ic_video_placeholder)
            }
        } else {
            Glide.with(holder.itemView.context)
                .load(video.uri)
                .placeholder(R.drawable.ic_video_placeholder)
                .error(R.drawable.ic_video_placeholder)
                .centerCrop()
                .into(holder.ivThumbnail)
        }
    }

    override fun getItemCount() = videoList.size

    class VideoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvName: TextView = itemView.findViewById(R.id.tvVideoName)
        val ivThumbnail: ImageView = itemView.findViewById(R.id.ivThumbnail)
        val btnPlay: ImageView = itemView.findViewById(R.id.btnPlay)
    }
}