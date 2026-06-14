package com.example.dualsubplayer.network

import io.github.thoroldvix.api.YoutubeClient
import okhttp3.Headers.Companion.toHeaders
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

class OkHttpYoutubeClient(private val client: OkHttpClient = OkHttpClient()) : YoutubeClient {

    override fun get(url: String, headers: Map<String, String>): String {
        val request = Request.Builder()
            .headers(headers.toHeaders())
            .url(url)
            .build()
        return executeRequest(request)
    }

    override fun post(url: String, json: String): String {
        val body = json.toRequestBody("application/json; charset=utf-8".toMediaType())
        val request = Request.Builder()
            .url(url)
            .post(body)
            .build()
        return executeRequest(request)
    }

    private fun executeRequest(request: Request): String {
        try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    return response.body?.string() ?: throw IOException("Response body is null")
                } else {
                    throw IOException("HTTP request failed with code ${response.code}")
                }
            }
        } catch (e: Exception) {
            throw IOException("HTTP request failed", e)
        }
    }
}