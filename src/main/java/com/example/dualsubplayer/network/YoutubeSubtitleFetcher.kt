package com.example.dualsubplayer.network

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import java.io.File
import java.util.concurrent.Executors

import io.github.thoroldvix.api.TranscriptFormatters
import io.github.thoroldvix.api.TranscriptApiFactory

object YoutubeSubtitleFetcher {

    private val mainHandler = Handler(Looper.getMainLooper())
    private val executor = Executors.newSingleThreadExecutor()

    private val youtubeClient = OkHttpYoutubeClient()
    private val transcriptApi = TranscriptApiFactory.createWithClient(youtubeClient)

    fun extractVideoId(url: String): String {
        val regex = Regex("(?:v=|youtu.be/)([a-zA-Z0-9_-]{11})")
        return regex.find(url)?.groupValues?.get(1) ?: ""
    }

    fun getSubtitlesUri(context: Context, videoId: String, lang: String = "en", onResult: (Uri?) -> Unit) {
        Log.d("YOUTUBE_SUBS", "1. Звертаємось до бібліотеки для відео: $videoId")

        executor.execute {
            try {
                // Бібліотека сама шукає і витягує субтитри!
                val transcriptContent = transcriptApi.getTranscript(videoId, lang)

                // Використовуємо вбудований у бібліотеку конвертер в SRT
                val srtFormatter = TranscriptFormatters.srtFormatter()
                val srtText = srtFormatter.format(transcriptContent)

                // Зберігаємо файл
                val cacheFile = File(context.cacheDir, "DualSub_${videoId}_$lang.srt")
                cacheFile.writeText(srtText)

                Log.d("YOUTUBE_SUBS", "2. УСПІХ! Файл створено: ${cacheFile.absolutePath}")
                showToast(context, "✅ Субтитри завантажено!")

                // Повертаємо файл у головний потік
                mainHandler.post { onResult(Uri.fromFile(cacheFile)) }

            } catch (e: Exception) {
                Log.e("YOUTUBE_SUBS", "❌ Помилка бібліотеки: ${e.message}", e)
                showToast(context, "⚠️ Помилка завантаження субтитрів")
                mainHandler.post { onResult(null) }
            }
        }
    }

    private fun showToast(context: Context, message: String) {
        mainHandler.post { Toast.makeText(context, message, Toast.LENGTH_SHORT).show() }
    }
}