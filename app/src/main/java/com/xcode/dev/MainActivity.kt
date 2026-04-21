package com.xcode.dev

import android.os.Bundle
import android.os.Environment
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.tabs.TabLayout
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var editor: EditText
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var listFiles: ListView
    private lateinit var tvCurrentPath: TextView
    private lateinit var tabLayout: TabLayout
    
    private var currentDir: File = Environment.getExternalStorageDirectory()
    private val fileContents = mutableMapOf<Int, String>()
    private val tabFiles = mutableMapOf<Int, File?>() // Melacak file asli per tab

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        editor = findViewById(R.id.editor)
        drawerLayout = findViewById(R.id.drawer_layout)
        listFiles = findViewById(R.id.list_files)
        tvCurrentPath = findViewById(R.id.tv_current_path)
        tabLayout = findViewById(R.id.tab_layout)
        
        val btnOpenDrawer = findViewById<ImageButton>(R.id.btn_open_drawer)
        val btnSave = findViewById<ImageButton>(R.id.btn_save)
        val btnPlay = findViewById<ImageButton>(R.id.btn_play)
        val shortcutLayout = findViewById<LinearLayout>(R.id.shortcut_layout)

        // 1. Setup Awal
        setupTabs()
        loadFileList(currentDir)

        // 2. Toolbar Actions
        btnOpenDrawer.setOnClickListener { drawerLayout.openDrawer(Gravity.LEFT) }
        btnSave.setOnClickListener { saveCurrentFile() }
        btnPlay.setOnClickListener { Toast.makeText(this, "Running Preview...", Toast.LENGTH_SHORT).show() }

        // 3. Explorer Logic
        listFiles.setOnItemClickListener { _, _, position, _ ->
            val fileName = listFiles.adapter.getItem(position) as String
            val clickedFile = if (fileName == "..") currentDir.parentFile ?: currentDir else File(currentDir, fileName.removeSuffix("/"))
            
            if (clickedFile.isDirectory) {
                currentDir = clickedFile
                loadFileList(currentDir)
            } else {
                openFileInTab(clickedFile)
                drawerLayout.closeDrawers()
            }
        }

        // 4. Shortcut Bar (! + TAB logic included)
        val shortcuts = arrayOf("!", "TAB", "<", ">", "/", "{", "}", "(", ")", ";", "=", "\"", "'", ":")
        shortcuts.forEach { label ->
            val btn = Button(this).apply {
                text = label
                minWidth = 100
                layoutParams = LinearLayout.LayoutParams(100, 85).apply { setMargins(5, 0, 5, 0) }
                setTextColor(0xFFFFFFFF.toInt())
                setBackgroundResource(android.R.drawable.btn_default)
            }
            btn.setOnClickListener {
                if (label == "TAB") handleTabShortcut() else editor.text.insert(editor.selectionStart, label)
            }
            shortcutLayout.addView(btn)
        }
    }

    private fun setupTabs() {
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                tab?.let { editor.setText(fileContents[it.position] ?: "") }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {
                tab?.let { fileContents[it.position] = editor.text.toString() }
            }
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun loadFileList(dir: File) {
        tvCurrentPath.text = dir.absolutePath
        val files = dir.listFiles()?.sortedBy { !it.isDirectory } ?: emptyList()
        val names = mutableListOf<String>()
        if (dir != Environment.getExternalStorageDirectory()) names.add("..")
        files.forEach { names.add(if (it.isDirectory) it.name + "/" else it.name) }
        listFiles.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, names)
    }

    private fun openFileInTab(file: File) {
        val newTabIndex = tabLayout.tabCount
        tabLayout.addTab(tabLayout.newTab().setText(file.name), true)
        fileContents[newTabIndex] = file.readText()
        tabFiles[newTabIndex] = file
        editor.setText(fileContents[newTabIndex])
    }

    private fun saveCurrentFile() {
        val currentFile = tabFiles[tabLayout.selectedTabPosition]
        if (currentFile != null) {
            currentFile.writeText(editor.text.toString())
            Toast.makeText(this, "Saved: ${currentFile.name}", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "File not linked to storage", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleTabShortcut() {
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

