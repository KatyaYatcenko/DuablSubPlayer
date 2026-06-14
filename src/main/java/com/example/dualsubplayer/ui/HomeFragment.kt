package com.example.dualsubplayer.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.dualsubplayer.R
import com.example.dualsubplayer.adapter.VideoAdapter
import com.example.dualsubplayer.adapter.VideoItem
import com.example.dualsubplayer.data.VideoStorageManager
import com.example.dualsubplayer.network.YoutubeSubtitleFetcher
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import org.json.JSONObject
import okhttp3.*
import java.io.IOException

class HomeFragment : Fragment() {

    private lateinit var videoAdapter: VideoAdapter
    private lateinit var rvVideos: RecyclerView
    private lateinit var emptyStateLayout: LinearLayout
    private var pendingVideoUri: Uri? = null
    private var pendingVideoName: String = ""

    private fun saveState() {
        VideoStorageManager.saveVideos(requireContext(), videoAdapter.getVideos())
    }

    private val subtitlePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data?.data != null) {
            val subUri = result.data!!.data!!
            val fileName = getRealFileName(subUri)

            if (fileName.lowercase().endsWith(".srt")) {
                if (pendingVideoUri != null) {
                    requireContext().contentResolver.takePersistableUriPermission(subUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)

                    val newVideo =
                        VideoItem(pendingVideoName, pendingVideoUri!!, externalSubtitleUri = subUri)
                    videoAdapter.addVideo(newVideo)
                    updateUI()
                    saveState()
                    openPlayer(newVideo)
                    pendingVideoUri = null
                }
            } else {
                Toast.makeText(requireContext(), R.string.select_srt_error, Toast.LENGTH_LONG).show()
                requireExternalSubtitleDialog()
            }
        } else {
            pendingVideoUri = null
        }
    }

    private val videoPickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data?.data != null) {
            val videoUri = result.data!!.data!!
            val videoName = getRealFileName(videoUri)

            requireContext().contentResolver.takePersistableUriPermission(videoUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)

            pendingVideoUri = videoUri
            pendingVideoName = videoName
            requireExternalSubtitleDialog()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        rvVideos = view.findViewById(R.id.rvVideos)
        emptyStateLayout = view.findViewById(R.id.emptyStateLayout)
        val fab: FloatingActionButton = view.findViewById(R.id.fabAddVideo)

        rvVideos.layoutManager = LinearLayoutManager(requireContext())
        videoAdapter = VideoAdapter { clickedVideo -> openPlayer(clickedVideo) }
        rvVideos.adapter = videoAdapter

        // === НОВЕ: Завантажуємо збережені відео ===
        val savedVideos = VideoStorageManager.loadVideos(requireContext())
        savedVideos.forEach { video ->
            video.isLoadingSubtitles = false
            videoAdapter.addVideo(video)
        }
        // =========================================

        val swipeHandler = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            override fun onMove(r: RecyclerView, v: RecyclerView.ViewHolder, t: RecyclerView.ViewHolder) = false
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                videoAdapter.removeVideo(viewHolder.adapterPosition)
                updateUI()
                saveState() // ЗБЕРІГАЄМО ПІСЛЯ ВИДАЛЕННЯ
            }
        }
        ItemTouchHelper(swipeHandler).attachToRecyclerView(rvVideos)

        updateUI()
        fab.setOnClickListener { showAddVideoMenu() }

        return view
    }
    // 1. СТИЛЬНЕ МЕНЮ ЗНИЗУ (Bottom Sheet)
    private fun showAddVideoMenu() {
        // Використовуємо сучасний BottomSheetDialog
        val bottomSheetDialog = BottomSheetDialog(requireContext())
        val view = layoutInflater.inflate(R.layout.dialog_bottom_sheet_add, null)
        bottomSheetDialog.setContentView(view)

        val btnGallery = view.findViewById<View>(R.id.btnGallery)
        val btnYouTube = view.findViewById<View>(R.id.btnYouTube)

        btnGallery.setOnClickListener {
            bottomSheetDialog.dismiss()
            openDeviceGallery()
        }

        btnYouTube.setOnClickListener {
            bottomSheetDialog.dismiss()
            showYouTubeLinkDialog()
        }

        bottomSheetDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            bottomSheetDialog.window?.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
            bottomSheetDialog.window?.attributes?.blurBehindRadius = 64
        }
        bottomSheetDialog.show()
    }

    // 2. СУЧАСНЕ ВІКНО ВВОДУ ПОСИЛАННЯ (Material 3)
    private fun showYouTubeLinkDialog() {
        val margin = (20 * resources.displayMetrics.density).toInt()
        val container = FrameLayout(requireContext())
        container.setPadding(margin, margin / 2, margin, 0)

        // Створюємо красиве поле з рамкою (Outlined)
        val textInputLayout = TextInputLayout(requireContext()).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
            hint = "https://www.youtube.com/watch?v=..."
            boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE
            setBoxCornerRadii(32f, 32f, 32f, 32f) // Круглі кути!
        }

        val input = TextInputEditText(textInputLayout.context).apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
        }

        textInputLayout.addView(input)
        container.addView(textInputLayout)

        // Використовуємо відцентрований стиль Material 3 для діалогу і робимо його "скляним"
        val dialog = MaterialAlertDialogBuilder(requireContext(), com.google.android.material.R.style.ThemeOverlay_Material3_MaterialAlertDialog_Centered)
            .setTitle("YouTube")
            .setMessage(R.string.youtube_link_msg)
            .setView(container)
            .setPositiveButton(R.string.add) { _, _ ->
                val link = input.text.toString()
                val videoId = YoutubeSubtitleFetcher.extractVideoId(link)

                if (videoId.isNotEmpty()) {
                    fetchYouTubeInfoAndAdd(videoId)
                } else {
                    Toast.makeText(requireContext(), R.string.invalid_link, Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .create()

        dialog.window?.setBackgroundDrawableResource(R.drawable.bg_glass_dialog)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            dialog.window?.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
            dialog.window?.attributes?.blurBehindRadius = 64
        }
        dialog.show()
    }
    private fun openDeviceGallery() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "video/*"
        }
        videoPickerLauncher.launch(intent)
    }


    // НОВИЙ ФЛОУ ДОДАВАННЯ: Відео спочатку заблоковане
    private fun fetchYouTubeInfoAndAdd(videoId: String) {
        Toast.makeText(requireContext(), R.string.adding_video, Toast.LENGTH_SHORT).show()

        val url = "https://www.youtube.com/oembed?url=https://www.youtube.com/watch?v=$videoId&format=json"
        val request = Request.Builder().url(url).build()
        val client = OkHttpClient()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                activity?.runOnUiThread { Toast.makeText(requireContext(), R.string.network_error, Toast.LENGTH_SHORT).show() }
            }

            override fun onResponse(call: Call, response: Response) {
                response.body?.string()?.let { jsonString ->
                    val jsonObject = JSONObject(jsonString)
                    val title = jsonObject.getString("title")
                    val thumbnailUrl = jsonObject.getString("thumbnail_url")

                    activity?.runOnUiThread {
                        val newVideo = VideoItem(
                            name = title,
                            uri = Uri.parse("youtube://$videoId"),
                            thumbnailUrl = thumbnailUrl,
                            isLoadingSubtitles = true
                        )
                        videoAdapter.addVideo(newVideo)
                        updateUI()

                        val position = videoAdapter.itemCount - 1

                        // Качаємо саби у фоні
                        YoutubeSubtitleFetcher.getSubtitlesUri(requireContext(), videoId, "en") { srtUri ->
                            activity?.runOnUiThread {
                                newVideo.isLoadingSubtitles = false // РОЗБЛОКОВУЄМО ВІДЕО

                                if (srtUri != null) {
                                    newVideo.externalSubtitleUri = srtUri
                                    Toast.makeText(requireContext(), R.string.subs_downloaded, Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(requireContext(), R.string.subs_not_found, Toast.LENGTH_SHORT).show()
                                }

                                // Оновлюємо вигляд елемента (робимо його яскравим)
                                videoAdapter.notifyItemChanged(position)
                                saveState()
                            }
                        }
                    }
                }
            }
        })
    }

    private fun requireExternalSubtitleDialog() {
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.no_subs_title)
            .setMessage(R.string.no_subs_msg)
            .setCancelable(false)
            .setPositiveButton(R.string.choose_file) { _, _ ->
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "*/*"
                }
                subtitlePickerLauncher.launch(intent)
            }
            .setNegativeButton(R.string.cancel) { _, _ -> pendingVideoUri = null }
            .create()
            
        dialog.window?.setBackgroundDrawableResource(R.drawable.bg_glass_dialog)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            dialog.window?.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
            dialog.window?.attributes?.blurBehindRadius = 64
        }
        dialog.show()
    }

    private fun updateUI() {
        val isEmpty = videoAdapter.isEmpty()
        emptyStateLayout.visibility = if (isEmpty) View.VISIBLE else View.GONE
        rvVideos.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

    // МИТТЄВИЙ ЗАПУСК
    private fun openPlayer(video: VideoItem) {
        if (video.isLoadingSubtitles) {
            Toast.makeText(requireContext(), R.string.wait_subs_loading, Toast.LENGTH_SHORT).show()
            return
        }

        val isYoutube = video.uri.scheme == "youtube"
        val videoId = video.uri.host ?: ""

        // Відкриваємо плеєр одразу з тим, що є в об'єкті (саби вже локальні)
        launchPlayerActivity(video, video.externalSubtitleUri, isYoutube, if (isYoutube) videoId else null)
    }

    private fun launchPlayerActivity(video: VideoItem, subUri: Uri?, isYoutube: Boolean, ytId: String?) {
        val intent = Intent(requireContext(), PlayerActivity::class.java).apply {
            putExtra("VIDEO_URI", video.uri.toString())
            putExtra("VIDEO_NAME", video.name)
            putExtra("IS_YOUTUBE", isYoutube)
            putExtra("YOUTUBE_ID", ytId)
            if (subUri != null) {
                putExtra("SUBTITLE_URI", subUri.toString())
            }
        }
        startActivity(intent)
    }

    private fun getRealFileName(uri: Uri): String {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor = requireContext().contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index != -1) result = it.getString(index)
                }
            }
        }
        return result ?: uri.lastPathSegment ?: getString(R.string.unknown_video)
    }
}