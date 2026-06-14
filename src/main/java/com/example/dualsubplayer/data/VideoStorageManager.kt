package com.example.dualsubplayer.data

import android.content.Context
import android.net.Uri
import com.example.dualsubplayer.adapter.VideoItem
import com.google.gson.*
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type

// Вчимо Gson розуміти формат Uri
class UriAdapter : JsonSerializer<Uri>, JsonDeserializer<Uri> {
    override fun serialize(src: Uri, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
        return JsonPrimitive(src.toString())
    }

    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): Uri {
        return Uri.parse(json.asString)
    }
}

object VideoStorageManager {
    private const val PREFS_NAME = "DualSubPrefs"
    private const val KEY_VIDEOS = "saved_videos_list"

    private val gson: Gson = GsonBuilder()
        .registerTypeAdapter(Uri::class.java, UriAdapter())
        .create()

    fun saveVideos(context: Context, videos: List<VideoItem>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = gson.toJson(videos)
        prefs.edit().putString(KEY_VIDEOS, json).apply()
    }

    fun loadVideos(context: Context): List<VideoItem> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_VIDEOS, null) ?: return emptyList()
        val type = object : TypeToken<List<VideoItem>>() {}.type
        return gson.fromJson(json, type)
    }
}