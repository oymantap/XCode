package com.xcode.dev

import android.content.Context
import org.json.JSONObject
import java.io.File

class SettingsManager(val context: Context) {
    val settingsFile = File(context.filesDir, "settings.json")

    init {
        if (!settingsFile.exists()) {
            val defaultSettings = """
            {
                "fontSize": "14px",
                "editorTheme": "monokai",
                "textWrap": true,
                "tabSize": 4,
                "linenumbers": true
            }
            """.trimIndent()
            settingsFile.writeText(defaultSettings)
        }
    }

    fun loadSettings(): JSONObject {
        return try { JSONObject(settingsFile.readText()) } 
        catch (e: Exception) { JSONObject() }
    }
}

