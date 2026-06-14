package com.example.dualsubplayer.ui

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.TextView
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.fragment.app.Fragment
import com.example.dualsubplayer.R
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class SettingsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_settings, container, false)

        val cardLanguage = view.findViewById<View>(R.id.cardLanguage)
        val tvCurrentLanguage = view.findViewById<TextView>(R.id.tvCurrentLanguage)
        val tvAppVersion = view.findViewById<TextView>(R.id.tvAppVersion)

        // 1. ВСТАНОВЛЮЄМО ВЕРСІЮ ДОДАТКУ ДИНАМІЧНО
        try {
            val pInfo = requireContext().packageManager.getPackageInfo(requireContext().packageName, 0)
            val version = pInfo.versionName
            tvAppVersion.text = getString(R.string.app_version, version)
        } catch (e: PackageManager.NameNotFoundException) {
            tvAppVersion.text = getString(R.string.app_version, "1.0.0")
        }

        // 2. ПОКАЗУЄМО ПОТОЧНУ МОВУ
        val currentLocales = AppCompatDelegate.getApplicationLocales()
        tvCurrentLanguage.text = if (currentLocales.isEmpty) {
            getString(R.string.lang_system)
        } else {
            // Беремо тег (наприклад "uk") і робимо з нього красиву назву
            val tag = currentLocales.toLanguageTags()
            when {
                tag.contains("uk") -> "Українська"
                tag.contains("en") -> "English"
                tag.contains("es") -> "Español"
                tag.contains("fr") -> "Français"
                tag.contains("de") -> "Deutsch"
                tag.contains("pl") -> "Polski"
                else -> tag
            }
        }

        // 3. ОБРОБКА КЛІКУ НА ЗМІНУ МОВИ
        cardLanguage.setOnClickListener {
            showLanguageDialog(tvCurrentLanguage)
        }

        return view
    }

    private fun showLanguageDialog(tvCurrentLanguage: TextView) {
        // Додай сюди інші мови, якщо плануєш перекладати додаток (strings.xml)
        val languages = arrayOf(getString(R.string.lang_system), "Українська", "English", "Español", "Français", "Deutsch", "Polski")
        val tags = arrayOf("", "uk", "en", "es", "fr", "de", "pl")

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.choose_language))
            .setItems(languages) { _, which ->
                val selectedTag = tags[which]

                val appLocale: LocaleListCompat = if (selectedTag.isEmpty()) {
                    LocaleListCompat.getEmptyLocaleList()
                } else {
                    LocaleListCompat.forLanguageTags(selectedTag)
                }
                // Ця команда перезавантажить екрани з новою мовою
                AppCompatDelegate.setApplicationLocales(appLocale)
            }
            .create()
            
        dialog.window?.setBackgroundDrawableResource(R.drawable.bg_glass_dialog)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            dialog.window?.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
            dialog.window?.attributes?.blurBehindRadius = 64
        }
        dialog.show()
    }
}