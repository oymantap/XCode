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

    // Ganti EditText ke WebView untuk Ace Editor
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
        loadFoldersFromPrefs()
    }

    private fun setupAceEditor() {
        editorWebView.settings.javaScriptEnabled = true
        editorWebView.settings.domStorageEnabled = true
        editorWebView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                // Editor siap digunakan
            }
        }
        editorWebView.loadUrl("file:///android_asset/editor.html")
    }

    private fun setupExplorer() {
        findViewById<ImageButton>(R.id.btn_add_folder).setOnClickListener {
            startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT_TREE), 1001)
        }
        
        // SAVE: Ambil kode dari Ace Editor
        findViewById<ImageButton>(R.id.btn_save).setOnClickListener {
            val currentUri = tabUris[tabLayout.selectedTabPosition] ?: return@setOnClickListener
            editorWebView.evaluateJavascript("getCode()") { code ->
                // Bersihkan quote dari JS evaluate
                val cleanCode = code.trim('"').replace("\\n", "\n").replace("\\'", "'").replace("\\\\", "\\")
                contentResolver.openOutputStream(currentUri, "wt")?.use { 
                    it.write(cleanCode.toByteArray()) 
                }
                Toast.makeText(this, "Saved!", Toast.LENGTH_SHORT).show()
            }
        }

        findViewById<ImageButton>(R.id.btn_play).setOnClickListener {
            editorWebView.evaluateJavascript("getCode()") { code ->
                val cleanCode = code.trim('"').replace("\\n", "\n").replace("\\'", "'")
                val intent = Intent(this, PreviewActivity::class.java)
                intent.putExtra("html_code", cleanCode)
                startActivity(intent)
            }
        }

        findViewById<ImageButton>(R.id.btn_open_drawer).setOnClickListener { drawerLayout.openDrawer(Gravity.LEFT) }
        findViewById<Button>(R.id.btn_new_file).setOnClickListener { showCreateFileDialog() }
        
        // Fitur Back ke Home/Root
        findViewById<Button>(R.id.btn_back_root)?.setOnClickListener {
            currentFolder = null
            refreshListView()
        }
    }

    private fun refreshListView() {
        val displayList = mutableListOf<String>()
        val files = if (currentFolder == null) {
            rootFolders.toTypedArray()
        } else {
            displayList.add("← [ BACK ]")
            currentFolder?.listFiles() ?: emptyArray()
        }

        files.forEach { if (it.name != null) displayList.add((if (it.isDirectory) "📁 " else "📄 ") + it.name) }

        val adapter = object : ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, displayList) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val v = super.getView(position, convertView, parent)
                (v as TextView).setTextColor(Color.WHITE)
                return v
            }
        }
        listFiles.adapter = adapter
        
        listFiles.setOnItemClickListener { _, _, position, _ ->
            if (currentFolder != null && position == 0) {
                currentFolder = if (rootFolders.any { it.uri == currentFolder?.uri }) null else currentFolder?.parentFile
                refreshListView()
                return@setOnItemClickListener
            }

            val actualPos = if (currentFolder == null) position else position - 1
            val selected = files[actualPos]
            
            if (selected.isDirectory) {
                currentFolder = selected
                refreshListView()
            } else {
                openFileWithTab(selected)
                drawerLayout.closeDrawers()
            }
        }
    }

    private fun openFileWithTab(file: DocumentFile) {
        for (i in 0 until tabLayout.tabCount) {
            if (tabUris[i] == file.uri) {
                tabLayout.getTabAt(i)?.select()
                return
            }
        }

        val content = contentResolver.openInputStream(file.uri)?.bufferedReader()?.use { it.readText() } ?: ""
        val newTab = tabLayout.newTab()
        
        // FIXED: Pake TypedValue biar build nggak gagal
        val tabView = TextView(this).apply {
            text = "${file.name}  ✕"
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
        }
        newTab.customView = tabView

        tabLayout.addTab(newTab, true)
        val pos = newTab.position
        tabUris[pos] = file.uri
        
        // Deteksi bahasa buat Ace Editor
        val ext = file.name?.substringAfterLast(".", "html") ?: "html"
        val mode = when(ext) {
            "js" -> "javascript"
            "css" -> "css"
            else -> "html"
        }
        
        val escaped = content.replace("'", "\\'").replace("\n", "\\n")
        editorWebView.evaluateJavascript("setCode('$escaped', '$mode')", null)
    }

    private fun setupTabs() {
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                // Logic ambil content dari map jika perlu (persistent state)
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {
                if (tabLayout.tabCount > 1) {
                    val p = tab?.position ?: return
                    tabUris.remove(p)
                    tabLayout.removeTabAt(p)
                }
            }
        })
    }

    // Persistence Logic
    private fun saveFoldersToPrefs() {
        val uris = rootFolders.map { it.uri.toString() }.toSet()
        getSharedPreferences("XCode", Context.MODE_PRIVATE).edit().putStringSet("root_uris", uris).apply()
    }

    private fun loadFoldersFromPrefs() {
        val set = getSharedPreferences("XCode", Context.MODE_PRIVATE).getStringSet("root_uris", null)
        set?.forEach {
            val uri = Uri.parse(it)
            contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            DocumentFile.fromTreeUri(this, uri)?.let { doc -> rootFolders.add(doc) }
        }
        refreshListView()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1001 && resultCode == RESULT_OK) {
            val uri = data?.data ?: return
            contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            DocumentFile.fromTreeUri(this, uri)?.let {
                rootFolders.add(it)
                saveFoldersToPrefs()
                refreshListView()
            }
        }
    }

    private fun showCreateFileDialog() {
        val input = EditText(this)
        AlertDialog.Builder(this).setTitle("New File").setView(input)
            .setPositiveButton("Create") { _, _ ->
                currentFolder?.createFile("text/plain", input.text.toString())
                refreshListView()
            }.show()
    }

    private fun setupShortcuts() {
        val sc = findViewById<LinearLayout>(R.id.shortcut_layout)
        val items = arrayOf("!", "TAB", "<", ">", "/", "{", "}", "(", ")", ";", "*", "+", "-", "=", "\"", "'")
        items.forEach { label ->
            val b = Button(this).apply {
                text = label
                setTextColor(Color.WHITE)
                setBackgroundColor(0)
            }
            b.setOnClickListener { 
                // Insert shortcut via JS ke Ace Editor
                editorWebView.evaluateJavascript("editor.insert('$label')", null)
            }
            sc.addView(b)
        }
    }
}

