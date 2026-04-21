package com.xcode.dev

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var editor: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        editor = findViewById(R.id.editor)
        val shortcutLayout = findViewById<LinearLayout>(R.id.shortcut_layout)

        // 1. Izin Storage (Biar fitur akses folder gak ilang)
        requestStoragePermission()

        // 2. Daftar Shortcut ala Acode (Full Set)
        val shortcuts = arrayOf("!", "TAB", "<", ">", "/", "(", ")", "{", "}", "[", "]", ":", ";", "\"", "'", "=", "+", "-")

        shortcuts.forEach { label ->
            val btn = Button(this).apply {
                text = label
                minWidth = 100
                setPadding(10, 0, 10, 0)
                setTextColor(0xFFFFFFFF.toInt())
                setBackgroundColor(0x00000000) // Transparent
            }

            btn.setOnClickListener {
                if (label == "TAB") {
                    handleTabShortcut()
                } else {
                    insertAtCursor(label)
                }
            }
            shortcutLayout.addView(btn)
        }
    }

    private fun handleTabShortcut() {
        val start = editor.selectionStart
        val text = editor.text.toString()
        
        // FITUR MEGA PROYEK: ! + TAB -> HTML5 Boilerplate Modern
        if (start > 0 && text.substring(start - 1, start) == "!") {
            editor.text.delete(start - 1, start) // Hapus tanda '!'
            
            val boilerplate = """
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <meta http-equiv="X-UA-Compatible" content="ie=edge">
  <title>Document</title>
</head>
<body>
  
</body>
</html>""".trimIndent()
            insertAtCursor(boilerplate)
        } else {
            insertAtCursor("    ") // Tab 4 spasi
        }
    }

    private fun insertAtCursor(str: String) {
        val start = editor.selectionStart
        val end = editor.selectionEnd
        editor.text.replace(Math.min(start, end), Math.max(start, end), str)
    }

    private fun requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                    intent.data = Uri.parse("package:$packageName")
                    startActivity(intent)
                } catch (e: Exception) {
                    val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                    startActivity(intent)
                }
            }
        }
    }
}
