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

        // Editor setup
        editor.setEditorLanguage(PythonLanguage())
        editor.colorScheme = SchemeDarcula()
        editor.setTextSize(14f)

        requestStoragePermission()

        val shortcuts = arrayOf("!", "TAB", "<", ">", "/", "(", ")", "{", "}", "[", "]", ":", ";", "\"", "'", "=", "+", "-")

        shortcuts.forEach { label ->
            val btn = Button(this).apply {
                text = label
            }

            btn.setOnClickListener {
                when (label) {
                    "TAB" -> handleTabShortcut()
                    else -> editor.insertText(label)
                }
            }

            shortcutLayout.addView(btn)
        }
    }

    private fun handleTabShortcut() {
        val content = editor.text.toString()

        if (content.endsWith("!")) {
            editor.deleteText(1)

            val htmlBoilerplate = """
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Document</title>
</head>
<body>

</body>
</html>
""".trimIndent()

            editor.insertText(htmlBoilerplate)
            editor.setEditorLanguage(HTMLLanguage())
        } else {
            editor.insertText("    ")
        }
    }

    private fun requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.data = Uri.parse("package:$packageName")
                startActivity(intent)
            }
        }
    }
}
