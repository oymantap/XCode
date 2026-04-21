package com.xcode.dev

import android.content.Intent
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
    
    private var rootDir: File? = null 
    private var currentDir: File? = null
    private val fileContents = mutableMapOf<Int, String>()
    private val tabFiles = mutableMapOf<Int, File?>()

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
        val txtExplorerHeader = findViewById<TextView>(R.id.txt_explorer_header)

        // Default tab
        if (tabLayout.tabCount == 0) addEmptyTab("main.txt")

        // Hubungkan Folder
        txtExplorerHeader.setOnClickListener {
            rootDir = Environment.getExternalStorageDirectory() 
            currentDir = rootDir
            loadFileList(currentDir!!)
            Toast.makeText(this, "Storage Connected", Toast.LENGTH_SHORT).show()
        }

        btnOpenDrawer.setOnClickListener { drawerLayout.openDrawer(Gravity.LEFT) }
        btnSave.setOnClickListener { saveCurrentFile() }
        
        btnPlay.setOnClickListener {
            // Preview logic placeholder
            Toast.makeText(this, "Opening Preview localhost:7777", Toast.LENGTH_SHORT).show()
        }

        setupExplorer()
        setupShortcuts(shortcutLayout)
    }

    private fun addEmptyTab(name: String) {
        val tab = tabLayout.newTab().setText(name)
        tabLayout.addTab(tab, true)
        fileContents[tab.position] = ""
        tabFiles[tab.position] = null
    }

    private fun setupExplorer() {
        listFiles.setOnItemClickListener { _, _, position, _ ->
            val fileName = listFiles.adapter.getItem(position) as String
            val clickedFile = if (fileName == "..") currentDir?.parentFile ?: currentDir!! 
                              else File(currentDir, fileName.removeSuffix("/"))
            
            if (clickedFile.isDirectory) {
                currentDir = clickedFile
                loadFileList(currentDir!!)
            } else {
                openFileInTab(clickedFile)
                drawerLayout.closeDrawers()
            }
        }

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                editor.setText(fileContents[tab?.position ?: 0] ?: "")
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {
                fileContents[tab?.position ?: 0] = editor.text.toString()
            }
            override fun onTabReselected(tab: TabLayout.Tab?) {
                if (tabLayout.tabCount > 1) {
                    val pos = tab!!.position
                    tabLayout.removeTabAt(pos)
                    fileContents.remove(pos)
                    tabFiles.remove(pos)
                }
            }
        })
    }

    private fun openFileInTab(file: File) {
        val tab = tabLayout.newTab().setText(file.name)
        tabLayout.addTab(tab, true)
        fileContents[tab.position] = file.readText()
        tabFiles[tab.position] = file
        editor.setText(fileContents[tab.position])
    }

    private fun loadFileList(dir: File) {
        tvCurrentPath.text = dir.absolutePath
        val files = dir.listFiles()?.sortedBy { !it.isDirectory } ?: emptyList()
        val names = mutableListOf<String>()
        if (dir != rootDir) names.add("..")
        files.forEach { names.add(if (it.isDirectory) it.name + "/" else it.name) }
        listFiles.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, names)
    }

    private fun saveCurrentFile() {
        val currentFile = tabFiles[tabLayout.selectedTabPosition]
        currentFile?.let {
            it.writeText(editor.text.toString())
            Toast.makeText(this, "Saved!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupShortcuts(layout: LinearLayout) {
        val shortcuts = arrayOf("!", "TAB", "<", ">", "/", "{", "}", "(", ")", ";", "=")
        shortcuts.forEach { label ->
            val btn = Button(this).apply {
                text = label
                minWidth = 100
                setTextColor(0xFFFFFFFF.toInt())
                setBackgroundColor(0x00000000)
            }
            btn.setOnClickListener {
                if (label == "TAB") handleTabShortcut() else editor.text.insert(editor.selectionStart, label)
            }
            layout.addView(btn)
        }
    }

    private fun handleTabShortcut() {
        val start = editor.selectionStart
        val text = editor.text.toString()
        if (start > 0 && text.substring(start - 1, start) == "!") {
            editor.text.delete(start - 1, start)
            // BOILERPLATE MODERN
            val boilerplate = "<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n  <meta charset=\"UTF-8\">\n  <title>Document</title>\n</head>\n<body>\n\n</body>\n</html>"
            editor.text.insert(editor.selectionStart, boilerplate)
        } else {
            editor.text.insert(editor.selectionStart, "    ")
        }
    }
}

