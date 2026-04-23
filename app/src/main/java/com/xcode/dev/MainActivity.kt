package com.xcode.dev

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
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
    private var loadingPopup: PopupWindow? = null
    
    private var currentFolder: DocumentFile? = null
    private val rootFolders = mutableListOf<DocumentFile>()
    private val openedTabs = mutableMapOf<Uri, TabLayout.Tab>()
    private val tabToUri = mutableMapOf<TabLayout.Tab, Uri>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.parseColor("#121212")
        setContentView(R.layout.activity_main)

        // Integrasi SettingsManager yang baru lo kasih
        settingsManager = SettingsManager(this)
        
        editorWebView = findViewById(R.id.editor)
        listFiles = findViewById(R.id.list_files)
        tabLayout = findViewById(R.id.tab_layout)
        drawerLayout = findViewById(R.id.drawer_layout)

        setupAceEditor()
        setupExplorer()
        setupTabs()
        setupShortcuts()
        
        drawerLayout.post { 
            showLoading("Restoring XCode Pro...") 
            loadFoldersFromPrefs()
            
            editorWebView.postDelayed({ 
                applySettingsToEditor()
                restoreTabsFromPrefs() 
                if (tabLayout.tabCount == 0) openDefaultFile()
                hideLoading()
            }, 1000)
        }
    }

    private fun showLoading(msg: String) {
        val view = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
            setPadding(60, 60, 60, 60)
            background = GradientDrawable().apply { setColor(Color.parseColor("#EE0D0D0D")); cornerRadius = 40f }
            addView(ProgressBar(context))
            addView(TextView(context).apply { text = msg; setTextColor(Color.WHITE); setPadding(0, 20, 0, 0) })
        }
        if (drawerLayout.windowToken != null) {
            loadingPopup = PopupWindow(view, 500, 400).apply { showAtLocation(drawerLayout, Gravity.CENTER, 0, 0) }
        }
    }

    private fun hideLoading() { loadingPopup?.dismiss() }

    private fun openDefaultFile() {
        val defaultFile = File(filesDir, "main.txt")
        if (!defaultFile.exists()) defaultFile.writeText("// Welcome to XCode Pro\n")
        openFileWithTab(DocumentFile.fromFile(defaultFile))
    }

    private fun String.fixCode(): String {
        var clean = this
        if (clean.startsWith("\"") && clean.endsWith("\"")) clean = clean.substring(1, clean.length - 1)
        return clean.replace("\\\\", "\\").replace("\\n", "\n").replace("\\t", "\t")
            .replace("\\\"", "\"").replace("\\'", "'").replace("\\u003C", "<")
            .replace("\\u003E", ">").replace("\\u0026", "&")
    }

    private fun saveCurrentFile() {
        val currentTab = tabLayout.getTabAt(tabLayout.selectedTabPosition) ?: return
        val uri = tabToUri[currentTab] ?: return
        editorWebView.evaluateJavascript("getCode()") { code ->
            val finalCode = code.fixCode()
            try {
                if (uri.toString().contains("settings.json") || uri.scheme == "file") {
                    val path = uri.path ?: return@evaluateJavascript
                    File(path).writeText(finalCode)
                } else {
                    contentResolver.openOutputStream(uri, "wt")?.use { it.write(finalCode.toByteArray()) }
                }
                Toast.makeText(this, "Saved!", Toast.LENGTH_SHORT).show()
                if (uri.toString().contains("settings.json")) applySettingsToEditor()
            } catch (e: Exception) { Toast.makeText(this, "Save Error: ${e.message}", Toast.LENGTH_LONG).show() }
        }
    }

    private fun getFileIcon(file: DocumentFile): Int {
        if (file.isDirectory) return R.drawable.ic_folder
        val n = file.name?.lowercase() ?: ""
        
        // Logic Flutter (Tetap dijaga biar ga ilang lagi)
        if (n.endsWith(".dart")) {
            try {
                if (file.parentFile?.name == "lib") return R.drawable.ic_flutter
                contentResolver.openInputStream(file.uri)?.bufferedReader()?.use { 
                    if (it.readLine()?.contains("package:flutter") == true) return R.drawable.ic_flutter 
                }
            } catch (e: Exception) {}
            return R.drawable.ic_dart
        }
        
        return when {
            n.endsWith(".html") -> R.drawable.ic_html
            n.endsWith(".js") -> R.drawable.ic_js
            n.endsWith(".css") -> R.drawable.ic_css
            n.endsWith(".java") -> R.drawable.ic_java
            n.endsWith(".kt") || n.endsWith(".kts") -> R.drawable.ic_kt
            n.endsWith(".json") || n.endsWith(".sql") -> R.drawable.ic_db
            else -> R.drawable.ic_files
        }
    }

    private fun setupExplorer() {
        findViewById<ImageButton>(R.id.btn_add_folder).setOnClickListener { 
            val i = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
                addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            }
            startActivityForResult(i, 1001) 
        }
        findViewById<ImageButton>(R.id.btn_save).setOnClickListener { saveCurrentFile() }
        findViewById<ImageButton>(R.id.btn_play).setOnClickListener { runPreview() }
        findViewById<ImageButton>(R.id.btn_open_drawer).setOnClickListener { drawerLayout.openDrawer(Gravity.LEFT) }
        
        findViewById<Button>(R.id.btn_new_file).setOnClickListener { 
            if (currentFolder != null) {
                showInputDialog("New File", "index.html") { name ->
                    currentFolder?.createFile("application/octet-stream", name)
                    refreshListView()
                }
            } else Toast.makeText(this, "Select folder in explorer!", Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.btn_back_root).setOnClickListener {
            currentFolder = null
            refreshListView()
        }

        listFiles.setOnItemLongClickListener { _, _, pos, _ ->
            val files = getVisibleFiles()
            if (pos >= 0 && pos < files.size) showModernPopup(files[pos], pos)
            true
        }
    }

    private fun refreshListView() {
        val files = getVisibleFiles()
        val adapter = object : ArrayAdapter<DocumentFile>(this, 0, files) {
            override fun getView(pos: Int, conv: View?, par: ViewGroup): View {
                val layout = LinearLayout(context).apply { 
                    orientation = LinearLayout.HORIZONTAL; setPadding(35, 30, 35, 30); gravity = Gravity.CENTER_VERTICAL 
                }
                val file = getItem(pos)!!
                layout.addView(ImageView(context).apply { 
                    layoutParams = LinearLayout.LayoutParams(55, 55); setImageResource(getFileIcon(file)) 
                })
                layout.addView(TextView(context).apply { 
                    text = "   " + file.name; setTextColor(if(file.name == "settings.json") Color.YELLOW else Color.WHITE)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                })
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

    private fun getVisibleFiles(): List<DocumentFile> {
        val list = mutableListOf<DocumentFile>()
        if (currentFolder == null) { 
            list.add(DocumentFile.fromFile(File(filesDir, "settings.json")))
            list.addAll(rootFolders) 
        } else { list.addAll(currentFolder!!.listFiles()) }
        return list
    }

    private fun openFileWithTab(file: DocumentFile, save: Boolean = true) {
        if (openedTabs.containsKey(file.uri)) { openedTabs[file.uri]?.select(); return }
        val newTab = tabLayout.newTab()
        val cv = LayoutInflater.from(this).inflate(R.layout.custom_tab_item, null)
        cv.findViewById<ImageView>(R.id.tab_icon).setImageResource(getFileIcon(file))
        cv.findViewById<TextView>(R.id.tab_title).text = file.name
        cv.findViewById<ImageButton>(R.id.btn_close_tab).setOnClickListener {
            openedTabs.remove(file.uri); tabToUri.remove(newTab); tabLayout.removeTab(newTab)
            if (tabLayout.tabCount == 0) openDefaultFile()
            saveTabsToPrefs()
        }
        newTab.customView = cv; tabLayout.addTab(newTab, true)
        openedTabs[file.uri] = newTab; tabToUri[newTab] = file.uri
        loadContentToEditor(file.uri)
        if (save) saveTabsToPrefs()
    }

    private fun loadContentToEditor(uri: Uri) {
        try {
            val content = when {
                uri.toString().contains("settings.json") -> settingsManager.settingsFile.readText()
                uri.scheme == "file" -> File(uri.path!!).readText()
                else -> contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: ""
            }
            val name = DocumentFile.fromSingleUri(this, uri)?.name ?: "index.html"
            val escaped = content.replace("\\", "\\\\").replace("'", "\\'").replace("\n", "\\n")
            editorWebView.evaluateJavascript("setCode('$escaped', '$name')", null)
            applySettingsToEditor()
        } catch (e: Exception) {}
    }

    private fun applySettingsToEditor() {
        val json = settingsManager.loadSettings()
        editorWebView.evaluateJavascript("applySettings($json)", null)
    }

    private fun runPreview() {
        editorWebView.evaluateJavascript("getCode()") { code ->
            val intent = Intent(this, PreviewActivity::class.java)
            intent.putExtra("html_code", code.fixCode())
            startActivity(intent)
        }
    }

    private fun loadFoldersFromPrefs() {
        val prefs = getSharedPreferences("XC", MODE_PRIVATE)
        prefs.getStringSet("f", null)?.forEach {
            val u = Uri.parse(it)
            try {
                contentResolver.takePersistableUriPermission(u, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                DocumentFile.fromTreeUri(this, u)?.let { f -> if (f.exists()) rootFolders.add(f) }
            } catch (e: Exception) {}
        }
        refreshListView()
    }

    private fun saveFoldersToPrefs() { 
        getSharedPreferences("XC", MODE_PRIVATE).edit().putStringSet("f", rootFolders.map { it.uri.toString() }.toSet()).apply() 
    }

    private fun saveTabsToPrefs() { 
        getSharedPreferences("XC", MODE_PRIVATE).edit().putStringSet("t", openedTabs.keys.map { it.toString() }.toSet()).apply() 
    }

    private fun restoreTabsFromPrefs() {
        getSharedPreferences("XC", MODE_PRIVATE).getStringSet("t", null)?.forEach {
            val u = Uri.parse(it)
            if (it.contains("settings.json")) openFileWithTab(DocumentFile.fromFile(File(filesDir, "settings.json")), false)
            else if (u.scheme == "file") openFileWithTab(DocumentFile.fromFile(File(u.path!!)), false)
            else DocumentFile.fromSingleUri(this, u)?.let { d -> if(d.exists()) openFileWithTab(d, false) }
        }
    }

    private fun setupAceEditor() {
        editorWebView.settings.apply { 
            javaScriptEnabled = true; domStorageEnabled = true; allowFileAccess = true 
        }
        editorWebView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(v: WebView?, u: String?) { applySettingsToEditor() }
        }
        editorWebView.loadUrl("file:///android_asset/editor.html")
    }

    private fun setupTabs() {
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) { tabToUri[tab]?.let { loadContentToEditor(it) } }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun setupShortcuts() {
        val sc = findViewById<LinearLayout>(R.id.shortcut_layout)
        sc.removeAllViews()
        val actions = mapOf("↺" to "editor.undo()", "↻" to "editor.redo()", "➜" to "handleShortcut('TAB')")
        actions.forEach { (lbl, js) ->
            val b = Button(this).apply { text = lbl; setTextColor(Color.WHITE); setBackgroundColor(0) }
            b.setOnClickListener { editorWebView.evaluateJavascript(js, null) }
            sc.addView(b)
        }
        val symbols = arrayOf("<", ">", "/", "{", "}", "(", ")", ";", "\"", "'")
        symbols.forEach { s ->
            val b = Button(this).apply { text = s; setTextColor(Color.WHITE); setBackgroundColor(0) }
            b.setOnClickListener { editorWebView.evaluateJavascript("handleShortcut('$s')", null) }
            sc.addView(b)
        }
    }

    private fun showInputDialog(title: String, default: String, cb: (String) -> Unit) {
        val input = EditText(this).apply { setText(default) }
        AlertDialog.Builder(this).setTitle(title).setView(input)
            .setPositiveButton("OK") { _, _ -> cb(input.text.toString()) }
            .setNegativeButton("Cancel", null).show()
    }

    private fun showModernPopup(file: DocumentFile, pos: Int) {
        val options = if (currentFolder == null && file.name != "settings.json") arrayOf("Rename", "Eject Folder") else arrayOf("Rename", "Delete")
        AlertDialog.Builder(this).setTitle(file.name).setItems(options) { _, which ->
            if (which == 0) showInputDialog("Rename", file.name ?: "") { file.renameTo(it); refreshListView() }
            else {
                if (currentFolder == null) { rootFolders.removeAt(pos - 1); saveFoldersToPrefs() }
                else { file.delete() }
                refreshListView()
            }
        }.show()
    }

    private fun dimBehind(popup: PopupWindow) {
        val container = popup.contentView.rootView
        val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val lp = container.layoutParams as WindowManager.LayoutParams
        lp.flags = WindowManager.LayoutParams.FLAG_DIM_BEHIND
        lp.dimAmount = 0.5f
        wm.updateViewLayout(container, lp)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1001 && resultCode == RESULT_OK) {
            data?.data?.let { u ->
                contentResolver.takePersistableUriPermission(u, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                DocumentFile.fromTreeUri(this, u)?.let { f -> 
                    if (!rootFolders.any { it.uri == f.uri }) { rootFolders.add(f); saveFoldersToPrefs(); refreshListView() }
                }
            }
        }
    }
}
