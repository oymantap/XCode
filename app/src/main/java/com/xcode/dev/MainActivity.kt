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

        // Buka file default biar gak kosong melompong
        editorWebView.postDelayed({
            if (tabLayout.tabCount == 0) openDefaultTab()
        }, 600)
    }

    private fun openDefaultTab() {
        val newTab = tabLayout.newTab()
        newTab.customView = createTabHeader("main.txt")
        tabLayout.addTab(newTab, true)
        tabUris[newTab.position] = null
        editorWebView.evaluateJavascript("setCode('', 'text')", null)
    }

    private fun createTabHeader(name: String): TextView {
        return TextView(this).apply {
            text = "$name  ✕"
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setPadding(12, 0, 12, 0)
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
            val currentUri = tabUris[tabLayout.selectedTabPosition] ?: return@setOnClickListener
            editorWebView.evaluateJavascript("getCode()") { code ->
                val cleanCode = code.trim('"').replace("\\n", "\n").replace("\\t", "\t").replace("\\\"", "\"").replace("\\\\", "\\")
                contentResolver.openOutputStream(currentUri, "wt")?.use { it.write(cleanCode.toByteArray()) }
                Toast.makeText(this, "Saved!", Toast.LENGTH_SHORT).show()
            }
        }

        findViewById<ImageButton>(R.id.btn_play).setOnClickListener {
            editorWebView.evaluateJavascript("getCode()") { code ->
                // FIX PREVIEW: Unescape Unicode (\u003C jadi <)
                val clean = code.trim('"')
                    .replace("\\n", "\n")
                    .replace("\\t", "\t")
                    .replace("\\\"", "\"")
                    .replace("\\u003C", "<")
                    .replace("\\u003E", ">")
                    .replace("\\u0026", "&")
                
                val intent = Intent(this, PreviewActivity::class.java)
                intent.putExtra("html_code", clean)
                startActivity(intent)
            }
        }

        findViewById<ImageButton>(R.id.btn_open_drawer).setOnClickListener { drawerLayout.openDrawer(Gravity.LEFT) }
        findViewById<Button>(R.id.btn_new_file).setOnClickListener { showCreateFileDialog() }
        findViewById<Button>(R.id.btn_back_root)?.setOnClickListener { currentFolder = null; refreshListView() }
    }

    private fun openFileWithTab(file: DocumentFile) {
        for (i in 0 until tabLayout.tabCount) {
            if (tabUris[i] == file.uri) { tabLayout.getTabAt(i)?.select(); return }
        }
        val content = contentResolver.openInputStream(file.uri)?.bufferedReader()?.use { it.readText() } ?: ""
        val newTab = tabLayout.newTab().apply { customView = createTabHeader(file.name ?: "file") }
        tabLayout.addTab(newTab, true)
        tabUris[newTab.position] = file.uri
        
        val ext = file.name?.substringAfterLast(".", "html") ?: "html"
        val mode = when(ext) { "js" -> "javascript"; "py" -> "python"; "css" -> "css"; "json" -> "json"; else -> "html" }
        val escaped = content.replace("\\", "\\\\").replace("'", "\\'").replace("\n", "\\n")
        editorWebView.evaluateJavascript("setCode('$escaped', '$mode')", null)
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

    private fun setupTabs() {
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                val fileName = (tab?.customView as? TextView)?.text.toString()
                findViewById<ImageButton>(R.id.btn_play).visibility = if (fileName.contains(".html")) View.VISIBLE else View.GONE
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {
                if (tabLayout.tabCount > 0) {
                    val p = tab?.position ?: return
                    tabUris.remove(p); tabLayout.removeTabAt(p)
                    if (tabLayout.tabCount == 0) openDefaultTab()
                }
            }
        })
    }

    private fun refreshListView() {
        val displayList = mutableListOf<String>()
        val files = if (currentFolder == null) rootFolders.toTypedArray() else {
            displayList.add("← [ BACK ]"); currentFolder?.listFiles() ?: emptyArray()
        }
        files.forEach { if (it.name != null) displayList.add((if (it.isDirectory) "📁 " else "📄 ") + it.name) }
        listFiles.adapter = object : ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, displayList) {
            override fun getView(pos: Int, conv: View?, par: ViewGroup): View {
                val v = super.getView(pos, conv, par); (v as TextView).setTextColor(Color.WHITE); return v
            }
        }
        listFiles.setOnItemClickListener { _, _, position, _ ->
            if (currentFolder != null && position == 0) {
                currentFolder = if (rootFolders.any { it.uri == currentFolder?.uri }) null else currentFolder?.parentFile
                refreshListView(); return@setOnItemClickListener
            }
            val actualPos = if (currentFolder == null) position else position - 1
            val selected = files[actualPos]
            if (selected.isDirectory) { currentFolder = selected; refreshListView() }
            else { openFileWithTab(selected); drawerLayout.closeDrawers() }
        }
    }

    private fun loadFoldersFromPrefs() {
        val set = getSharedPreferences("XCode", Context.MODE_PRIVATE).getStringSet("root_uris", null)
        set?.forEach {
            val uri = Uri.parse(it)
            try {
                contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                DocumentFile.fromTreeUri(this, uri)?.let { doc -> rootFolders.add(doc) }
            } catch (e: Exception) {}
        }
        refreshListView()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1001 && resultCode == RESULT_OK) {
            data?.data?.let { uri ->
                contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                DocumentFile.fromTreeUri(this, uri)?.let { 
                    rootFolders.add(it)
                    getSharedPreferences("XCode", Context.MODE_PRIVATE).edit().putStringSet("root_uris", rootFolders.map { f -> f.uri.toString() }.toSet()).apply()
                    refreshListView() 
                }
            }
        }
    }

    private fun showCreateFileDialog() {
        val input = EditText(this)
        AlertDialog.Builder(this).setTitle("New File").setView(input).setPositiveButton("Create") { _, _ ->
            currentFolder?.createFile("text/plain", input.text.toString()); refreshListView()
        }.show()
    }
}

