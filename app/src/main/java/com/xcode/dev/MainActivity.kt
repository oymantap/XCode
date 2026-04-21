package com.xcode.dev

import android.os.Bundle
import android.webkit.WebView
import android.widget.LinearLayout
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import io.github.rosemoe.sora.widget.CodeEditor // Library editor modern

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 1. Layout Utama (Vertical)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        // 2. Shortcut Bar (Horizontal)
        val shortcutBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        val tools = arrayOf("{", "}", "(", ")", ";", "->", "print", "def")
        tools.forEach { tool ->
            val btn = Button(this).apply { text = tool }
            btn.setOnClickListener { /* Logic sisip teks */ }
            shortcutBar.addView(btn)
        }

        // 3. Editor Utama (XCode Core)
        val editor = CodeEditor(this)
        // Setup Syntax Highlighting (React/Python logic goes here)
        
        // 4. WebView (Buat jalanin kode)
        val runnerView = WebView(this)

        // Susun UI-nya
        root.addView(shortcutBar)
        root.addView(editor, LinearLayout.LayoutParams(-1, 0, 1f))
        root.addView(runnerView, LinearLayout.LayoutParams(-1, 0, 1f))

        setContentView(root)
    }
}

