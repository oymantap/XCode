package com.xcode.dev

import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import com.ahmadaghazadeh.editor.widget.CodeEditor

class MainActivity : AppCompatActivity() {

    private lateinit var editor: CodeEditor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        editor = findViewById(R.id.editor)
        val shortcutLayout = findViewById<LinearLayout>(R.id.shortcut_layout)

        // Setup Editor
        editor.setTheme(com.ahmadaghazadeh.editor.processor.utils.Theme.DARCULA)
        editor.setType(com.ahmadaghazadeh.editor.processor.utils.Language.PYTHON)
        editor.content = "" // Awal kosong

        // Shortcut Bar
        val shortcuts = arrayOf("!", "TAB", "<", ">", "/", "(", ")", "{", "}", "[", "]", ":", ";", "\"", "'", "=", "+", "-")

        shortcuts.forEach { label ->
            val btn = Button(this).apply {
                text = label
                minWidth = 80
                setBackgroundColor(0x00000000)
                setTextColor(0xFFFFFFFF.toInt())
            }

            btn.setOnClickListener {
                if (label == "TAB") {
                    handleTab()
                } else {
                    editor.insertText(label)
                }
            }
            shortcutLayout.addView(btn)
        }
    }

    private fun handleTab() {
        val currentText = editor.content
        if (currentText.endsWith("!")) {
            // Hapus '!' dan masukkan boilerplate HTML5
            val cleanText = currentText.substring(0, currentText.length - 1)
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

            editor.content = cleanText + htmlBoilerplate
            editor.setType(com.ahmadaghazadeh.editor.processor.utils.Language.HTML)
        } else {
            editor.insertText("    ")
        }
    }
}

