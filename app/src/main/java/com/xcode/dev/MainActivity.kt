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
    
    private var currentFolder: DocumentFile? = null
    private val rootFolders = mutableListOf<DocumentFile>()
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
        setupShortcuts()
        loadFoldersFromPrefs()

        editorWebView.postDelayed({ 
            applySettingsToEditor()
            restoreTabsFromPrefs() 
        }, 1000)
    }

    // --- FIX KODE RUSAK (UNESCAPE UNICODE) ---
    private fun String.unescapeJava(): String {
        var str = this.replace("\\u003C", "<")
            .replace("\\u003E", ">")
            .replace("\\u0026", "&")
            .replace("\\\"", "\"")
            .replace("\\'", "'")
            .replace("\\n", "\n")
            .replace("\\t", "\t")
            .replace("\\\\", "\\")
        if (str.startsWith("\"") && str.endsWith("\"")) str = str.substring(1, str.length - 1)
        return str
    }

    private fun saveCurrentFile() {
        val currentTab = tabLayout.getTabAt(tabLayout.selectedTabPosition) ?: return
        val uri = tabToUri[currentTab] ?: return
        
        editorWebView.evaluateJavascript("getCode()") { code ->
            val cleanCode = code.unescapeJava() // BALIKIN KE HTML ASLI
            try {
                if (uri.toString().contains("settings.json")) {
                    settingsManager.settingsFile.writeText(cleanCode)
                    applySettingsToEditor()
                } else {
                    contentResolver.openOutputStream(uri, "wt")?.use { it.write(cleanCode.toByteArray()) }
                }
                Toast.makeText(this, "Saved Successfully!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // --- POPUP MODERN TENGAH LAYAR ---
    private fun showModernPopup(file: DocumentFile, pos: Int) {
        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val shape = GradientDrawable().apply {
                setColor(Color.parseColor("#F21A1A1A")) // Transparan Blur Gelap
                cornerRadius = 40f
                setStroke(2, Color.parseColor("#33FFFFFF"))
            }
            background = shape
            setPadding(30, 30, 30, 30)
        }

        val popup = PopupWindow(rootLayout, 600, ViewGroup.LayoutParams.WRAP_CONTENT, true)
        popup.elevation = 50f
        popup.animationStyle = android.R.style.Animation_Dialog

        val title = TextView(this).apply {
            text = file.name; setTextColor(Color.GRAY); setTextSize(12f); setPadding(20, 0, 0, 20)
        }
        rootLayout.addView(title)

        val options = if (currentFolder == null) arrayOf("Rename", "Remove Folder") else arrayOf("Rename", "Delete File")
        options.forEachIndexed { index, s ->
            val btn = Button(this).apply {
                text = s; setTextColor(Color.WHITE); setBackgroundColor(0)
                gravity = Gravity.START or Gravity.CENTER_VERTICAL
                isAllCaps = false
            }
            btn.setOnClickListener {
                if (index == 0) showRenameDialog(file) else {
                    if (currentFolder == null) {
                        rootFolders.removeAt(pos - 1); saveFoldersToPrefs(); refreshListView()
                    } else showDeleteConfirm(file)
                }
                popup.dismiss()
            }
            rootLayout.addView(btn)
        }
        // Munculin di tengah layar biar keren
        popup.showAtLocation(drawerLayout, Gravity.CENTER, 0, 0)
        
        // Kasih efek gelap di belakang popup
        val container = popup.contentView.rootView
        val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val p = container.layoutParams as WindowManager.LayoutParams
        p.flags = p.flags or WindowManager.LayoutParams.FLAG_DIM_BEHIND
        p.dimAmount = 0.5f
        wm.updateViewLayout(container, p)
    }

    // --- SETUP SHORTCUTS (UNDO/REDO) ---
    private fun setupShortcuts() {
        val sc = findViewById<LinearLayout>(R.id.shortcut_layout)
        sc.removeAllViews()
        val actions = mapOf("↩️" to "editorUndo()", "↪️" to "editorRedo()")
        actions.forEach { (lbl, js) ->
            val b = createBtn(lbl).apply { setOnClickListener { editorWebView.evaluateJavascript(js, null) } }
            sc.addView(b)
        }
        val items = arrayOf("TAB", "<", ">", "/", "{", "}", "(", ")", ";", "\"", "'")
        items.forEach { lbl ->
            val b = createBtn(lbl).apply { setOnClickListener { editorWebView.evaluateJavascript("handleShortcut('$lbl')", null) } }
            sc.addView(b)
        }
    }

    private fun createBtn(label: String) = Button(this).apply {
        text = label; setTextColor(Color.WHITE); setBackgroundColor(0)
        layoutParams = LinearLayout.LayoutParams(130, -1)
    }

    // --- SETUP TABS ---
    private fun openFileWithTab(file: DocumentFile, save: Boolean = true) {
        if (openedTabs.containsKey(file.uri)) { openedTabs[file.uri]?.select(); return }
        val newTab = tabLayout.newTab()
        val customView = LayoutInflater.from(this).inflate(R.layout.custom_tab_item, null)
        customView.findViewById<ImageView>(R.id.tab_icon).setImageResource(getFileIcon(file))
        customView.findViewById<TextView>(R.id.tab_title).text = file.name
        customView.findViewById<ImageButton>(R.id.btn_close_tab).setOnClickListener {
            openedTabs.remove(file.uri); tabToUri.remove(newTab); tabLayout.removeTab(newTab)
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
            val name = DocumentFile.fromSingleUri(this, uri)?.name ?: "file.html"
            val ext = name.substringAfterLast(".", "html")
            val mode = when(ext) { "js" -> "javascript"; "css" -> "css"; "json" -> "json"; else -> "html" }
            val escaped = content.replace("\\", "\\\\").replace("'", "\\'").replace("\n", "\\n")
            editorWebView.evaluateJavascript("setCode('$escaped', '$mode')", null)
        } catch (e: Exception) {}
    }

    private fun setupExplorer() {
        findViewById<ImageButton>(R.id.btn_add_folder).setOnClickListener { startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT_TREE), 1001) }
        findViewById<ImageButton>(R.id.btn_save).setOnClickListener { saveCurrentFile() }
        findViewById<ImageButton>(R.id.btn_play).setOnClickListener { runPreview() }
        findViewById<ImageButton>(R.id.btn_open_drawer).setOnClickListener { drawerLayout.openDrawer(Gravity.LEFT) }
        findViewById<Button>(R.id.btn_new_file).setOnClickListener { showCreateFileDialog() }
        listFiles.setOnItemLongClickListener { _, _, pos, _ ->
            val files = getVisibleFiles()
            val actualPos = if (currentFolder != null) pos - 1 else pos
            if (actualPos >= 0 && actualPos < files.size) {
                val selected = files[actualPos]
                if (selected.name != "settings.json") showModernPopup(selected, actualPos)
            }
            true
        }
        refreshListView()
    }

    private fun refreshListView() {
        val files = getVisibleFiles()
        val adapter = object : ArrayAdapter<DocumentFile>(this, 0, files) {
            override fun getView(pos: Int, conv: View?, par: ViewGroup): View {
                val layout = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL; setPadding(35, 30, 35, 30); gravity = Gravity.CENTER_VERTICAL }
                val file = getItem(pos)!!
                layout.addView(ImageView(context).apply { layoutParams = LinearLayout.LayoutParams(55, 55); setImageResource(getFileIcon(file)) })
                layout.addView(TextView(context).apply { text = "   " + file.name; setTextColor(Color.WHITE); setTextSize(15f) })
                return layout
            }
        }
        if (currentFolder != null) {
            val header = Button(this).apply { text = "← BACK"; setTextColor(Color.CYAN); setBackgroundColor(0)
                setOnClickListener { currentFolder = currentFolder?.parentFile; if (rootFolders.any { it.uri == currentFolder?.uri }) currentFolder = null; refreshListView() }
            }
            if (listFiles.headerViewsCount == 0) listFiles.addHeaderView(header)
        } else { while (listFiles.headerViewsCount > 0) { listFiles.removeHeaderView(listFiles.getChildAt(0)) } }
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

    private fun saveFoldersToPrefs() { getSharedPreferences("XC", MODE_PRIVATE).edit().putStringSet("roots", rootFolders.map { it.uri.toString() }.toSet()).apply() }
    private fun loadFoldersFromPrefs() {
        getSharedPreferences("XC", MODE_PRIVATE).getStringSet("roots", null)?.forEach {
            val uri = Uri.parse(it)
            contentResolver.takePersistableUriPermission(uri, 3)
            DocumentFile.fromTreeUri(this, uri)?.let { rootFolders.add(it) }
        }
        refreshListView()
    }

    private fun saveTabsToPrefs() { getSharedPreferences("XC", MODE_PRIVATE).edit().putStringSet("tabs", openedTabs.keys.map { it.toString() }.toSet()).apply() }
    private fun restoreTabsFromPrefs() {
        getSharedPreferences("XC", MODE_PRIVATE).getStringSet("tabs", null)?.forEach {
            val uri = Uri.parse(it)
            if (uri.toString().contains("settings.json")) openFileWithTab(DocumentFile.fromFile(settingsManager.settingsFile), false)
            else DocumentFile.fromSingleUri(this, uri)?.let { if(it.exists()) openFileWithTab(it, false) }
        }
    }

    private fun getFileIcon(file: DocumentFile): Int {
        if (file.isDirectory) return R.drawable.ic_folder
        val n = file.name?.lowercase() ?: ""
        return when {
            n.endsWith(".html") -> R.drawable.ic_html; n.endsWith(".js") -> R.drawable.ic_js
            n.endsWith(".css") -> R.drawable.ic_css; else -> R.drawable.ic_files
        }
    }

    private fun setupAceEditor() {
        editorWebView.settings.apply { javaScriptEnabled = true; domStorageEnabled = true; allowFileAccess = true }
        editorWebView.webViewClient = WebViewClient()
        editorWebView.loadUrl("file:///android_asset/editor.html")
    }

    private fun setupTabs() {
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) { tabToUri[tab]?.let { loadContentToEditor(it) } }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun showRenameDialog(file: DocumentFile) {
        val input = EditText(this).apply { setText(file.name) }
        AlertDialog.Builder(this).setTitle("Rename").setView(input).setPositiveButton("OK") { _, _ -> file.renameTo(input.text.toString()); refreshListView() }.show()
    }

    private fun showDeleteConfirm(file: DocumentFile) {
        AlertDialog.Builder(this).setTitle("Delete").setMessage("Are you sure?").setPositiveButton("Yes") { _, _ -> file.delete(); refreshListView() }.show()
    }

    private fun showCreateFileDialog() {
        val input = EditText(this).apply { hint = "index.html" }
        AlertDialog.Builder(this).setTitle("New File").setView(input).setPositiveButton("Create") { _, _ ->
            currentFolder?.createFile("text/plain", input.text.toString()); refreshListView()
        }.show()
    }

    private fun applySettingsToEditor() { editorWebView.evaluateJavascript("applySettings(${settingsManager.loadSettings()})", null) }

    private fun runPreview() {
        editorWebView.evaluateJavascript("getCode()") { code ->
            val clean = code.unescapeJava()
            startActivity(Intent(this, PreviewActivity::class.java).apply { putExtra("html_code", clean) })
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1001 && resultCode == RESULT_OK) {
            data?.data?.let { uri ->
                contentResolver.takePersistableUriPermission(uri, 3)
                DocumentFile.fromTreeUri(this, uri)?.let { rootFolders.add(it); saveFoldersToPrefs(); refreshListView() }
            }
        }
    }
}

