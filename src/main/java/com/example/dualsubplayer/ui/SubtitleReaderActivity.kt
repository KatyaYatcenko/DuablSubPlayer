package com.example.dualsubplayer.ui

import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.dualsubplayer.R
import com.example.dualsubplayer.adapter.SubtitleAdapter
import com.example.dualsubplayer.data.AppDatabase
import com.example.dualsubplayer.data.DictionaryWord
import com.example.dualsubplayer.utils.SrtParser
import com.example.dualsubplayer.utils.SubtitleItem
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.launch

class SubtitleReaderActivity : AppCompatActivity() {

    private lateinit var rvSubtitles: RecyclerView
    private var subtitlesList: MutableList<SubtitleItem> = mutableListOf()
    private var subtitleAdapter: SubtitleAdapter? = null
    private var currentVideoName: String = "Текст відео"

    // Перекладач
    private var activeTranslator: Translator? = null
    private var isTranslationRunning = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_subtitle_reader)

        val btnBack = findViewById<View>(R.id.btnBackReader)
        val tvTitle = findViewById<android.widget.TextView>(R.id.tvReaderTitle)
        val btnTranslate = findViewById<View>(R.id.btnTranslateReader)

        rvSubtitles = findViewById(R.id.rvReaderSubtitles)
        rvSubtitles.layoutManager = LinearLayoutManager(this)

        // Додаємо роздільник між рядками
        val divider = DividerItemDecoration(this, LinearLayoutManager.VERTICAL)
        rvSubtitles.addItemDecoration(divider)

        currentVideoName = intent.getStringExtra("VIDEO_NAME") ?: getString(R.string.video_text)
        val subtitleUriString = intent.getStringExtra("SUBTITLE_URI")

        tvTitle.text = currentVideoName
        btnBack.setOnClickListener { finish() }

        // Кнопка перекладу в панелі
        btnTranslate.setOnClickListener {
            showLanguageDialog()
        }

        if (subtitleUriString != null) {
            loadSubtitles(Uri.parse(subtitleUriString))
            showLanguageDialog()
        } else {
            Toast.makeText(this, R.string.text_not_found, Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadSubtitles(uri: Uri) {
        try {
            subtitlesList = SrtParser.parse(this, uri).toMutableList()

            subtitleAdapter = SubtitleAdapter(
                subtitlesList,
                onSubtitleClick = {
                    // У режимі читання довгий клік по рядку нічого не робить (бо немає плеєра)
                },
                onRetranslateClick = { position ->
                    retranslateSingleLine(position)
                },
                onWordSelectClick = { subtitleItem ->
                    // ВИКЛИКАЄМО МОДАЛКУ СЛОВНИКА
                    showDictionaryBottomSheet(subtitleItem)
                }
            )
            rvSubtitles.adapter = subtitleAdapter

        } catch (e: Exception) {
            Log.e("READER_ERR", "Помилка парсингу: ${e.message}")
            Toast.makeText(this, R.string.file_read_error, Toast.LENGTH_SHORT).show()
        }
    }

    // ==========================================
    // ЛОГІКА СЛОВНИКА (Модальне вікно)
    // ==========================================
    private fun showDictionaryBottomSheet(item: SubtitleItem) {

        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(this@SubtitleReaderActivity)

            val savedWords = db.dictionaryDao().getAllSavedOriginalWords()
                .flatMap { it.split(Regex("[\\s\\p{Punct}]+")) }
                .map { it.lowercase() }
                .filter { it.isNotBlank() }

            val dialog = BottomSheetDialog(this@SubtitleReaderActivity)
            val view = layoutInflater.inflate(R.layout.dialog_bottom_sheet_dictionary, null)
            dialog.setContentView(view)


            val chipGroupOriginal = view.findViewById<ChipGroup>(R.id.chipGroupOriginal)
            val chipGroupTranslated = view.findViewById<ChipGroup>(R.id.chipGroupTranslated)
            val btnSave = view.findViewById<Button>(R.id.btnSaveToDictionary)

            // 2. Створюємо чіпи для перекладу
            val translatedText = item.translatedText ?: ""
            val translatedWords = translatedText.split(Regex("[\\s\\p{Punct}]+")).filter { it.isNotBlank() }

            translatedWords.forEach { word ->
                val chip = Chip(this@SubtitleReaderActivity).apply {
                    text = word
                    isCheckable = true
                    isClickable = true
                }
                chipGroupTranslated.addView(chip)
            }

            // 3. Створюємо чіпи оригіналу
            val originalWords = item.text.split(Regex("[\\s\\p{Punct}]+")).filter { it.isNotBlank() }

            originalWords.forEachIndexed { index, word ->
                val chip = Chip(this@SubtitleReaderActivity).apply {
                    text = word
                    isCheckable = true
                    isClickable = true
                    chipStrokeWidth = 3f
                }

                if (savedWords.contains(word.lowercase())) {
                    chip.setTextColor(Color.RED)
                    chip.chipStrokeColor = ColorStateList.valueOf(Color.RED)
                    chip.isEnabled = false
                } else {
                    val greenColor = Color.parseColor("#4CAF50")
                    chip.setTextColor(greenColor)
                    chip.chipStrokeColor = ColorStateList.valueOf(greenColor)

                    chip.setOnCheckedChangeListener { _, isChecked ->
                        val translationChip = chipGroupTranslated.getChildAt(index) as? Chip
                        translationChip?.isChecked = isChecked
                    }
                }
                chipGroupOriginal.addView(chip)
            }

            // 4. Збереження КОЖНОГО слова окремим записом
            btnSave.setOnClickListener {
                // Отримуємо списки вибраних ID
                val selectedOriginalIds = chipGroupOriginal.checkedChipIds
                val selectedTranslationIds = chipGroupTranslated.checkedChipIds

                if (selectedOriginalIds.isNotEmpty()) {
                    lifecycleScope.launch {
                        // Проходимо циклом по всіх вибраних оригіналах
                        selectedOriginalIds.forEachIndexed { i, id ->
                            val originalText = chipGroupOriginal.findViewById<Chip>(id).text.toString()

                            // Беремо відповідний переклад за індексом, якщо він є
                            val translationText = if (i < selectedTranslationIds.size) {
                                chipGroupTranslated.findViewById<Chip>(selectedTranslationIds[i]).text.toString()
                            } else {
                                "?" // Заглушка, якщо переклад не вибрано
                            }

                            val wordToSave = DictionaryWord(
                                videoId = "local_video",
                                videoTitle = currentVideoName,
                                originalWord = originalText,
                                translation = translationText
                            )
                            db.dictionaryDao().insertWord(wordToSave)
                        }

                        Toast.makeText(this@SubtitleReaderActivity, R.string.words_added_to_dict, Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                    }
                } else {
                    Toast.makeText(this@SubtitleReaderActivity, R.string.choose_at_least_one_word, Toast.LENGTH_SHORT).show()
                }
            }
            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                dialog.window?.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
                dialog.window?.attributes?.blurBehindRadius = 64
            }
            dialog.show()
        }
    }

    // ==========================================
    // ЛОГІКА ПЕРЕКЛАДУ
    // ==========================================
    private fun showLanguageDialog() {
        val languages = arrayOf(getString(R.string.lang_system), "Українська", "English", "Español", "Français", "Deutsch", "Polski")
        val codes = arrayOf("", "uk", "en", "es", "fr", "de", "pl")

        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.choose_translation_lang))
            .setCancelable(true)
            .setItems(languages) { _, which ->
                subtitlesList.forEach { it.translatedText = getString(R.string.waiting) }
                subtitleAdapter?.notifyDataSetChanged()

                isTranslationRunning = false
                activeTranslator?.close()
                activeTranslator = null

                if (codes[which].isEmpty()) {
                    // System language
                } else {
                    Toast.makeText(this, R.string.preparing_translation, Toast.LENGTH_SHORT).show()
                    Handler(Looper.getMainLooper()).postDelayed({
                        startTranslationProcess(codes[which])
                    }, 500)
                }
            }.create()
            
        dialog.window?.setBackgroundDrawableResource(R.drawable.bg_glass_dialog)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            dialog.window?.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
            dialog.window?.attributes?.blurBehindRadius = 64
        }
        dialog.show()
    }

    private fun startTranslationProcess(targetLangCode: String) {
        val languageIdentifier = LanguageIdentification.getClient()
        val sampleText = subtitlesList.take(5).joinToString(" ") { it.text }

        languageIdentifier.identifyLanguage(sampleText)
            .addOnSuccessListener { languageCode ->
                val sourceLang = if (languageCode == "und") "en" else languageCode
                if (sourceLang == targetLangCode) return@addOnSuccessListener
                startTranslation(sourceLang, targetLangCode)
            }
            .addOnFailureListener {
                startTranslation("en", targetLangCode)
            }
    }

    private fun startTranslation(sourceLang: String, targetLang: String) {
        val options = TranslatorOptions.Builder()
            .setSourceLanguage(sourceLang).setTargetLanguage(targetLang).build()

        activeTranslator = Translation.getClient(options)
        activeTranslator?.downloadModelIfNeeded()
            ?.addOnSuccessListener {
                isTranslationRunning = true
                translateLine(0)
            }
    }

    private fun translateLine(index: Int) {
        if (index >= subtitlesList.size || !isTranslationRunning) return
        activeTranslator?.translate(subtitlesList[index].text)
            ?.addOnSuccessListener { translatedText ->
                subtitlesList[index].translatedText = translatedText
                subtitleAdapter?.notifyItemChanged(index)
                translateLine(index + 1)
            }
            ?.addOnFailureListener { translateLine(index + 1) }
    }

    private fun retranslateSingleLine(position: Int) {
        if (activeTranslator == null) return
        val item = subtitlesList[position]
        item.translatedText = getString(R.string.translating)
        subtitleAdapter?.notifyItemChanged(position)

        activeTranslator?.translate(item.text)
            ?.addOnSuccessListener { newText ->
                item.translatedText = newText
                subtitleAdapter?.notifyItemChanged(position)
            }
    }

    override fun onDestroy() {
        super.onDestroy()
        isTranslationRunning = false
        activeTranslator?.close()
    }
}