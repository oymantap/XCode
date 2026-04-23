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

class PreviewActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var urlBar: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.parseColor("#121212")

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#121212"))
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(30, 20, 30, 20)
            setBackgroundColor(Color.parseColor("#1E1E1E"))
            gravity = Gravity.CENTER_VERTICAL
        }

        urlBar = TextView(this).apply {
            text = "http://xcode.local/preview"
            setTextColor(Color.parseColor("#888888"))
            setBackgroundColor(Color.parseColor("#2D2D2D"))
            setPadding(35, 12, 35, 12)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
        }

        header.addView(ImageView(this).apply {
            setImageResource(android.R.drawable.ic_menu_compass)
            setColorFilter(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(45, 45)
        })

        header.addView(urlBar)
        root.addView(header)

        val code = intent.getStringExtra("html_code") ?: ""
        val baseUriString = intent.getStringExtra("base_uri") ?: ""

        Toast.makeText(this, "BASE: $baseUriString", Toast.LENGTH_LONG).show()

        val parentFolder = if (baseUriString.isNotEmpty()) {
            DocumentFile.fromTreeUri(this, Uri.parse(baseUriString))
        } else null

        webView = WebView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
             0,
            1f
         )

            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                allowFileAccess = true
                allowContentAccess = true
                allowFileAccessFromFileURLs = true
                allowUniversalAccessFromFileURLs = true
            }

            webViewClient = object : WebViewClient() {

override fun shouldInterceptRequest(
    view: WebView?,
    request: WebResourceRequest?
): WebResourceResponse? {

    val url = request?.url?.toString() ?: return null

    var path = url.removePrefix("http://xcode.local/")
    path = Uri.decode(path).trimStart('/')

    if (path.isEmpty()) return null

    // 🔥 LOG 1
    android.util.Log.d("WEBVIEW", "REQ: $url")
    android.util.Log.d("WEBVIEW", "PATH: $path")

    if (url.startsWith("http://xcode.local/") && parentFolder != null) {

        val file = findFileRecursive(parentFolder, path)

        // 🔥 LOG 2
        android.util.Log.d("WEBVIEW", "FOUND: ${file?.name}")

        if (file != null) {
            try {
                val input = contentResolver.openInputStream(file.uri)

                val ext = file.name?.substringAfterLast(".", "") ?: ""
                val mime = MimeTypeMap.getSingleton()
                    .getMimeTypeFromExtension(ext.lowercase()) ?: "text/plain"

                return WebResourceResponse(mime, "UTF-8", input)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    return super.shouldInterceptRequest(view, request)
              }
            }

            setBackgroundColor(Color.WHITE)
        }

        root.addView(webView)
        setContentView(root)

        if (baseUriString.isNotEmpty()) {
            webView.loadDataWithBaseURL(
                "http://xcode.local/",
                code,
                "text/html",
                "UTF-8",
                null
            )
            urlBar.text = "http://xcode.local/index.html"
        } else {
            webView.loadData(code, "text/html", "UTF-8")
        }
    }

    // 🔥 SUPPORT PATH css/style.css / js/app.js
private fun findFileRecursive(folder: DocumentFile, path: String): DocumentFile? {
    val parts = path.split("/").filter { it.isNotEmpty() }

    var current: DocumentFile? = folder

    for (part in parts) {
        val next = current?.listFiles()?.find { it.name == part }
        if (next == null) return null
        current = next
    }

    return current
}

    override fun onBackPressed() {
        if (webView.canGoBack()) webView.goBack() else super.onBackPressed()
    }
}
