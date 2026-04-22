package com.xcode.dev

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.TypedValue
import android.view.*
import android.webkit.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.documentfile.provider.DocumentFile
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.tabs.TabLayout

class MainActivity : AppCompatActivity() {

    private lateinit var editorWebView: WebView
    private lateinit var listFiles: ListView
    private lateinit var tabLayout: TabLayout
    private lateinit var drawerLayout: DrawerLayout
    
    private var currentFolder: DocumentFile? = null
    private val rootFolders = mutableListOf<DocumentFile>()
    private val tabUris = mutableMapOf<Int, Uri?>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        editorWebView = findViewById(R.id.editor)
        listFiles = findViewById(R.id.list_files)
        tabLayout = findViewById(R.id.tab_layout)
        drawerLayout = findViewById(R.id.drawer_layout)

        setupAceEditor()
        setupExplorer()
        setupTabs()
        setupShortcuts()
        
        // Load Folder & Tab State
        loadFoldersFromPrefs()
        
        // Beri jeda dikit biar Ace Editor siap baru restore tab
        editorWebView.postDelayed({
            restoreTabsFromPrefs()
        }, 1000)
    }

    // --- ICON LOGIC ---
    private fun getFileIcon(file: DocumentFile): Int {
        if (file.isDirectory) return R.drawable.ic_folder
        val name = file.name?.lowercase() ?: return R.drawable.ic_files
        
        return when {
            name.endsWith(".html") -> R.drawable.ic_html
            name.endsWith(".js") -> R.drawable.ic_js
            name.endsWith(".css") -> R.drawable.ic_css
            name.endsWith(".jsx") -> R.drawable.ic_jsx
            name.endsWith(".ts") -> R.drawable.ic_ts
            name.endsWith(".go") -> R.drawable.ic_go
            name.endsWith(".py") -> R.drawable.ic_py
            name.endsWith(".php") -> R.drawable.ic_php
            name.endsWith(".java") -> R.drawable.ic_java
            name.endsWith(".kt") -> R.drawable.ic_kt
            name.endsWith(".rs") -> R.drawable.ic_rs
            name.endsWith(".rb") -> R.drawable.ic_rb
            name.endsWith(".lua") -> R.drawable.ic_lua
            name.endsWith(".sql") || name.endsWith(".json") -> R.drawable.ic_db
            name.endsWith(".dart") -> {
                // Logic bedain Dart vs Flutter (ngintip isi file)
                val isFlutter = try {
                    val stream = contentResolver.openInputStream(file.uri)
                    val line = stream?.bufferedReader()?.readLine() ?: ""
                    stream?.close()
                    line.contains("flutter")
                } catch (e: Exception) { false }
                if (isFlutter) R.drawable.ic_flutter else R.drawable.ic_dart
            }
            else -> R.drawable.ic_files
        }
    }

    private fun setupAceEditor() {
        editorWebView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = true
        }
        editorWebView.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        editorWebView.webViewClient = WebViewClient()
        editorWebView.loadUrl("file:///android_asset/editor.html")
    }

    private fun setupExplorer() {
        findViewById<ImageButton>(R.id.btn_add_folder).setOnClickListener {
            startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT_TREE), 1001)
        }
        
        findViewById<ImageButton>(R.id.btn_save).setOnClickListener {
            saveCurrentFile()
        }

        findViewById<ImageButton>(R.id.btn_play).setOnClickListener {
            runPreview()
        }

        findViewById<ImageButton>(R.id.btn_open_drawer).setOnClickListener { drawerLayout.openDrawer(Gravity.LEFT) }
        findViewById<Button>(R.id.btn_new_file).setOnClickListener { showCreateFileDialog() }
        findViewById<Button>(R.id.btn_back_root)?.setOnClickListener { currentFolder = null; refreshListView() }

        // Klik lama buat hapus folder dari XCode
        listFiles.setOnItemLongClickListener { _, _, position, _ ->
            if (currentFolder == null) {
                val folder = rootFolders[position]
                AlertDialog.Builder(this)
                    .setTitle("Hapus Folder")
                    .setMessage("Hapus ${folder.name} dari list XCode?")
                    .setPositiveButton("Hapus") { _, _ ->
                        rootFolders.removeAt(position)
                        saveFoldersToPrefs()
                        refreshListView()
                    }.setNegativeButton("Batal", null).show()
                true
            } else false
        }
    }

    private fun openFileWithTab(file: DocumentFile, saveState: Boolean = true) {
        for (i in 0 until tabLayout.tabCount) {
            if (tabUris[i] == file.uri) {
                tabLayout.getTabAt(i)?.select()
                return
            }
        }

        val content = contentResolver.openInputStream(file.uri)?.bufferedReader()?.use { it.readText() } ?: ""
        val newTab = tabLayout.newTab()
        
        // Custom View buat Tab (Ikon + Nama)
        val tabView = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(10, 0, 10, 0)
        }
        val icon = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(40, 40)
            setImageResource(getFileIcon(file))
        }
        val text = TextView(this).apply {
            text = "  ${file.name}  ✕"
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
        }
        tabView.addView(icon)
        tabView.addView(text)
        
        newTab.customView = tabView
        tabLayout.addTab(newTab, true)
        
        val pos = newTab.position
        tabUris[pos] = file.uri
        
        val ext = file.name?.substringAfterLast(".", "html") ?: "html"
        val mode = when(ext) { "js" -> "javascript"; "py" -> "python"; "css" -> "css"; "ts" -> "typescript"; else -> "html" }
        val escaped = content.replace("\\", "\\\\").replace("'", "\\'").replace("\n", "\\n")
        editorWebView.evaluateJavascript("setCode('$escaped', '$mode')", null)

        if (saveState) saveTabsToPrefs()
    }

    private fun setupTabs() {
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                val p = tab?.position ?: return
                val uri = tabUris[p] ?: return
                val content = contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: ""
                val escaped = content.replace("\\", "\\\\").replace("'", "\\'").replace("\n", "\\n")
                editorWebView.evaluateJavascript("setCode('$escaped', 'html')", null)
                
                val fileName = DocumentFile.fromSingleUri(this@MainActivity, uri)?.name ?: ""
                findViewById<ImageButton>(R.id.btn_play).visibility = if (fileName.contains(".html")) View.VISIBLE else View.GONE
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {
                // Tutup Tab (Double Click)
                val p = tab?.position ?: return
                tabUris.remove(p)
                tabLayout.removeTabAt(p)
                saveTabsToPrefs()
                if (tabLayout.tabCount == 0) openDefaultTab()
            }
        })
    }

    private fun refreshListView() {
        val files = if (currentFolder == null) rootFolders.toTypedArray() else currentFolder!!.listFiles()
        val adapter = object : ArrayAdapter<DocumentFile>(this, 0, files) {
            override fun getView(pos: Int, conv: View?, par: ViewGroup): View {
                val layout = LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    setPadding(30, 25, 30, 25)
                    gravity = Gravity.CENTER_VERTICAL
                }
                val file = getItem(pos)!!
                val icon = ImageView(context).apply {
                    layoutParams = LinearLayout.LayoutParams(50, 50)
                    setImageResource(getFileIcon(file))
                }
                val txt = TextView(context).apply {
                    text = "   " + file.name
                    setTextColor(Color.WHITE)
                }
                layout.addView(icon); layout.addView(txt)
                return layout
            }
        }
        listFiles.adapter = adapter
        listFiles.setOnItemClickListener { _, _, pos, _ ->
            val selected = files[pos]
            if (selected.isDirectory) { currentFolder = selected; refreshListView() }
            else { openFileWithTab(selected); drawerLayout.closeDrawers() }
        }
    }

    // --- PERSISTENCE ---
    private fun saveFoldersToPrefs() {
        val set = rootFolders.map { it.uri.toString() }.toSet()
        getSharedPreferences("XCode", MODE_PRIVATE).edit().putStringSet("root_uris", set).apply()
    }

    private fun loadFoldersFromPrefs() {
        val set = getSharedPreferences("XCode", MODE_PRIVATE).getStringSet("root_uris", null)
        set?.forEach {
            val uri = Uri.parse(it)
            try {
                contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                DocumentFile.fromTreeUri(this, uri)?.let { doc -> rootFolders.add(doc) }
            } catch (e: Exception) {}
        }
        refreshListView()
    }

    private fun saveTabsToPrefs() {
        val uris = tabUris.values.filterNotNull().map { it.toString() }.toSet()
        getSharedPreferences("XCode", MODE_PRIVATE).edit().putStringSet("open_tabs", uris).apply()
    }

    private fun restoreTabsFromPrefs() {
        val set = getSharedPreferences("XCode", MODE_PRIVATE).getStringSet("open_tabs", null)
        set?.forEach {
            val uri = Uri.parse(it)
            DocumentFile.fromSingleUri(this, uri)?.let { doc -> if(doc.exists()) openFileWithTab(doc, false) }
        }
        if (tabLayout.tabCount == 0) openDefaultTab()
    }

    private fun saveCurrentFile() {
        val currentUri = tabUris[tabLayout.selectedTabPosition] ?: return
        editorWebView.evaluateJavascript("getCode()") { code ->
            val clean = code.trim('"').replace("\\n", "\n").replace("\\t", "\t").replace("\\\"", "\"").replace("\\\\", "\\")
            contentResolver.openOutputStream(currentUri, "wt")?.use { it.write(clean.toByteArray()) }
            Toast.makeText(this, "Saved!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun runPreview() {
        editorWebView.evaluateJavascript("getCode()") { code ->
            val clean = code.trim('"').replace("\\n", "\n").replace("\\t", "\t").replace("\\\"", "\"")
                .replace("\\u003C", "<").replace("\\u003E", ">").replace("\\u0026", "&")
            val intent = Intent(this, PreviewActivity::class.java).apply { putExtra("html_code", clean) }
            startActivity(intent)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1001 && resultCode == RESULT_OK) {
            data?.data?.let { uri ->
                contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                DocumentFile.fromTreeUri(this, uri)?.let { 
                    rootFolders.add(it)
                    saveFoldersToPrefs()
                    refreshListView() 
                }
            }
        }
    }

    private fun openDefaultTab() {
        val newTab = tabLayout.newTab()
        newTab.customView = TextView(this).apply { text = "main.txt  ✕"; setTextColor(Color.WHITE); setPadding(10,0,10,0) }
        tabLayout.addTab(newTab, true)
        tabUris[newTab.position] = null
        editorWebView.evaluateJavascript("setCode('', 'text')", null)
    }

    private fun showCreateFileDialog() {
        val input = EditText(this)
        AlertDialog.Builder(this).setTitle("New File").setView(input).setPositiveButton("Create") { _, _ ->
            currentFolder?.createFile("text/plain", input.text.toString()); refreshListView()
        }.show()
    }

    private fun setupShortcuts() {
        val sc = findViewById<LinearLayout>(R.id.shortcut_layout)
        sc.removeAllViews()
        val items = arrayOf("!", "TAB", "<", ">", "/", "{", "}", "(", ")", ";", "*", "+", "-", "=", "\"", "'")
        items.forEach { label ->
            val b = Button(this).apply {
                text = label; setTextColor(Color.WHITE); setBackgroundColor(0)
                layoutParams = LinearLayout.LayoutParams(130, -1)
            }
            b.setOnClickListener { editorWebView.evaluateJavascript("handleShortcut('$label')", null) }
            sc.addView(b)
        }
    }
}

