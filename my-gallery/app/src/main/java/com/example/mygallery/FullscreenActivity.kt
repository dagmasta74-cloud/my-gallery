package com.example.mygallery

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.github.chrisbanes.photoview.PhotoView
import java.io.File

class FullscreenActivity : AppCompatActivity() {

    private lateinit var photoView: PhotoView
    private lateinit var btnDelete: Button
    private lateinit var btnEdit: Button
    private lateinit var mediaItem: MediaItem

    companion object {
        fun start(activity: MainActivity, mediaItem: MediaItem) {
            val intent = Intent(activity, FullscreenActivity::class.java)
            intent.putExtra("media_item", mediaItem)
            activity.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fullscreen)

        mediaItem = intent.getSerializableExtra("media_item") as MediaItem

        photoView = findViewById(R.id.photoView)
        btnDelete = findViewById(R.id.btnDelete)
        btnEdit = findViewById(R.id.btnEdit)

        setupViews()
        setupButtons()
    }

    private fun setupViews() {
        if (mediaItem.type == MediaType.VIDEO) {
            val uri = Uri.fromFile(File(mediaItem.path))
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "video/*")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(intent)
            finish()
            return
        }

        Glide.with(this)
            .load(File(mediaItem.path))
            .into(photoView)
    }

    private fun setupButtons() {
        btnDelete.setOnClickListener {
            showDeleteDialog()
        }

        btnEdit.setOnClickListener {
            EditorActivity.start(this, mediaItem.path)
        }
    }

    private fun showDeleteDialog() {
        AlertDialog.Builder(this)
            .setTitle("Удалить файл")
            .setMessage("Вы уверены, что хотите удалить этот файл?")
            .setPositiveButton("Удалить") { _, _ ->
                deleteFile()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun deleteFile() {
        try {
            val uri = if (mediaItem.type == MediaType.IMAGE) {
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            } else {
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            }

            val selection = "${MediaStore.MediaColumns._ID}=?"
            val selectionArgs = arrayOf(mediaItem.id.toString())

            val deleted = contentResolver.delete(uri, selection, selectionArgs) > 0

            if (deleted) {
                Toast.makeText(this, "Файл удален", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                val file = File(mediaItem.path)
                if (file.exists() && file.delete()) {
                    Toast.makeText(this, "Файл удален", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this, "Не удалось удалить файл", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
