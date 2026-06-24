package com.example.mygallery

import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.burhanrashid52.photoeditor.PhotoEditor
import com.burhanrashid52.photoeditor.PhotoEditorView
import java.io.File
import java.io.FileOutputStream

class EditorActivity : AppCompatActivity() {

    private lateinit var photoEditorView: PhotoEditorView
    private lateinit var photoEditor: PhotoEditor
    private lateinit var btnSave: Button
    private lateinit var btnCancel: Button
    private lateinit var btnText: Button
    private lateinit var btnBrush: Button
    private lateinit var btnUndo: ImageButton
    private lateinit var etText: EditText
    private lateinit var btnAddText: Button
    private lateinit var colorPicker: View
    private var imagePath: String = ""
    private var currentColor: Int = Color.RED
    private var isBrushMode = false

    companion object {
        fun start(activity: FullscreenActivity, path: String) {
            val intent = Intent(activity, EditorActivity::class.java)
            intent.putExtra("image_path", path)
            activity.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_editor)

        imagePath = intent.getStringExtra("image_path") ?: ""

        initViews()
        setupPhotoEditor()
        setupButtons()
    }

    private fun initViews() {
        photoEditorView = findViewById(R.id.photoEditorView)
        btnSave = findViewById(R.id.btnSave)
        btnCancel = findViewById(R.id.btnCancel)
        btnText = findViewById(R.id.btnText)
        btnBrush = findViewById(R.id.btnBrush)
        btnUndo = findViewById(R.id.btnUndo)
        etText = findViewById(R.id.etText)
        btnAddText = findViewById(R.id.btnAddText)
        colorPicker = findViewById(R.id.colorPicker)
    }

    private fun setupPhotoEditor() {
        photoEditor = PhotoEditor.Builder(this, photoEditorView)
            .setPinchTextScalable(true)
            .build()

        photoEditorView.source.setImageBitmap(
            android.graphics.BitmapFactory.decodeFile(imagePath)
        )
    }

    private fun setupButtons() {
        colorPicker.setOnClickListener {
            showColorPickerDialog()
        }

        btnText.setOnClickListener {
            etText.visibility = View.VISIBLE
            btnAddText.visibility = View.VISIBLE
            photoEditor.setBrushDrawingMode(false)
            isBrushMode = false
        }

        btnAddText.setOnClickListener {
            val text = etText.text.toString()
            if (text.isNotEmpty()) {
                photoEditor.addText(text, currentColor)
                etText.text.clear()
                etText.visibility = View.GONE
                btnAddText.visibility = View.GONE
            } else {
                Toast.makeText(this, "Введите текст", Toast.LENGTH_SHORT).show()
            }
        }

        btnBrush.setOnClickListener {
            isBrushMode = !isBrushMode
            photoEditor.setBrushDrawingMode(isBrushMode)
            photoEditor.setBrushColor(currentColor)
            btnBrush.isSelected = isBrushMode
            etText.visibility = View.GONE
            btnAddText.visibility = View.GONE
        }

        btnUndo.setOnClickListener {
            photoEditor.undo()
        }

        btnCancel.setOnClickListener {
            finish()
        }

        btnSave.setOnClickListener {
            saveEditedImage()
        }
    }

    private fun showColorPickerDialog() {
        val colors = arrayOf(
            Color.RED, Color.BLUE, Color.GREEN, Color.YELLOW,
            Color.CYAN, Color.MAGENTA, Color.BLACK, Color.WHITE
        )
        val colorNames = arrayOf("Красный", "Синий", "Зеленый", "Желтый",
            "Голубой", "Фиолетовый", "Черный", "Белый")

        AlertDialog.Builder(this)
            .setTitle("Выберите цвет")
            .setItems(colorNames) { _, which ->
                currentColor = colors[which]
                colorPicker.setBackgroundColor(currentColor)
                if (isBrushMode) {
                    photoEditor.setBrushColor(currentColor)
                }
            }
            .show()
    }

    private fun saveEditedImage() {
        try {
            val bitmap = photoEditorView.source.getBitmap() ?: return

            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, "edited_${System.currentTimeMillis()}.jpg")
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/MyGallery")
                } else {
                    put(MediaStore.Images.Media.DATA, "${Environment.getExternalStorageDirectory().absolutePath}/Pictures/MyGallery/")
                }
            }

            val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)

            uri?.let {
                contentResolver.openOutputStream(it)?.use { outputStream ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
                }
                Toast.makeText(this, "Изображение сохранено", Toast.LENGTH_SHORT).show()
                finish()
            } ?: run {
                val file = File(getExternalFilesDir(null), "edited_${System.currentTimeMillis()}.jpg")
                FileOutputStream(file).use { outputStream ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
                }
                Toast.makeText(this, "Изображение сохранено", Toast.LENGTH_SHORT).show()
                finish()
            }

        } catch (e: Exception) {
            Toast.makeText(this, "Ошибка сохранения: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
