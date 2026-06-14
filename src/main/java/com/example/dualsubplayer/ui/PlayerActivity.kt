package com.example.dualsubplayer.ui

import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
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
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import kotlinx.coroutines.launch

class PlayerActivity : AppCompatActivity() {

    // --- ПЛЕЄРИ ---
    private var player: ExoPlayer? = null
    private lateinit var playerView: PlayerView
    private lateinit var youtubePlayerView: YouTubePlayerView
    private var ytPlayer: YouTubePlayer? = null

    private var isYouTube = false
    private var youtubeId: String = ""
    private var youtubeCurrentTimeMs = 0L
    private var currentVideoName: String = "Відео"

    // --- СУБТИТРИ ---
    private lateinit var rvSubtitles: RecyclerView
    private var subtitlesList: MutableList<SubtitleItem> = mutableListOf()
    private var subtitleAdapter: SubtitleAdapter? = null

    // --- ПЕРЕКЛАДАЧ ---
    private var activeTranslator: Translator? = null
    private var isTranslationRunning = false

    // --- СИНХРОНІЗАЦІЯ ---
    private val syncHandler = Handler(Looper.getMainLooper())
    private val syncRunnable = object : Runnable {
        override fun run() {
            syncSubtitles()
            syncHandler.postDelayed(this, 250)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_player)

        val btnBack = findViewById<View>(R.id.btnBackPlayer)
        val tvTitle = findViewById<android.widget.TextView>(R.id.tvPlayerTitle)
        val btnTranslate = findViewById<View>(R.id.btnTranslatePlayer)
        
        playerView = findViewById(R.id.playerView)
        youtubePlayerView = findViewById(R.id.youtubePlayerView)
        rvSubtitles = findViewById(R.id.rvSubtitles)
        rvSubtitles.layoutManager = LinearLayoutManager(this)

        currentVideoName = intent.getStringExtra("VIDEO_NAME") ?: "Відео"
        val videoUriString = intent.getStringExtra("VIDEO_URI")
        val subtitleUriString = intent.getStringExtra("SUBTITLE_URI")
        isYouTube = intent.getBooleanExtra("IS_YOUTUBE", false)
        youtubeId = intent.getStringExtra("YOUTUBE_ID")?.trim() ?: ""

        tvTitle.text = currentVideoName
        btnBack.setOnClickListener { finish() }

        btnTranslate.setOnClickListener {
            showLanguageDialog()
        }

        if (subtitleUriString != null) {
            loadSubtitles(Uri.parse(subtitleUriString))
            showLanguageDialog()
        }

        if (isYouTube && youtubeId.isNotEmpty()) {
            initYouTubePlayer()
        } else if (videoUriString != null) {
            initExoPlayer(Uri.parse(videoUriString))
        }
    }

    private fun initYouTubePlayer() {
        playerView.visibility = View.GONE
        youtubePlayerView.visibility = View.VISIBLE
        lifecycle.addObserver(youtubePlayerView)

        youtubePlayerView.addYouTubePlayerListener(object : AbstractYouTubePlayerListener() {
            override fun onReady(youTubePlayer: YouTubePlayer) {
                ytPlayer = youTubePlayer
                youTubePlayer.loadVideo(youtubeId, 0f)
                syncHandler.post(syncRunnable)
            }
            override fun onCurrentSecond(youTubePlayer: YouTubePlayer, second: Float) {
                youtubeCurrentTimeMs = (second * 1000).toLong()
            }
        })
    }

    private fun initExoPlayer(videoUri: Uri) {
        youtubePlayerView.visibility = View.GONE
        playerView.visibility = View.VISIBLE
        player = ExoPlayer.Builder(this).build().apply {
            setMediaItem(MediaItem.fromUri(videoUri))
            prepare()
            play()
        }
        playerView.player = player
        syncHandler.post(syncRunnable)
    }

    private fun loadSubtitles(uri: Uri) {
        try {
            subtitlesList = SrtParser.parse(this, uri).toMutableList()
            subtitleAdapter = SubtitleAdapter(
                subtitlesList,
                onSubtitleClick = { timeMs ->
                    if (isYouTube) ytPlayer?.seekTo(timeMs / 1000f) else player?.seekTo(timeMs)
                },
                onRetranslateClick = { position ->
                    retranslateSingleLine(position)
                },
                onWordSelectClick = { subtitleItem ->
                    showDictionaryBottomSheet(subtitleItem)
                }
            )
            rvSubtitles.adapter = subtitleAdapter
        } catch (e: Exception) {
            Log.e("PLAYER_ERR", "Помилка парсингу SRT: ${e.message}")
        }
    }

