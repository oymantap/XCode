package com.xcode.dev

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.tabs.TabLayout

class MainActivity : AppCompatActivity() {
    private lateinit var editor: EditText
    private val fileContents = mutableMapOf<Int, String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        editor = findViewById(R.id.editor)
        val tabLayout = findViewById<TabLayout>(R.id.tab_layout)
        val btnFolder = findViewById<ImageButton>(R.id.btn_folder)
        val btnPlay = findViewById<ImageButton>(R.id.btn_play)
        val shortcutLayout = findViewById<LinearLayout>(R.id.shortcut_layout)

        // 1. Tombol Folder (Open File)
        btnFolder.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "*/*"
            }
            startActivityForResult(intent, 101)
        }

        // 2. Tombol Play (Run Webview)
        btnPlay.setOnClickListener {
            Toast.makeText(this, "Running Project...", Toast.LENGTH_SHORT).show()
            // Logic WebView intent bisa ditaruh sini
        }

        // 3. Shortcut Bar (Dikasih Jarak)
        val shortcuts = arrayOf("!", "TAB", "<", ">", "/", "{", "}", "(", ")", ";", "=")
        shortcuts.forEach { label ->
            val btn = Button(this).apply {
                text = label
                minWidth = 90
                layoutParams = LinearLayout.LayoutParams(90, 80).apply { setMargins(5, 0, 5, 0) }
                setTextColor(0xFFFFFFFF.toInt())
                setBackgroundResource(android.R.drawable.btn_default) // Atau custom shape
            }
            btn.setOnClickListener { 
                if (label == "TAB") handleTab() else editor.text.insert(editor.selectionStart, label)
            }
            shortcutLayout.addView(btn)
        }
    }

    private fun handleTab() {
        val start = editor.selectionStart
        if (start > 0 && editor.text.substring(start - 1, start) == "!") {
            editor.text.delete(start - 1, start)
            val html = "<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n  <meta charset=\"UTF-8\">\n  <title>Document</title>\n</head>\n<body>\n\n</body>\n</html>"
            editor.text.insert(editor.selectionStart, html)
        } else {
            editor.text.insert(editor.selectionStart, "    ")
        }
    }
}
