package com.xcode.dev

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.tabs.TabLayout

class MainActivity : AppCompatActivity() {

    private lateinit var editor: EditText
    private lateinit var tabLayout: TabLayout
    
    // Simpan konten file per tab
    private val fileContents = mutableMapOf<Int, String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        editor = findViewById(R.id.editor)
        tabLayout = findViewById(R.id.tab_layout)
        val shortcutLayout = findViewById<LinearLayout>(R.id.shortcut_layout)

        // Setup 3 Tab Awal (Bisa ditambahin logic buat add tab dinamis nanti)
        setupTabs()

        // Shortcut Bar
        val shortcuts = arrayOf("!", "TAB", "<", ">", "/", "(", ")", "{", "}", "[", "]", ":", ";", "\"", "'", "=", "+", "-")
        shortcuts.forEach { label ->
            val btn = Button(this).apply {
                text = label
                minWidth = 100
                setTextColor(0xFFFFFFFF.toInt())
                setBackgroundColor(0x00000000)
            }
            btn.setOnClickListener {
                if (label == "TAB") handleTabShortcut() else insertAtCursor(label)
            }
            shortcutLayout.addView(btn)
        }
    }

    private fun setupTabs() {
        val files = arrayOf("index.html", "script.py", "style.css")
        files.forEachIndexed { index, name ->
            tabLayout.addTab(tabLayout.newTab().setText(name))
            fileContents[index] = "" // Inisialisasi kosong
        }

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                tab?.let { 
                    // Simpan teks dari tab sebelumnya, load teks tab sekarang
                    editor.setText(fileContents[it.position] ?: "")
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {
                tab?.let { fileContents[it.position] = editor.text.toString() }
            }
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun handleTabShortcut() {
        val start = editor.selectionStart
        val text = editor.text.toString()
        
        if (start > 0 && text.substring(start - 1, start) == "!") {
            editor.text.delete(start - 1, start)
            val boilerplate = """
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>XCode Project</title>
</head>
<body>
  
</body>
</html>""".trimIndent()
            insertAtCursor(boilerplate)
        } else {
            insertAtCursor("    ")
        }
    }

    private fun insertAtCursor(str: String) {
        val start = Math.max(editor.selectionStart, 0)
        val end = Math.max(editor.selectionEnd, 0)
        editor.text.replace(start, end, str)
    }
}

