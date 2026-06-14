package com.example.dualsubplayer.ui

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.dualsubplayer.R
import com.example.dualsubplayer.data.AppDatabase
import com.example.dualsubplayer.data.DictionaryWord
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class GameFragment : Fragment() {

    private var videoTitle: String = ""
    private var wordsList: List<DictionaryWord> = listOf()
    private var currentIndex = 0
    private var score = 0

    private lateinit var tvProgress: TextView
    private lateinit var tvQuestionWord: TextView
    private lateinit var buttons: List<Button>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        videoTitle = arguments?.getString("VIDEO_TITLE") ?: ""
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_game, container, false)

        val btnBack = view.findViewById<View>(R.id.btnBackGame)
        tvProgress = view.findViewById(R.id.tvGameProgress)
        tvQuestionWord = view.findViewById(R.id.tvGameQuestionWord)

        buttons = listOf(
            view.findViewById(R.id.btnOption1),
            view.findViewById(R.id.btnOption2),
            view.findViewById(R.id.btnOption3),
            view.findViewById(R.id.btnOption4)
        )

        btnBack.setOnClickListener { parentFragmentManager.popBackStack() }

        startGame()

        return view
    }

    private fun startGame() {
        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(requireContext())
            // Беремо слова і перемішуємо їх для гри
            wordsList = db.dictionaryDao().getWordsForVideo(videoTitle).shuffled()

            if (wordsList.size < 4) {
                Toast.makeText(requireContext(), R.string.game_min_words_error, Toast.LENGTH_LONG).show()
                parentFragmentManager.popBackStack() // Виходимо, якщо слів мало
                return@launch
            }

            currentIndex = 0
            score = 0
            showNextQuestion()
        }
    }

    private fun showNextQuestion() {
        if (currentIndex >= wordsList.size) {
            showGameOverDialog()
            return
        }

        val currentWord = wordsList[currentIndex]
        tvProgress.text = "${currentIndex + 1} / ${wordsList.size}"
        tvQuestionWord.text = currentWord.originalWord

        // Збираємо 3 НЕПРАВИЛЬНІ переклади з інших слів цього відео
        val wrongAnswers = wordsList.filter { it.originalWord != currentWord.originalWord }
            .shuffled()
            .take(3)
            .map { it.translation }

        // Додаємо правильний переклад і перемішуємо всі 4 варіанти
        val options = (wrongAnswers + currentWord.translation).shuffled()

        // Налаштовуємо кнопки
        buttons.forEachIndexed { index, button ->
            button.text = options[index]
            button.backgroundTintList = null // Скидаємо колір до стандартного
            button.isEnabled = true
            button.setOnClickListener { checkAnswer(button, currentWord.translation) }
        }
    }

    private fun checkAnswer(clickedButton: Button, correctAnswer: String) {
        // Блокуємо всі кнопки, щоб не клікали двічі
        buttons.forEach { it.isEnabled = false }

        if (clickedButton.text == correctAnswer) {
            // ПРАВИЛЬНО - Зелений
            clickedButton.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#4CAF50"))
            score++
        } else {
            // НЕПРАВИЛЬНО - Червоний
            clickedButton.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#F44336"))
            // Одразу підсвічуємо зеленою кнопку з правильною відповіддю
            buttons.find { it.text == correctAnswer }?.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#4CAF50"))
        }

        // Чекаємо 1.5 секунди і показуємо наступне слово
        Handler(Looper.getMainLooper()).postDelayed({
            currentIndex++
            showNextQuestion()
        }, 1500)
    }

    private fun showGameOverDialog() {
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.game_over_title)
            .setMessage(getString(R.string.game_over_msg, score, wordsList.size))
            .setCancelable(false)
            .setPositiveButton(R.string.super_btn) { _, _ ->
                parentFragmentManager.popBackStack() // Повертаємось у словник
            }
            .create()
            
        dialog.window?.setBackgroundDrawableResource(R.drawable.bg_glass_dialog)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            dialog.window?.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
            dialog.window?.attributes?.blurBehindRadius = 64
        }
        dialog.show()
    }

    companion object {
        fun newInstance(title: String): GameFragment {
            val fragment = GameFragment()
            val args = Bundle()
            args.putString("VIDEO_TITLE", title)
            fragment.arguments = args
            return fragment
        }
    }
}