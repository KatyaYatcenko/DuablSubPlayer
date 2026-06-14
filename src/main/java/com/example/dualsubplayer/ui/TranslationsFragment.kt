package com.example.dualsubplayer.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.dualsubplayer.R
import com.example.dualsubplayer.adapter.VideoAdapter
import com.example.dualsubplayer.adapter.VideoItem
import com.example.dualsubplayer.data.VideoStorageManager

class TranslationsFragment : Fragment() {

    private lateinit var videoAdapter: VideoAdapter
    private lateinit var rvSavedTranslations: RecyclerView
    private lateinit var emptyTranslationsLayout: LinearLayout

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_translations, container, false)

        rvSavedTranslations = view.findViewById(R.id.rvSavedTranslations)
        emptyTranslationsLayout = view.findViewById(R.id.emptyTranslationsLayout)

        // Налаштовуємо список
        rvSavedTranslations.layoutManager = LinearLayoutManager(requireContext())
        videoAdapter = VideoAdapter { clickedVideo ->
            openPlayer(clickedVideo)
        }
        rvSavedTranslations.adapter = videoAdapter

        // Додаємо свайп для видалення (якщо захочеш почистити історію)
        val swipeHandler = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            override fun onMove(r: RecyclerView, v: RecyclerView.ViewHolder, t: RecyclerView.ViewHolder) = false
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                videoAdapter.removeVideo(viewHolder.adapterPosition)
                updateUI()
                // Зберігаємо оновлений (зменшений) список в пам'ять
                VideoStorageManager.saveVideos(requireContext(), videoAdapter.getVideos())
            }
        }
        ItemTouchHelper(swipeHandler).attachToRecyclerView(rvSavedTranslations)

        return view
    }

    // ВАЖЛИВО: onResume спрацьовує щоразу, коли ти відкриваєш цю вкладку
    override fun onResume() {
        super.onResume()
        loadSavedTranslations()
    }

    private fun loadSavedTranslations() {
        // Очищаємо старий список в адаптері, щоб не було дублікатів
        while (videoAdapter.getVideos().isNotEmpty()) {
            videoAdapter.removeVideo(0)
        }

        // Читаємо свіжі дані з пам'яті телефону
        val savedVideos = VideoStorageManager.loadVideos(requireContext())

        savedVideos.forEach { video ->
            // Скидаємо статус завантаження (якщо він раптом завис при закритті)
            video.isLoadingSubtitles = false
            videoAdapter.addVideo(video)
        }

        updateUI()
    }

    private fun updateUI() {
        if (videoAdapter.isEmpty()) {
            emptyTranslationsLayout.visibility = View.VISIBLE
            rvSavedTranslations.visibility = View.GONE
        } else {
            emptyTranslationsLayout.visibility = View.GONE
            rvSavedTranslations.visibility = View.VISIBLE
        }
    }

    private fun openPlayer(video: VideoItem) {
        val intent = Intent(requireContext(), SubtitleReaderActivity::class.java).apply {
            putExtra("VIDEO_NAME", video.name)

            if (video.externalSubtitleUri != null) {
                putExtra("SUBTITLE_URI", video.externalSubtitleUri.toString())
            }
        }
        startActivity(intent)
    }
}