package com.xcode.dev

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import io.github.rosemoe.sora.langs.html.HTMLLanguage
import io.github.rosemoe.sora.langs.python.PythonLanguage
import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.widget.schemes.SchemeDarcula

class MainActivity : AppCompatActivity() {

    private lateinit var editor: CodeEditor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        editor = findViewById(R.id.editor)
        val shortcutLayout = findViewById<LinearLayout>(R.id.shortcut_layout)

        // 1. Setup Editor Modern
        editor.setEditorLanguage(PythonLanguage()) // Default Python
        editor.colorScheme = SchemeDarcula() // Tema gelap pro
        editor.setTextSize(14f)

        // 2. Izin Akses Folder Real-time
        requestStoragePermission()

        // 3. Daftar Shortcut ala Acode (Lengkap)
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
                when (label) {
                    "TAB" -> handleTabShortcut()
                    else -> editor.insertText(label, 1)
                }
            }
            shortcutLayout.addView(btn)
        }
    }

    private fun handleTabShortcut() {
        val content = editor.text.toString()
        val cursor = editor.cursor.leftLine // Sederhananya cek akhir teks
        
        // Fitur Mega Proyek: ! + TAB -> HTML Structure
        if (content.endsWith("!")) {
            // Hapus tanda ! dan ganti dengan boilerplate
            editor.dispatchKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_DEL))
            editor.insertText("<!DOCTYPE html>\n<html>\n<head>\n    <title>XCode Project</title>\n</head>\n<body>\n    \n</body>\n</html>", 1)
            editor.setEditorLanguage(HTMLLanguage()) // Otomatis ganti highlight ke HTML
        } else {
            editor.insertText("    ", 1) // Tab biasa (4 spasi)
        }
    }

    private fun requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.data = Uri.parse("package:$packageName")
                startActivity(intent)
                Toast.makeText(this, "Izinkan akses semua file untuk XCode", Toast.LENGTH_LONG).show()
            }
        }
    }
}

