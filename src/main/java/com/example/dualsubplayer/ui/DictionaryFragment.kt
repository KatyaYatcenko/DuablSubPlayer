package com.example.dualsubplayer.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.dualsubplayer.data.AppDatabase
import com.example.dualsubplayer.adapter.DictionaryAdapter
import com.example.dualsubplayer.data.DictionarySummary
import com.example.dualsubplayer.R
import kotlinx.coroutines.launch

class DictionaryFragment : Fragment() {

    private lateinit var rvDictionary: RecyclerView
    private lateinit var emptyLayout: LinearLayout

    private lateinit var db: AppDatabase
    private lateinit var adapter: DictionaryAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_dictionary, container, false)

        rvDictionary = view.findViewById(R.id.rvDictionary)
        emptyLayout = view.findViewById(R.id.emptyDictionaryLayout)

        // Ініціалізуємо Базу Даних
        db = AppDatabase.Companion.getDatabase(requireContext())

        // Налаштовуємо список
        rvDictionary.layoutManager = LinearLayoutManager(requireContext())

        // Ініціалізуємо Адаптер
        adapter = DictionaryAdapter(emptyList()) { videoTitle ->
            // Відкриваємо новий фрагмент зі словами
            val fragment = DictionaryWordsFragment.newInstance(videoTitle)

            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .addToBackStack(null)
                .commit()
        }
        rvDictionary.adapter = adapter

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Запит до бази даних
        viewLifecycleOwner.lifecycleScope.launch {
            val summaries = db.dictionaryDao().getDictionarySummary()
            updateUI(summaries)
        }
    }

    // Показуємо список АБО екран-заглушку "Словник порожній"
    private fun updateUI(summaries: List<DictionarySummary>) {
        if (summaries.isEmpty()) {
            rvDictionary.visibility = View.GONE
            emptyLayout.visibility = View.VISIBLE
        } else {
            rvDictionary.visibility = View.VISIBLE
            emptyLayout.visibility = View.GONE

            // Передаємо нові дані в адаптер
            adapter.updateData(summaries)
        }
    }
}