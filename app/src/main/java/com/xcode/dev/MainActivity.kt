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
        loadFoldersFromPrefs()

        editorWebView.postDelayed({ restoreTabsFromPrefs() }, 1000)
    }

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
            name.endsWith(".dart") -> R.drawable.ic_dart // Sesuai list lo
            else -> R.drawable.ic_files
        }
    }

    private fun setupAceEditor() {
        editorWebView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = true
        }
        editorWebView.webViewClient = WebViewClient()
        editorWebView.loadUrl("file:///android_asset/editor.html")
    }

    private fun setupExplorer() {
        findViewById<ImageButton>(R.id.btn_add_folder).setOnClickListener {
            startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT_TREE), 1001)
        }
        findViewById<ImageButton>(R.id.btn_save).setOnClickListener { saveCurrentFile() }
        findViewById<ImageButton>(R.id.btn_play).setOnClickListener { runPreview() }
        findViewById<ImageButton>(R.id.btn_open_drawer).setOnClickListener { drawerLayout.openDrawer(Gravity.LEFT) }
        findViewById<Button>(R.id.btn_new_file).setOnClickListener { showCreateFileDialog() }
        findViewById<Button>(R.id.btn_back_root)?.setOnClickListener { currentFolder = null; refreshListView() }

        // LONG CLICK: Menu Rename/Delete/Remove
        listFiles.setOnItemLongClickListener { _, view, position, _ ->
            val files = getVisibleFiles()
            if (position < files.size) {
                val selected = files[position]
                showFileMenu(selected, position)
            }
            true
        }
    }

    private fun getVisibleFiles(): List<DocumentFile> {
        return if (currentFolder == null) rootFolders else currentFolder!!.listFiles().toList()
    }

    private fun showFileMenu(file: DocumentFile, pos: Int) {
        val options = if (currentFolder == null) arrayOf("Rename", "Remove from XCode") 
                      else arrayOf("Rename", "Delete File/Folder")
        
        AlertDialog.Builder(this).setItems(options) { _, which ->
            when (which) {
                0 -> showRenameDialog(file)
                1 -> if (currentFolder == null) {
                        rootFolders.removeAt(pos); saveFoldersToPrefs(); refreshListView()
                     } else { showDeleteConfirm(file) }
            }
        }.show()
    }

    private fun showRenameDialog(file: DocumentFile) {
        val input = EditText(this).apply { setText(file.name) }
        AlertDialog.Builder(this).setTitle("Rename").setView(input)
            .setPositiveButton("OK") { _, _ -> 
                file.renameTo(input.text.toString())
                refreshListView()
            }.show()
    }

    private fun showDeleteConfirm(file: DocumentFile) {
        AlertDialog.Builder(this).setTitle("Hapus").setMessage("Yakin hapus ${file.name}?")
            .setPositiveButton("Ya") { _, _ -> file.delete(); refreshListView() }.show()
    }

    private fun refreshListView() {
        val files = getVisibleFiles().toMutableList()
        val adapter = object : ArrayAdapter<DocumentFile>(this, 0, files) {
            override fun getView(pos: Int, conv: View?, par: ViewGroup): View {
                val layout = LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    setPadding(35, 30, 35, 30)
                    gravity = Gravity.CENTER_VERTICAL
                }
                
                // Fitur: Tombol Back (UP) jika di dalam folder
                if (currentFolder != null && pos == 0 && files.isNotEmpty()) {
                    // Kita manipulasi tampilan item pertama jika ingin manual, 
                    // tapi cara termudah adalah cek folder parent.
                }

                val file = getItem(pos)!!
                val icon = ImageView(context).apply {
                    layoutParams = LinearLayout.LayoutParams(55, 55)
                    setImageResource(getFileIcon(file))
                }
                val txt = TextView(context).apply {
                    text = "   " + file.name
                    setTextColor(Color.WHITE)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
                }
                layout.addView(icon); layout.addView(txt)
                return layout
            }
        }

        // Header manual buat Back/Up
        if (currentFolder != null) {
            val header = Button(this).apply {
                text = "← UP (Ke Folder Sebelumnya)"
                setTextColor(Color.CYAN)
                setBackgroundColor(0)
                setOnClickListener { 
                    currentFolder = currentFolder?.parentFile 
                    if (rootFolders.any { it.uri == currentFolder?.uri }) currentFolder = null
                    refreshListView()
                }
            }
            if (listFiles.headerViewsCount == 0) listFiles.addHeaderView(header)
        } else {
            val count = listFiles.headerViewsCount
            for (i in 0 until count) { listFiles.removeHeaderView(listFiles.getChildAt(i)) }
            // Refresh listview hack to clear headers
            listFiles.adapter = null 
        }

        listFiles.adapter = adapter
        listFiles.setOnItemClickListener { _, _, pos, _ ->
            val actualPos = if (currentFolder != null) pos - 1 else pos
            if (actualPos < 0) return@setOnItemClickListener
            val selected = files[actualPos]
            if (selected.isDirectory) { currentFolder = selected; refreshListView() }
            else { openFileWithTab(selected); drawerLayout.closeDrawers() }
        }
    }

    private fun openFileWithTab(file: DocumentFile, save: Boolean = true) {
        for (i in 0 until tabLayout.tabCount) {
            if (tabUris[i] == file.uri) { tabLayout.getTabAt(i)?.select(); return }
        }
        val content = try { contentResolver.openInputStream(file.uri)?.bufferedReader()?.use { it.readText() } ?: "" } catch(e:Exception){""}
        val newTab = tabLayout.newTab()
        val tabView = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            setPadding(15, 0, 15, 0)
        }
        val icon = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(40, 40)
            setImageResource(getFileIcon(file))
        }
        val text = TextView(this).apply {
            text = "  ${file.name}  ✕"; setTextColor(Color.WHITE); setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
        }
        tabView.addView(icon); tabView.addView(text)
        newTab.customView = tabView
        tabLayout.addTab(newTab, true)
        tabUris[newTab.position] = file.uri
        
        val ext = file.name?.substringAfterLast(".", "html") ?: "html"
        val mode = when(ext) { "js" -> "javascript"; "py" -> "python"; "css" -> "css"; "ts" -> "typescript"; else -> "html" }
        val escaped = content.replace("\\", "\\\\").replace("'", "\\'").replace("\n", "\\n")
        editorWebView.evaluateJavascript("setCode('$escaped', '$mode')", null)
        if (save) saveTabsToPrefs()
    }

    private fun setupTabs() {
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                val p = tab?.position ?: return
                val uri = tabUris[p] ?: return
                val content = contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: ""
                val escaped = content.replace("\\", "\\\\").replace("'", "\\'").replace("\n", "\\n")
                editorWebView.evaluateJavascript("setCode('$escaped', 'html')", null)
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {
                val p = tab?.position ?: return
                tabUris.remove(p); tabLayout.removeTabAt(p)
                saveTabsToPrefs()
                if (tabLayout.tabCount == 0) openDefaultTab()
            }
        })
    }

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
            DocumentFile.fromSingleUri(this, uri)?.let { if(it.exists()) openFileWithTab(it, false) }
        }
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
                DocumentFile.fromTreeUri(this, uri)?.let { rootFolders.add(it); saveFoldersToPrefs(); refreshListView() }
            }
        }
    }

    private fun openDefaultTab() {
        val newTab = tabLayout.newTab()
        newTab.customView = TextView(this).apply { text = "main.txt  ✕"; setTextColor(Color.WHITE) }
        tabLayout.addTab(newTab, true)
        tabUris[newTab.position] = null
        editorWebView.evaluateJavascript("setCode('', 'text')", null)
    }

    private fun showCreateFileDialog() {
        val input = EditText(this).apply { hint = "namafile.js" }
        AlertDialog.Builder(this).setTitle("New File").setView(input).setPositiveButton("Create") { _, _ ->
            // FIX: Gunakan "application/octet-stream" agar Android tidak memaksa ekstensi .txt
            currentFolder?.createFile("application/octet-stream", input.text.toString())
            refreshListView()
        }.show()
    }

    private fun setupShortcuts() {
        val sc = findViewById<LinearLayout>(R.id.shortcut_layout)
        sc.removeAllViews()
        val items = arrayOf("!", "TAB", "<", ">", "/", "{", "}", "(", ")", ";", "*", "+", "-", "=", "\"", "'")
        items.forEach { label ->
            val b = Button(this).apply { text = label; setTextColor(Color.WHITE); setBackgroundColor(0); layoutParams = LinearLayout.LayoutParams(130, -1) }
            b.setOnClickListener { editorWebView.evaluateJavascript("handleShortcut('$label')", null) }
            sc.addView(b)
        }
    }
}

