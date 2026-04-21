package com.xcode.dev

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var editor: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Cari EditText standar di layout
        editor = findViewById(R.id.editor)
        val shortcutLayout = findViewById<LinearLayout>(R.id.shortcut_layout)

        // Shortcut Bar ala Acode
        val shortcuts = arrayOf("!", "TAB", "<", ">", "/", "(", ")", "{", "}", "[", "]", ":", ";", "\"", "'", "=", "+", "-")

        shortcuts.forEach { label ->
            val btn = Button(this).apply {
                text = label
                minWidth = 100
                setPadding(10, 0, 10, 0)
                setTextColor(0xFFFFFFFF.toInt())
                setBackgroundColor(0x00000000) // Transparan
            }

            btn.setOnClickListener {
                if (label == "TAB") {
                    handleTabShortcut()
                } else {
                    insertText(label)
                }
            }
            shortcutLayout.addView(btn)
        }
    }

    private fun handleTabShortcut() {
        val start = editor.selectionStart
        val text = editor.text.toString()
        
        // Cek kalau karakter sebelum kursor adalah '!'
        if (start > 0 && text.substring(start - 1, start) == "!") {
            // Hapus '!' dan masukkan Boilerplate HTML5 pesanan Prof
            editor.text.delete(start - 1, start)
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
            insertText(htmlBoilerplate)
        } else {
            insertText("    ") // 4 spasi
        }
    }

    private fun insertText(text: String) {
        val start = editor.selectionStart
        val end = editor.selectionEnd
        editor.text.replace(Math.min(start, end), Math.max(start, end), text)
    }
}

