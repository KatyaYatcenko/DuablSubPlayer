package com.example.dualsubplayer.ui

import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.dualsubplayer.R
import com.example.dualsubplayer.adapter.DictionaryWordAdapter
import com.example.dualsubplayer.data.AppDatabase
import com.example.dualsubplayer.data.DictionaryWord
import com.google.android.material.appbar.MaterialToolbar
import kotlinx.coroutines.launch
import java.util.Locale

class DictionaryWordsFragment : Fragment(), TextToSpeech.OnInitListener {

    private var videoTitle: String = ""
    private var tts: TextToSpeech? = null

    // Створюємо змінну для списку, яку можна змінювати (додавати/видаляти)
    private var wordsList: MutableList<DictionaryWord> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        videoTitle = arguments?.getString("VIDEO_TITLE") ?: getString(R.string.dictionary)
        tts = TextToSpeech(requireContext(), this)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_dictionary_words, container, false)

        val btnBack = view.findViewById<View>(R.id.btnBackWords)
        val rvWords = view.findViewById<RecyclerView>(R.id.rvWordsList)
        val btnStartGame = view.findViewById<Button>(R.id.btnStartGame)

        btnBack.setOnClickListener { parentFragmentManager.popBackStack() }

        rvWords.layoutManager = LinearLayoutManager(requireContext())

        // Завантаження даних
        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(requireContext())
            val words = db.dictionaryDao().getWordsForVideo(videoTitle)

            wordsList = words.toMutableList() // Перетворюємо в MutableList

            val adapter = DictionaryWordAdapter(wordsList) { wordToSpeak ->
                tts?.speak(wordToSpeak, TextToSpeech.QUEUE_FLUSH, null, "")
            }
            rvWords.adapter = adapter

            // --- ДОДАЄМО СВАЙП ДЛЯ ВИДАЛЕННЯ ---
            val swipeHandler = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
                override fun onMove(rv: RecyclerView, vh: RecyclerView.ViewHolder, t: RecyclerView.ViewHolder) = false

                override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                    val position = viewHolder.adapterPosition
                    val wordToDelete = wordsList[position]

                    lifecycleScope.launch {
                        db.dictionaryDao().deleteWord(wordToDelete) // Видаляємо з БД
                        wordsList.removeAt(position) // Видаляємо зі списку
                        rvWords.adapter?.notifyItemRemoved(position) // Оновлюємо UI
                        Toast.makeText(requireContext(), R.string.word_deleted, Toast.LENGTH_SHORT).show()
                    }
                }
            }
            ItemTouchHelper(swipeHandler).attachToRecyclerView(rvWords)
        }

        btnStartGame.setOnClickListener {
            val gameFragment = GameFragment.newInstance(videoTitle)

            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, gameFragment) // Зверни увагу на твій ID контейнера (fragmentContainer)
                .addToBackStack(null)
                .commit()
        }

        return view
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) tts?.language = Locale.US
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }

    companion object {
        fun newInstance(title: String): DictionaryWordsFragment {
            val fragment = DictionaryWordsFragment()
            val args = Bundle()
            args.putString("VIDEO_TITLE", title)
            fragment.arguments = args
            return fragment
        }
    }
}