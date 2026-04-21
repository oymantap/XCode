package com.xcode.dev

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.*
import android.text.style.ForegroundColorSpan
import android.view.Gravity
import android.view.View
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
    private var currentFolder: DocumentFile? = null
    private val tabUris = mutableMapOf<Int, Uri?>()
    private val fileContents = mutableMapOf<Int, String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        editor = findViewById(R.id.editor)
        drawerLayout = findViewById(R.id.drawer_layout)
        listFiles = findViewById(R.id.list_files)
        tabLayout = findViewById(R.id.tab_layout)
        btnPlay = findViewById(R.id.btn_play)
        
        setupExplorerButtons()
        setupTabs()
        setupShortcuts(findViewById(R.id.shortcut_layout))
        setupSyntaxHighlighting()
    }

    private fun setupExplorerButtons() {
        findViewById<ImageButton>(R.id.btn_add_folder).setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
            startActivityForResult(intent, 1001)
        }

        findViewById<Button>(R.id.btn_new_file).setOnClickListener { showCreateFileDialog() }
        findViewById<ImageButton>(R.id.btn_save).setOnClickListener { saveFile() }
        btnPlay.setOnClickListener { runWebView() }
        findViewById<ImageButton>(R.id.btn_open_drawer).setOnClickListener { drawerLayout.openDrawer(Gravity.LEFT) }
    }

    private fun setupTabs() {
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                val pos = tab?.position ?: 0
                editor.setText(fileContents[pos] ?: "")
                btnPlay.visibility = if (tab?.text.toString().endsWith(".html")) View.VISIBLE else View.GONE
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {
                fileContents[tab?.position ?: 0] = editor.text.toString()
            }
            override fun onTabReselected(tab: TabLayout.Tab?) {
                // Tahan tab atau klik lagi buat tutup (Simulasi tombol X)
                showCloseTabDialog(tab)
            }
        })
    }

    private fun showCloseTabDialog(tab: TabLayout.Tab?) {
        if (tab == null || tabLayout.tabCount <= 1) return
        AlertDialog.Builder(this).setMessage("Tutup tab ${tab.text}?")
            .setPositiveButton("Ya") { _, _ ->
                val pos = tab.position
                tabUris.remove(pos)
                fileContents.remove(pos)
                tabLayout.removeTabAt(pos)
            }.setNegativeButton("Batal", null).show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1001 && resultCode == RESULT_OK) {
            val uri = data?.data ?: return
            contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            rootFolder = DocumentFile.fromTreeUri(this, uri)
            currentFolder = rootFolder
            refreshListView()
        }
    }

    private fun refreshListView() {
        val files = currentFolder?.listFiles() ?: emptyArray()
        val names = mutableListOf<String>()
        
        // Tombol Back di List
        if (currentFolder != rootFolder) names.add("← Kembali")

        files.forEach { 
            val prefix = if (it.isDirectory) "📁 " else "📄 "
            names.add(prefix + (it.name ?: "Unknown")) 
        }

        listFiles.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, names)
        
        listFiles.setOnItemClickListener { _, _, i, _ ->
            var idx = i
            if (currentFolder != rootFolder) {
                if (idx == 0) {
                    currentFolder = currentFolder?.parentFile
                    refreshListView()
                    return@setOnItemClickListener
                }
                idx-- // Adjust index karena ada tombol back
            }

            val selected = currentFolder?.listFiles()?.get(idx)
            if (selected != null) {
                if (selected.isDirectory) {
                    currentFolder = selected
                    refreshListView()
                } else {
                    openFileWithFilter(selected)
                    drawerLayout.closeDrawers()
                }
            }
        }
    }

    private fun openFileWithFilter(file: DocumentFile) {
        // CEK APAKAH SUDAH TERBUKA (FILTER SPAM TAB)
        for (i in 0 until tabLayout.tabCount) {
            if (tabUris[i] == file.uri) {
                tabLayout.getTabAt(i)?.select()
                Toast.makeText(this, "File sudah terbuka", Toast.LENGTH_SHORT).show()
                return
            }
        }

        val content = contentResolver.openInputStream(file.uri)?.bufferedReader()?.use { it.readText() } ?: ""
        val newTab = tabLayout.newTab().setText(file.name)
        tabLayout.addTab(newTab, true)
        
        val pos = newTab.position
        tabUris[pos] = file.uri
        fileContents[pos] = content
        editor.setText(content)
    }

    private fun saveFile() {
        val uri = tabUris[tabLayout.selectedTabPosition] ?: return
        contentResolver.openOutputStream(uri, "wt")?.use { it.write(editor.text.toString().toByteArray()) }
        Toast.makeText(this, "Saved!", Toast.LENGTH_SHORT).show()
    }

    private fun showCreateFileDialog() {
        val input = EditText(this)
        AlertDialog.Builder(this).setTitle("File Baru").setView(input)
            .setPositiveButton("Buat") { _, _ ->
                val name = input.text.toString()
                currentFolder?.createFile("text/plain", name)
                refreshListView()
            }.show()
    }

    private fun runWebView() {
        val intent = Intent(this, PreviewActivity::class.java)
        intent.putExtra("html_code", editor.text.toString())
        startActivity(intent)
    }

    private fun setupShortcuts(layout: LinearLayout) {
        val items = arrayOf("!", "TAB", "<", ">", "/", "\\", "{", "}", "(", ")", ";", ":", "\"", "+", "*", "=", "?", "|")
        items.forEach { label ->
            val btn = Button(this).apply {
                text = label
                layoutParams = LinearLayout.LayoutParams(-2, -1)
                setBackgroundColor(0)
                setTextColor(-1)
                isAllCaps = false
            }
            btn.setOnClickListener {
                if (label == "TAB") handleTab() else editor.text.insert(editor.selectionStart, label)
            }
            layout.addView(btn)
        }
    }

    private fun handleTab() {
        val start = editor.selectionStart
        if (start > 0 && editor.text.substring(start - 1, start) == "!") {
            editor.text.delete(start - 1, start)
            editor.text.insert(editor.selectionStart, "<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n  <meta charset=\"UTF-8\">\n  <title>Document</title>\n</head>\n<body>\n\n</body>\n</html>")
        } else editor.text.insert(editor.selectionStart, "    ")
    }

    private fun setupSyntaxHighlighting() {
        editor.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun onTextChanged(s: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun afterTextChanged(s: Editable?) {
                editor.removeTextChangedListener(this)
                val text = s.toString()
                s?.clearSpans()
                
                // Regex sederhana buat HTML tags
                val tagPattern = "<[^>]*>".toRegex()
                tagPattern.findAll(text).forEach {
                    s?.setSpan(ForegroundColorSpan(0xFF569CD6.toInt()), it.range.first, it.range.last + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                editor.addTextChangedListener(this)
            }
        })
    }
}

