package com.example.dualsubplayer.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

data class DictionarySummary(
    val videoTitle: String,
    val wordCount: Int
)

@Dao
interface DictionaryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWord(word: DictionaryWord): Long

    @Delete
    suspend fun deleteWord(word: DictionaryWord): Int

    // ОНОВЛЕНИЙ ЗАПИТ: Рахуємо реальну кількість слів за кількістю пробілів
    @Query("""
        SELECT videoTitle, 
               SUM(LENGTH(originalWord) - LENGTH(REPLACE(originalWord, ' ', '')) + 1) as wordCount 
        FROM dictionary_words 
        GROUP BY videoTitle
    """)
    suspend fun getDictionarySummary(): List<DictionarySummary>

    @Query("SELECT * FROM dictionary_words WHERE videoTitle = :title")
    suspend fun getWordsForVideo(title: String): List<DictionaryWord>

    // --- НОВИЙ МЕТОД ---
    // Отримує список усіх збережених слів для перевірки на дублікати
    @Query("SELECT originalWord FROM dictionary_words")
    suspend fun getAllSavedOriginalWords(): List<String>
}