package com.xcode.dev

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
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
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var editorWebView: WebView
    private lateinit var listFiles: ListView
    private lateinit var tabLayout: TabLayout
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var settingsManager: SettingsManager
    
    private var currentFolder: DocumentFile? = null
    private val rootFolders = mutableListOf<DocumentFile>()
    
    // State management tab yang stabil
    private val openedTabs = mutableMapOf<Uri, TabLayout.Tab>()
    private val tabToUri = mutableMapOf<TabLayout.Tab, Uri>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        settingsManager = SettingsManager(this)
        editorWebView = findViewById(R.id.editor)
        listFiles = findViewById(R.id.list_files)
        tabLayout = findViewById(R.id.tab_layout)
        drawerLayout = findViewById(R.id.drawer_layout)

        setupAceEditor()
        setupExplorer()
        setupTabs()
        setupShortcuts() // Tombol undo/redo otomatis masuk sini
        loadFoldersFromPrefs()

        editorWebView.postDelayed({ 
            applySettingsToEditor()
            restoreTabsFromPrefs() 
        }, 1000)
    }

    private fun getFileIcon(file: DocumentFile): Int {
        if (file.isDirectory) return R.drawable.ic_folder
        val name = file.name?.lowercase() ?: ""
        if (name == "settings.json") return R.drawable.ic_db
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

    private fun setupTabs() {
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                val uri = tabToUri[tab] ?: return
                loadContentToEditor(uri)
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun openFileWithTab(file: DocumentFile, save: Boolean = true) {
        if (openedTabs.containsKey(file.uri)) {
            openedTabs[file.uri]?.select()
            return
        }

        val newTab = tabLayout.newTab()
        val customView = LayoutInflater.from(this).inflate(R.layout.custom_tab_item, null)
        val icon = customView.findViewById<ImageView>(R.id.tab_icon)
        val text = customView.findViewById<TextView>(R.id.tab_title)
        val close = customView.findViewById<ImageButton>(R.id.btn_close_tab)

        icon.setImageResource(getFileIcon(file))
        text.text = file.name
        
        close.setOnClickListener {
            val uri = tabToUri[newTab]
            openedTabs.remove(uri)
            tabToUri.remove(newTab)
            tabLayout.removeTab(newTab)
            if (tabLayout.tabCount == 0) editorWebView.evaluateJavascript("setCode('', 'text')", null)
            saveTabsToPrefs()
        }

        newTab.customView = customView
        tabLayout.addTab(newTab, true)
        openedTabs[file.uri] = newTab
        tabToUri[newTab] = file.uri
        
        loadContentToEditor(file.uri)
        if (save) saveTabsToPrefs()
    }

    private fun loadContentToEditor(uri: Uri) {
        try {
            val content = if (uri.toString().contains("settings.json")) settingsManager.settingsFile.readText()
            else contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: ""
            
            val name = if (uri.toString().contains("settings.json")) "settings.json" 
                      else DocumentFile.fromSingleUri(this, uri)?.name ?: ""
            
            val ext = name.substringAfterLast(".", "html")
            val mode = when(ext) { "js" -> "javascript"; "py" -> "python"; "css" -> "css"; "ts" -> "typescript"; "json" -> "json"; else -> "html" }
            val escaped = content.replace("\\", "\\\\").replace("'", "\\'").replace("\n", "\\n")
            editorWebView.evaluateJavascript("setCode('$escaped', '$mode')", null)
        } catch (e: Exception) {}
    }

    private fun showModernPopup(view: View, file: DocumentFile, pos: Int) {
        val popupView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundDrawable(Color.DKGRAY.toDrawableWithRadius(20)) // Transparan abu gelap
            setPadding(20, 20, 20, 20)
            elevation = 10f
        }

        val popup = PopupWindow(popupView, 500, ViewGroup.LayoutParams.WRAP_CONTENT, true)
        popup.animationStyle = android.R.style.Animation_Dialog

        val options = if (currentFolder == null) arrayOf("Rename", "Remove Folder") else arrayOf("Rename", "Delete File")
        
        options.forEachIndexed { index, title ->
            val btn = Button(this).apply {
                text = title; setTextColor(Color.WHITE); setBackgroundColor(0)
                gravity = Gravity.START or Gravity.CENTER_VERTICAL
            }
            btn.setOnClickListener {
                if (index == 0) showRenameDialog(file) else {
                    if (currentFolder == null) { rootFolders.removeAt(pos - 1); saveFoldersToPrefs(); refreshListView() }
                    else showDeleteConfirm(file)
                }
                popup.dismiss()
            }
            popupView.addView(btn)
        }
        popup.showAsDropDown(view, 100, -100)
    }

    private fun Int.toDrawableWithRadius(radius: Int): ColorDrawable {
        val gd = android.graphics.drawable.GradientDrawable()
        gd.setColor(Color.parseColor("#CC222222")) // Transparan
        gd.cornerRadius = radius.toFloat()
        return gd as? ColorDrawable ?: ColorDrawable(this) // Dummy return for logic
    }
    
    // --- FITUR UNDO/REDO & SHORTCUTS ---
    private fun setupShortcuts() {
        val sc = findViewById<LinearLayout>(R.id.shortcut_layout)
        sc.removeAllViews()
        
        // Tambahin Tombol Undo & Redo di depan
        val historyItems = arrayOf("↩️", "↪️")
        historyItems.forEach { label ->
            val b = Button(this).apply { text = label; setTextColor(Color.WHITE); setBackgroundColor(0); layoutParams = LinearLayout.LayoutParams(130, -1) }
            b.setOnClickListener {
                if (label == "↩️") editorWebView.evaluateJavascript("editorUndo()", null)
                else editorWebView.evaluateJavascript("editorRedo()", null)
            }
            sc.addView(b)
        }

        val items = arrayOf("!", "TAB", "<", ">", "/", "{", "}", "(", ")", ";", "*", "+", "-", "=", "\"", "'")
        items.forEach { label ->
            val b = Button(this).apply { text = label; setTextColor(Color.WHITE); setBackgroundColor(0); layoutParams = LinearLayout.LayoutParams(130, -1) }
            b.setOnClickListener { editorWebView.evaluateJavascript("handleShortcut('$label')", null) }
            sc.addView(b)
        }
    }

    private fun saveCurrentFile() {
        val currentTab = tabLayout.getTabAt(tabLayout.selectedTabPosition) ?: return
        val uri = tabToUri[currentTab] ?: return
        
        editorWebView.evaluateJavascript("getCode()") { code ->
            val clean = code.removeSurrounding("\"").replace("\\n", "\n").replace("\\t", "\t").replace("\\\"", "\"").replace("\\\\", "\\")
            if (uri.toString().contains("settings.json")) {
                settingsManager.settingsFile.writeText(clean)
                applySettingsToEditor()
            } else {
                contentResolver.openOutputStream(uri, "wt")?.use { it.write(clean.toByteArray()) }
            }
            Toast.makeText(this, "Saved!", Toast.LENGTH_SHORT).show()
        }
    }

    // --- SISA KODE (REPAIR & STABILIZE) ---
    private fun setupExplorer() {
        findViewById<ImageButton>(R.id.btn_add_folder).setOnClickListener { startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT_TREE), 1001) }
        findViewById<ImageButton>(R.id.btn_save).setOnClickListener { saveCurrentFile() }
        findViewById<ImageButton>(R.id.btn_open_drawer).setOnClickListener { drawerLayout.openDrawer(Gravity.LEFT) }
        findViewById<Button>(R.id.btn_new_file).setOnClickListener { showCreateFileDialog() }

        listFiles.setOnItemLongClickListener { v, _, position, _ ->
            val files = getVisibleFiles()
            val actualPos = if (currentFolder != null) position - 1 else position
            if (actualPos >= 0 && actualPos < files.size) {
                val selected = files[actualPos]
                if (selected.name != "settings.json") showModernPopup(v, selected, actualPos)
            }
            true
        }
    }

    private fun refreshListView() {
        val files = getVisibleFiles()
        val adapter = object : ArrayAdapter<DocumentFile>(this, 0, files) {
            override fun getView(pos: Int, conv: View?, par: ViewGroup): View {
                val layout = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL; setPadding(35, 30, 35, 30); gravity = Gravity.CENTER_VERTICAL }
                val file = getItem(pos)!!
                val icon = ImageView(context).apply { layoutParams = LinearLayout.LayoutParams(55, 55); setImageResource(getFileIcon(file)) }
                val txt = TextView(context).apply { text = "   " + file.name; setTextColor(if(file.name == "settings.json") Color.YELLOW else Color.WHITE); setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f) }
                layout.addView(icon); layout.addView(txt)
                return layout
            }
        }
        if (currentFolder != null) {
            val header = Button(this).apply { text = "← UP"; setTextColor(Color.CYAN); setBackgroundColor(0)
                setOnClickListener { currentFolder = currentFolder?.parentFile; if (rootFolders.any { it.uri == currentFolder?.uri }) currentFolder = null; refreshListView() }
            }
            if (listFiles.headerViewsCount == 0) listFiles.addHeaderView(header)
        } else {
            while (listFiles.headerViewsCount > 0) { listFiles.removeHeaderView(listFiles.getChildAt(0)) }
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

    private fun getVisibleFiles(): List<DocumentFile> {
        val list = mutableListOf<DocumentFile>()
        if (currentFolder == null) { list.add(DocumentFile.fromFile(settingsManager.settingsFile)); list.addAll(rootFolders) }
        else list.addAll(currentFolder!!.listFiles())
        return list
    }

    private fun saveFoldersToPrefs() {
        getSharedPreferences("XCode", MODE_PRIVATE).edit().putStringSet("root_uris", rootFolders.map { it.uri.toString() }.toSet()).apply()
    }

    private fun loadFoldersFromPrefs() {
        getSharedPreferences("XCode", MODE_PRIVATE).getStringSet("root_uris", null)?.forEach {
            val uri = Uri.parse(it)
            try { contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                DocumentFile.fromTreeUri(this, uri)?.let { rootFolders.add(it) } } catch (e: Exception) {}
        }
        refreshListView()
    }

    private fun saveTabsToPrefs() {
        getSharedPreferences("XCode", MODE_PRIVATE).edit().putStringSet("open_tabs", openedTabs.keys.map { it.toString() }.toSet()).apply()
    }

    private fun restoreTabsFromPrefs() {
        getSharedPreferences("XCode", MODE_PRIVATE).getStringSet("open_tabs", null)?.forEach {
            val uri = Uri.parse(it)
            if (uri.toString().contains("settings.json")) openFileWithTab(DocumentFile.fromFile(settingsManager.settingsFile), false)
            else DocumentFile.fromSingleUri(this, uri)?.let { doc -> if(doc.exists()) openFileWithTab(doc, false) }
        }
    }

    private fun showRenameDialog(file: DocumentFile) {
        val input = EditText(this).apply { setText(file.name) }
        AlertDialog.Builder(this).setTitle("Rename").setView(input).setPositiveButton("OK") { _, _ -> file.renameTo(input.text.toString()); refreshListView() }.show()
    }

    private fun showDeleteConfirm(file: DocumentFile) {
        AlertDialog.Builder(this).setTitle("Hapus").setMessage("Yakin?").setPositiveButton("Ya") { _, _ -> file.delete(); refreshListView() }.show()
    }

    private fun showCreateFileDialog() {
        val input = EditText(this).apply { hint = "index.js" }
        AlertDialog.Builder(this).setTitle("New File").setView(input).setPositiveButton("Create") { _, _ ->
            currentFolder?.createFile("application/octet-stream", input.text.toString()); refreshListView()
        }.show()
    }

    private fun setupAceEditor() {
        editorWebView.settings.apply { javaScriptEnabled = true; domStorageEnabled = true; allowFileAccess = true }
        editorWebView.webViewClient = WebViewClient()
        editorWebView.loadUrl("file:///android_asset/editor.html")
    }

    private fun applySettingsToEditor() {
        editorWebView.evaluateJavascript("applySettings(${settingsManager.loadSettings()})", null)
    }

    private fun runPreview() {
        editorWebView.evaluateJavascript("getCode()") { code ->
            val clean = code.removeSurrounding("\"").replace("\\n", "\n").replace("\\u003C", "<").replace("\\u003E", ">")
            startActivity(Intent(this, PreviewActivity::class.java).apply { putExtra("html_code", clean) })
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
}
