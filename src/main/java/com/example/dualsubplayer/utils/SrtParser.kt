package com.example.dualsubplayer.utils

import android.content.Context
import android.net.Uri
import java.io.BufferedReader
import java.io.InputStreamReader
import java.lang.StringBuilder

data class SubtitleItem(
    val index: Int,
    val startTimeMs: Long,
    val endTimeMs: Long,
    val text: String,
    var translatedText: String? = null
)

object SrtParser {

    fun parse(context: Context, uri: Uri): List<SubtitleItem> {
        val subtitles = mutableListOf<SubtitleItem>()

        try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return emptyList()
            val reader = BufferedReader(InputStreamReader(inputStream))

            var line: String?
            var currentIndex = 0
            var startTime = 0L
            var endTime = 0L
            val textBuilder = StringBuilder()

            var state = 0

            while (reader.readLine().also { line = it } != null) {
                val trimmed = line!!.trim()

                if (trimmed.isEmpty()) {
                    if (state == 2 && textBuilder.isNotEmpty()) {
                        subtitles.add(SubtitleItem(currentIndex, startTime, endTime, textBuilder.toString().trim()))
                        textBuilder.clear()
                    }
                    state = 0
                    continue
                }

                when (state) {
                    0 -> {
                        currentIndex = trimmed.toIntOrNull() ?: 0
                        state = 1
                    }
                    1 -> {
                        if (trimmed.contains("-->")) {
                            val times = trimmed.split("-->")
                            if (times.size == 2) {
                                startTime = parseTime(times[0].trim())
                                endTime = parseTime(times[1].trim())
                            }
                        }
                        state = 2
                    }
                    2 -> {
                        if (textBuilder.isNotEmpty()) textBuilder.append("\n")
                        textBuilder.append(trimmed)
                    }
                }
            }

            if (state == 2 && textBuilder.isNotEmpty()) {
                subtitles.add(SubtitleItem(currentIndex, startTime, endTime, textBuilder.toString().trim()))
            }

            reader.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return subtitles
    }

    private fun parseTime(timeStr: String): Long {
        return try {
            val parts = timeStr.trim().split(":", ",", ".")
            if (parts.size == 4) {
                val hours = parts[0].toLong()
                val minutes = parts[1].toLong()
                val seconds = parts[2].toLong()
                val millis = parts[3].toLong()
                (hours * 3600000) + (minutes * 60000) + (seconds * 1000) + millis
            } else 0L
        } catch (e: Exception) {
            0L
        }
    }
}