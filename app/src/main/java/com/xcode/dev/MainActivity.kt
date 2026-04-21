package com.xcode.dev

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.text.*
import android.text.style.ForegroundColorSpan
import android.view.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.documentfile.provider.DocumentFile
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.tabs.TabLayout
import java.util.regex.Pattern

class MainActivity : AppCompatActivity() {

    private lateinit var editor: EditText
    private lateinit var listFiles: ListView
    private lateinit var tabLayout: TabLayout
    private lateinit var drawerLayout: DrawerLayout
    
    private var currentFolder: DocumentFile? = null
    private val rootFolders = mutableListOf<DocumentFile>()
    private val tabUris = mutableMapOf<Int, Uri?>()
    private val fileContents = mutableMapOf<Int, String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        editor = findViewById(R.id.editor)
        listFiles = findViewById(R.id.list_files)
        tabLayout = findViewById(R.id.tab_layout)
        drawerLayout = findViewById(R.id.drawer_layout)

        setupExplorer()
        setupTabs()
        setupShortcuts()
        setupHighlighter()
        loadFoldersFromPrefs()
    }

    private fun setupExplorer() {
        findViewById<ImageButton>(R.id.btn_add_folder).setOnClickListener {
            startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT_TREE), 1001)
        }
        findViewById<ImageButton>(R.id.btn_save).setOnClickListener { saveCurrentFile() }
        findViewById<ImageButton>(R.id.btn_play).setOnClickListener {
            val intent = Intent(this, PreviewActivity::class.java)
            intent.putExtra("html_code", editor.text.toString())
            startActivity(intent)
        }
        findViewById<ImageButton>(R.id.btn_open_drawer).setOnClickListener { drawerLayout.openDrawer(Gravity.LEFT) }
        
        findViewById<Button>(R.id.btn_new_file).setOnClickListener { showCreateFileDialog() }
    }

    private fun refreshListView() {
        val displayList = mutableListOf<String>()
        val files = if (currentFolder == null) {
            if (rootFolders.isEmpty()) displayList.add("No Folder Connected")
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
                v.setPadding(30, 30, 30, 30)
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
        // Filter Spam
        for (i in 0 until tabLayout.tabCount) {
            if (tabUris[i] == file.uri) {
                tabLayout.getTabAt(i)?.select()
                return
            }
        }

        val content = contentResolver.openInputStream(file.uri)?.bufferedReader()?.use { it.readText() } ?: ""
        val newTab = tabLayout.newTab()
        
        // CUSTOM TAB VIEW WITH X
        val tabView = LayoutInflater.from(this).inflate(android.R.layout.simple_list_item_1, null) as TextView
        tabView.text = file.name + "  ✕"
        tabView.setTextColor(Color.WHITE)
        tabView.textSize = 12sp
        newTab.customView = tabView

        tabLayout.addTab(newTab, true)
        val pos = newTab.position
        tabUris[pos] = file.uri
        fileContents[pos] = content
        editor.setText(content)
        
        // Logic Close on Click X (Simulasi lewat reselect karena custom view)
    }

    private fun setupTabs() {
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                editor.setText(fileContents[tab?.position ?: 0] ?: "")
                findViewById<ImageButton>(R.id.btn_play).visibility = 
                    if (tab?.text?.contains(".html") == true || tab?.customView != null) View.VISIBLE else View.GONE
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {
                fileContents[tab?.position ?: 0] = editor.text.toString()
            }
            override fun onTabReselected(tab: TabLayout.Tab?) {
                // TUTUP TAB
                if (tabLayout.tabCount > 1) {
                    val p = tab?.position ?: return
                    tabUris.remove(p)
                    fileContents.remove(p)
                    tabLayout.removeTabAt(p)
                }
            }
        })
    }

    private fun setupHighlighter() {
        editor.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun onTextChanged(s: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val text = s.toString()
                s?.clearSpans()
                // Pro Highlighting Regex
                val tagPattern = Pattern.compile("<[^>]*>")
                val attrPattern = Pattern.compile("\\s[a-zA-Z-]+=")
                val stringPattern = Pattern.compile("\"[^\"]*\"|'[^']*'")
                
                val mTags = tagPattern.matcher(text)
                while (mTags.find()) s?.setSpan(ForegroundColorSpan(Color.parseColor("#569CD6")), mTags.start(), mTags.end(), 0)
                
                val mStrings = stringPattern.matcher(text)
                while (mStrings.find()) s?.setSpan(ForegroundColorSpan(Color.parseColor("#CE9178")), mStrings.start(), mStrings.end(), 0)
            }
        })
    }

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

    private fun saveCurrentFile() {
        val uri = tabUris[tabLayout.selectedTabPosition] ?: return
        contentResolver.openOutputStream(uri, "wt")?.use { it.write(editor.text.toString().toByteArray()) }
        Toast.makeText(this, "Saved to Storage", Toast.LENGTH_SHORT).show()
    }

    private fun setupShortcuts() {
        val sc = findViewById<LinearLayout>(R.id.shortcut_layout)
        val items = arrayOf("!", "TAB", "<", ">", "/", "{", "}", "(", ")", ";", "=", "\"", "'")
        items.forEach { label ->
            val b = Button(this).apply {
                text = label
                setTextColor(Color.WHITE)
                setBackgroundColor(0)
            }
            b.setOnClickListener { editor.text.insert(editor.selectionStart, label) }
            sc.addView(b)
        }
    }
    
    private fun showCreateFileDialog() {
        val input = EditText(this)
        AlertDialog.Builder(this).setTitle("New File").setView(input)
            .setPositiveButton("Create") { _, _ ->
                currentFolder?.createFile("*/*", input.text.toString())
                refreshListView()
            }.show()
    }
}

