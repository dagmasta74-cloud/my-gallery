package com.example.mygallery

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: MediaAdapter
    private val mediaItems = mutableListOf<MediaItem>()
    private val groupedItems = mutableListOf<Pair<String, List<MediaItem>>>()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.all { it }) {
            loadMedia()
        } else {
            Toast.makeText(this, "Нужны разрешения для работы", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setupRecyclerView()
        checkPermissions()
    }

    private fun setupRecyclerView() {
        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = MediaAdapter(groupedItems) { mediaItem ->
            FullscreenActivity.start(this, mediaItem)
        }
        recyclerView.adapter = adapter
    }

    private fun checkPermissions() {
        val permissions = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.READ_MEDIA_IMAGES)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO)
                != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.READ_MEDIA_VIDEO)
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }

        if (permissions.isNotEmpty()) {
            requestPermissionLauncher.launch(permissions.toTypedArray())
        } else {
            loadMedia()
        }
    }

    private fun loadMedia() {
        mediaItems.clear()
        groupedItems.clear()

        loadImages()
        loadVideos()

        mediaItems.sortByDescending { it.dateTaken }
        groupByDate()

        adapter.notifyDataSetChanged()
    }

    private fun loadImages() {
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DATA,
            MediaStore.Images.Media.DATE_TAKEN,
            MediaStore.Images.Media.DISPLAY_NAME
        )

        contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            null
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val pathCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)

            while (cursor.moveToNext()) {
                val path = cursor.getString(pathCol)
                if (File(path).exists()) {
                    mediaItems.add(
                        MediaItem(
                            id = cursor.getLong(idCol),
                            path = path,
                            dateTaken = cursor.getLong(dateCol),
                            type = MediaType.IMAGE,
                            displayName = cursor.getString(nameCol)
                        )
                    )
                }
            }
        }
    }

    private fun loadVideos() {
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.DATE_TAKEN,
            MediaStore.Video.Media.DISPLAY_NAME
        )

        contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            null
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val pathCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
            val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_TAKEN)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)

            while (cursor.moveToNext()) {
                val path = cursor.getString(pathCol)
                if (File(path).exists()) {
                    mediaItems.add(
                        MediaItem(
                            id = cursor.getLong(idCol),
                            path = path,
                            dateTaken = cursor.getLong(dateCol),
                            type = MediaType.VIDEO,
                            displayName = cursor.getString(nameCol)
                        )
                    )
                }
            }
        }
    }

    private fun groupByDate() {
        val dateFormat = SimpleDateFormat("d MMMM yyyy", Locale.getDefault())
        val today = Calendar.getInstance()
        val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }

        val grouped = mutableMapOf<String, MutableList<MediaItem>>()

        mediaItems.forEach { item ->
            val date = Date(item.dateTaken)
            val calendar = Calendar.getInstance().apply { time = date }

            val label = when {
                calendar.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                        calendar.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR) -> "Сегодня"

                calendar.get(Calendar.YEAR) == yesterday.get(Calendar.YEAR) &&
                        calendar.get(Calendar.DAY_OF_YEAR) == yesterday.get(Calendar.DAY_OF_YEAR) -> "Вчера"

                else -> dateFormat.format(date)
            }

            grouped.getOrPut(label) { mutableListOf() }.add(item)
        }

        groupedItems.addAll(grouped.entries.sortedByDescending {
            when (it.key) {
                "Сегодня" -> Long.MAX_VALUE
                "Вчера" -> Long.MAX_VALUE - 1
                else -> {
                    try {
                        val date = SimpleDateFormat("d MMMM yyyy", Locale.getDefault()).parse(it.key)
                        date?.time ?: 0L
                    } catch (e: Exception) {
                        0L
                    }
                }
            }
        }.map { Pair(it.key, it.value) })
    }

    override fun onResume() {
        super.onResume()
        if (::adapter.isInitialized) {
            loadMedia()
        }
    }
}
