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
            layoutParams = LinearLayout.LayoutParams(0, -2, 1f).apply { setMargins(20, 0, 20, 0) }
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
        val parentFolder = if (baseUriString.isNotEmpty()) DocumentFile.fromTreeUri(this, Uri.parse(baseUriString)) else null

        webView = WebView(this).apply {
            layoutParams = LinearLayout.LayoutParams(-1, -1)
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                allowFileAccess = true
                allowContentAccess = true
            }
            
            // JEMBATAN KEAJAIBAN: Biar bisa baca file terpisah
            webViewClient = object : WebViewClient() {
                override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
                    val url = request?.url?.toString() ?: ""
                    
                    // Kalau HTML minta file lokal (kayak style.css atau game.js)
                    if (url.startsWith("http://xcode.local/") && parentFolder != null) {
                        val fileName = url.replace("http://xcode.local/", "")
                        val file = parentFolder.findFile(fileName)
                        
                        if (file != null) {
                            try {
                                val inputStream = contentResolver.openInputStream(file.uri)
                                val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(file.name?.substringAfterLast(".")) ?: "text/plain"
                                return WebResourceResponse(mimeType, "UTF-8", inputStream)
                            } catch (e: Exception) { e.printStackTrace() }
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
            // Kita pake domain palsu http://xcode.local/ sebagai pancingan
            webView.loadDataWithBaseURL("http://xcode.local/", code, "text/html", "UTF-8", null)
            urlBar.text = "http://xcode.local/index.html"
        } else {
            webView.loadData(code, "text/html", "UTF-8")
        }
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) webView.goBack() else super.onBackPressed()
    }
}
