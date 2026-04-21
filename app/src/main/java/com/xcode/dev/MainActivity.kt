package com.xcode.dev

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.documentfile.provider.DocumentFile
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.tabs.TabLayout

class MainActivity : AppCompatActivity() {

    private lateinit var editor: EditText
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var listFiles: ListView
    private lateinit var tabLayout: TabLayout
    private lateinit var btnPlay: ImageButton
    
    private var rootFolder: DocumentFile? = null
    private val fileContents = mutableMapOf<Int, String>()
    private val tabUris = mutableMapOf<Int, Uri?>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        editor = findViewById(R.id.editor)
        drawerLayout = findViewById(R.id.drawer_layout)
        listFiles = findViewById(R.id.list_files)
        tabLayout = findViewById(R.id.tab_layout)
        btnPlay = findViewById(R.id.btn_play)
        val btnAddFolder = findViewById<ImageButton>(R.id.btn_add_folder)
        val btnNewFile = findViewById<Button>(R.id.btn_new_file)
        val shortcutLayout = findViewById<LinearLayout>(R.id.shortcut_layout)

        // 1. Hubungkan Folder (Pilih Media/Folder)
        btnAddFolder.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
            startActivityForResult(intent, 1001)
        }

        // 2. Tambah File Baru
        btnNewFile.setOnClickListener { showCreateFileDialog() }

        // 3. Editor Save & Play
        findViewById<ImageButton>(R.id.btn_save).setOnClickListener { saveFile() }
        btnPlay.setOnClickListener { runWebView() }
        findViewById<ImageButton>(R.id.btn_open_drawer).setOnClickListener { drawerLayout.openDrawer(Gravity.LEFT) }

        // 4. Tab & Shortcut
        setupTabs()
        setupShortcuts(shortcutLayout)
        
        // 5. Context Menu (Rename/Hapus)
        registerForContextMenu(listFiles)
        listFiles.setOnItemLongClickListener { _, _, pos, _ ->
            showFileOptions(pos)
            true
        }
    }

    private fun setupTabs() {
        if (tabLayout.tabCount == 0) addTab("main.txt", null)
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                val pos = tab?.position ?: 0
                editor.setText(fileContents[pos] ?: "")
                updatePlayButtonVisibility(tab?.text.toString())
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {
                fileContents[tab?.position ?: 0] = editor.text.toString()
            }
            override fun onTabReselected(tab: TabLayout.Tab?) {
                if (tabLayout.tabCount > 1) removeTab(tab!!)
            }
        })
    }

    private fun updatePlayButtonVisibility(fileName: String) {
        btnPlay.visibility = if (fileName.endsWith(".html")) View.VISIBLE else View.GONE
    }

    private fun addTab(name: String, uri: Uri?) {
        val tab = tabLayout.newTab().setText(name)
        tabLayout.addTab(tab, true)
        tabUris[tab.position] = uri
        updatePlayButtonVisibility(name)
    }

    private fun removeTab(tab: TabLayout.Tab) {
        val pos = tab.position
        tabLayout.removeTabAt(pos)
        tabUris.remove(pos)
        fileContents.remove(pos)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1001 && resultCode == RESULT_OK) {
            val uri = data?.data ?: return
            contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            rootFolder = DocumentFile.fromTreeUri(this, uri)
            refreshListView()
        }
    }

    private fun refreshListView() {
        val names = rootFolder?.listFiles()?.map { it.name ?: "Unknown" } ?: emptyList()
        listFiles.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, names)
        listFiles.setOnItemClickListener { _, _, i, _ ->
            val file = rootFolder?.listFiles()?.get(i)
            if (file != null && file.isFile) {
                openFile(file)
                drawerLayout.closeDrawers()
            }
        }
    }

    private fun openFile(file: DocumentFile) {
        val content = contentResolver.openInputStream(file.uri)?.bufferedReader()?.use { it.readText() } ?: ""
        addTab(file.name ?: "file", file.uri)
        editor.setText(content)
    }

    private fun saveFile() {
        val uri = tabUris[tabLayout.selectedTabPosition] ?: return
        contentResolver.openOutputStream(uri, "wt")?.use { it.write(editor.text.toString().toByteArray()) }
        Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show()
    }

    private fun showCreateFileDialog() {
        val input = EditText(this)
        AlertDialog.Builder(this).setTitle("New File").setView(input)
            .setPositiveButton("Create") { _, _ ->
                val name = input.text.toString()
                rootFolder?.createFile("*/*", name)
                refreshListView()
            }.show()
    }

    private fun showFileOptions(pos: Int) {
        val file = rootFolder?.listFiles()?.get(pos) ?: return
        val options = arrayOf("Rename", "Delete")
        AlertDialog.Builder(this).setItems(options) { _, which ->
            if (which == 0) { // Rename logic
            } else {
                file.delete()
                refreshListView()
            }
        }.show()
    }

    private fun runWebView() {
        // Jangan pake AlertDialog, langsung pindah layar (Activity)
        val intent = Intent(this, PreviewActivity::class.java)
        
        // Kirim kode dari editor ke layar preview
        intent.putExtra("html_code", editor.text.toString())
        
        startActivity(intent)
    }

    private fun setupShortcuts(layout: LinearLayout) {
        val items = arrayOf("!", "TAB", "<", ">", "/", "\", "{", "}", "(", ")", ";", ":"", "+", "*", "=", "?", "|", "1", "2", "3", "4", "5", "6", "7", "8", "9", "0", "XCode")
        items.forEach { label ->
            val btn = Button(this).apply {
                text = label
                layoutParams = LinearLayout.LayoutParams(-2, -1)
                setBackgroundColor(0)
                setTextColor(-1)
            }
            btn.setOnClickListener {
                if (label == "TAB") handleTab() else editor.text.insert(editor.selectionStart, label)
            }
            layout.addView(btn)
            layout.addView(View(this).apply { layoutParams = LinearLayout.LayoutParams(2, -1); setBackgroundColor(0x33FFFFFF) }) // Pembatas |
        }
    }

    private fun handleTab() {
        val start = editor.selectionStart
        if (start > 0 && editor.text.substring(start - 1, start) == "!") {
            editor.text.delete(start - 1, start)
            editor.text.insert(editor.selectionStart, "<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n  <meta charset=\"UTF-8\">\n  <title>Document</title>\n</head>\n<body>\n\n</body>\n</html>")
        } else editor.text.insert(editor.selectionStart, "    ")
    }
}
