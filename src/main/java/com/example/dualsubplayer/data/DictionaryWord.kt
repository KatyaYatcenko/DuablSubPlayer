package com.example.dualsubplayer.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "dictionary_words")
data class DictionaryWord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val videoId: String,
    val videoTitle: String,
    val originalWord: String,
    val translation: String,
    val correctAnswers: Int = 0
)