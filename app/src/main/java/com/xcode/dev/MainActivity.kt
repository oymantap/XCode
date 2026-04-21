package com.xcode.dev

import android.content.Intent
import android.net.Uri
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
    
    private var rootDir: File? = null // Folder yang dihubungkan
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

        // 1. DEFAULT FILE (main.txt)
        if (tabLayout.tabCount == 0) {
            addEmptyTab("main.txt")
        }

        // 2. Hubungkan Folder (Klik Header Explorer)
        findViewById<TextView>(R.id.txt_explorer_header).setOnClickListener {
            // Logic: Pilih folder lewat SAF (Storage Access Framework) 
            // Untuk simple-nya gue set ke Home dulu, lo bisa ganti ke Intent Picker
            rootDir = Environment.getExternalStorageDirectory()
            currentDir = rootDir
            loadFileList(currentDir!!)
            Toast.makeText(this, "Folder Terhubung!", Toast.LENGTH_SHORT).show()
        }

        btnOpenDrawer.setOnClickListener { drawerLayout.openDrawer(Gravity.LEFT) }
        btnSave.setOnClickListener { saveCurrentFile() }
        
        // 3. RUN PREVIEW (WebView Layer)
        btnPlay.setOnClickListener {
            val intent = Intent(this, PreviewActivity::class.java)
            intent.putExtra("code", editor.text.toString())
            startActivity(intent)
        }

        // 4. Explorer Logic
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

        // Shortcut setup tetap sama...
        setupShortcuts(shortcutLayout)
        setupTabCloseLogic()
    }

    private fun addEmptyTab(name: String) {
        val tab = tabLayout.newTab().setText(name)
        tabLayout.addTab(tab, true)
        fileContents[tab.position] = ""
        tabFiles[tab.position] = null
    }

    private fun setupTabCloseLogic() {
        // Double klik tab untuk tutup (Standard TabLayout gak punya tombol x bawaan tanpa custom view)
        // Gue kasih tips: Gunakan tabLayout.getTabAt(i).setCustomView(R.layout.custom_tab) nanti
        Toast.makeText(this, "Klik tahan tab untuk menutup file", Toast.LENGTH_SHORT).show()
    }

    private fun openFileInTab(file: File) {
        val tab = tabLayout.newTab().setText(file.name)
        tabLayout.addTab(tab, true)
        val index = tab.position
        fileContents[index] = file.readText()
        tabFiles[index] = file
        editor.setText(fileContents[index])
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
        if (currentFile != null) {
            currentFile.writeText(editor.text.toString())
            Toast.makeText(this, "Tersimpan ke ${currentFile.name}", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Gagal: Ini file temporary (main.txt)", Toast.LENGTH_SHORT).show()
        }
    }
    
    // ... Shortcut logic (! + TAB) sama seperti sebelumnya
}
