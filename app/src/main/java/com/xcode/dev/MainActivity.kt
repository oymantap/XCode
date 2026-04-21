package com.xcode.dev

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.view.KeyEvent
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

        // 1. Setup Editor
        editor.setEditorLanguage(PythonLanguage()) 
        editor.colorScheme = SchemeDarcula() 
        editor.setTextSize(14f)

        requestStoragePermission()

        // 2. Shortcut Bar
        val shortcuts = arrayOf("!", "TAB", "<", ">", "/", "(", ")", "{", "}", "[", "]", ":", ";", "\"", "'", "=", "+", "-")

        shortcuts.forEach { label ->
            val btn = Button(this).apply {
                text = label
                minWidth = 100
                setPadding(10, 0, 10, 0)
                setTextColor(0xFFFFFFFF.toInt())
                setBackgroundColor(0x00000000)
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
        
        // Cek jika karakter terakhir sebelum TAB adalah '!'
        if (content.endsWith("!")) {
            // Hapus tanda '!'
            editor.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL))
            
            // Masukkan Boilerplate HTML5 permintaan Prof Rycl
            val htmlBoilerplate = """
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

            editor.insertText(htmlBoilerplate, 1)
            editor.setEditorLanguage(HTMLLanguage())
        } else {
            editor.insertText("    ", 1) // 4 spasi
        }
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