    private fun showDictionaryBottomSheet(item: SubtitleItem) {
        if (isYouTube) {
            ytPlayer?.pause()
        } else {
            player?.pause()
        }

        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(this@PlayerActivity)

            // 1. СУПЕР-СКАНЕР: розбиваємо збережені фрази на окремі слова для ідеального пошуку дублікатів
            val savedWords = db.dictionaryDao().getAllSavedOriginalWords()
                .flatMap { it.split(Regex("[\\s\\p{Punct}]+")) }
                .map { it.lowercase() }
                .filter { it.isNotBlank() }

            val dialog = BottomSheetDialog(this@PlayerActivity)
            val view = layoutInflater.inflate(R.layout.dialog_bottom_sheet_dictionary, null)
            dialog.setContentView(view)

            dialog.setOnDismissListener {
                if (isYouTube) ytPlayer?.play() else player?.play()
            }

            val chipGroupOriginal = view.findViewById<ChipGroup>(R.id.chipGroupOriginal)
            val chipGroupTranslated = view.findViewById<ChipGroup>(R.id.chipGroupTranslated)
            val btnSave = view.findViewById<Button>(R.id.btnSaveToDictionary)

            // 2. Створюємо чіпи для перекладу
            val translatedText = item.translatedText ?: ""
            val translatedWords = translatedText.split(Regex("[\\s\\p{Punct}]+")).filter { it.isNotBlank() }

            translatedWords.forEach { word ->
                val chip = Chip(this@PlayerActivity).apply {
                    text = word
                    isCheckable = true
                    isClickable = true
                }
                chipGroupTranslated.addView(chip)
            }

            // 3. Створюємо чіпи оригіналу
            val originalWords = item.text.split(Regex("[\\s\\p{Punct}]+")).filter { it.isNotBlank() }

            originalWords.forEachIndexed { index, word ->
                val chip = Chip(this@PlayerActivity).apply {
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
                                videoId = if (isYouTube) youtubeId else "local_video",
                                videoTitle = currentVideoName,
                                originalWord = originalText,
                                translation = translationText
                            )
                            db.dictionaryDao().insertWord(wordToSave)
                        }

                        Toast.makeText(this@PlayerActivity, R.string.words_added_to_dict, Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                    }
                } else {
                    Toast.makeText(this@PlayerActivity, R.string.choose_at_least_one_word, Toast.LENGTH_SHORT).show()
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

    private fun syncSubtitles() {
        if (subtitleAdapter == null || subtitlesList.isEmpty()) return
        val currentPos = if (isYouTube) youtubeCurrentTimeMs else (player?.currentPosition ?: 0L)
        val index = subtitlesList.indexOfFirst { currentPos in it.startTimeMs..it.endTimeMs }

        if (index != -1 && index != subtitleAdapter?.getActivePosition()) {
            subtitleAdapter?.setActivePosition(index)
            rvSubtitles.smoothScrollToPosition(index)
        }
    }

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
                    // System default selected, extract text
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
                if (sourceLang == targetLangCode) {
                    Toast.makeText(this, R.string.subs_already_this_lang, Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }
                startTranslation(sourceLang, targetLangCode)
            }
            .addOnFailureListener {
                startTranslation("en", targetLangCode)
            }
    }

    private fun startTranslation(sourceLang: String, targetLang: String) {
        val options = TranslatorOptions.Builder()
            .setSourceLanguage(sourceLang)
            .setTargetLanguage(targetLang)
            .build()

        activeTranslator = Translation.getClient(options)
        activeTranslator?.downloadModelIfNeeded()
            ?.addOnSuccessListener {
                isTranslationRunning = true
                translateLine(0)
            }
            ?.addOnFailureListener {
                Toast.makeText(this, R.string.error_lang_pack, Toast.LENGTH_SHORT).show()
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
            ?.addOnFailureListener {
                translateLine(index + 1)
            }
    }

    private fun retranslateSingleLine(position: Int) {
        if (activeTranslator == null) {
            Toast.makeText(this, R.string.choose_translation_lang, Toast.LENGTH_SHORT).show()
            return
        }
        val item = subtitlesList[position]
        item.translatedText = getString(R.string.translating)
        subtitleAdapter?.notifyItemChanged(position)

        activeTranslator?.translate(item.text)
            ?.addOnSuccessListener { newText ->
                item.translatedText = newText
                subtitleAdapter?.notifyItemChanged(position)
            }
            ?.addOnFailureListener {
                item.translatedText = getString(R.string.translation_error)
                subtitleAdapter?.notifyItemChanged(position)
            }
    }

    override fun onDestroy() {
        super.onDestroy()
        syncHandler.removeCallbacks(syncRunnable)
        player?.release()
        youtubePlayerView.release()
        isTranslationRunning = false
        activeTranslator?.close()
    }
}