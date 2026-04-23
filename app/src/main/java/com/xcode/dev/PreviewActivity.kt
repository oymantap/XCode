package com.xcode.dev

import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.TypedValue
import android.webkit.*
import android.widget.*
import android.view.Gravity
import androidx.appcompat.app.AppCompatActivity
import androidx.documentfile.provider.DocumentFile
import java.io.InputStream

class PreviewActivity : AppCompatActivity() {
    private lateinit var webView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.parseColor("#121212")
        
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#121212"))
        }

        webView = WebView(this).apply {
            layoutParams = LinearLayout.LayoutParams(-1, -1)
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = true
            webViewClient = WebViewClient()
        }
        
        root.addView(webView)
        setContentView(root)
        
        val code = intent.getStringExtra("html_code") ?: ""
        val baseUriString = intent.getStringExtra("base_uri") ?: ""
        
        // PROSES INJEKSI OTOMATIS
        val finalCode = if (baseUriString.isNotEmpty()) {
            injectAssets(code, Uri.parse(baseUriString))
        } else {
            code
        }

        webView.loadDataWithBaseURL("https://xcode.local", finalCode, "text/html", "UTF-8", null)
    }

    private fun injectAssets(html: String, folderUri: Uri): String {
        var mergedHtml = html
        val parentFolder = DocumentFile.fromTreeUri(this, folderUri) ?: return html

        // 1. Cari semua tag <link rel="stylesheet" href="...">
        val cssRegex = """<link[^>]+href=["']([^"']+)["'][^>]*>""".toRegex()
        cssRegex.findAll(html).forEach { match ->
            val fileName = match.groups[1]?.value ?: ""
            val file = parentFolder.findFile(fileName)
            if (file != null) {
                val cssContent = contentResolver.openInputStream(file.uri)?.bufferedReader()?.use { it.readText() } ?: ""
                mergedHtml = mergedHtml.replace(match.value, "<style>$cssContent</style>")
            }
        }

        // 2. Cari semua tag <script src="..."></script>
        val jsRegex = """<script[^>]+src=["']([^"']+)["'][^>]*>\s*</script>""".toRegex()
        jsRegex.findAll(mergedHtml).forEach { match ->
            val fileName = match.groups[1]?.value ?: ""
            val file = parentFolder.findFile(fileName)
            if (file != null) {
                val jsContent = contentResolver.openInputStream(file.uri)?.bufferedReader()?.use { it.readText() } ?: ""
                mergedHtml = mergedHtml.replace(match.value, "<script>$jsContent</script>")
            }
        }

        return mergedHtml
    }
}
