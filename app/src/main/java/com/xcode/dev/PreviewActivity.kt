package com.xcode.dev

import android.graphics.Color
import android.os.Bundle
import android.util.TypedValue
import android.webkit.*
import android.widget.*
import android.view.Gravity
import androidx.appcompat.app.AppCompatActivity

class PreviewActivity : AppCompatActivity() {
    private lateinit var webView: WebView
    private lateinit var urlBar: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.parseColor("#121212")
        
        // Root Layout
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#121212"))
        }

        // Toolbar / Browser Header
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(30, 20, 30, 20)
            setBackgroundColor(Color.parseColor("#1E1E1E"))
            gravity = Gravity.CENTER_VERTICAL
        }

        urlBar = TextView(this).apply {
            text = "http://localhost:8080/index.html"
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

        // WebView Setup
        webView = WebView(this).apply {
            layoutParams = LinearLayout.LayoutParams(-1, -1)
            
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                databaseEnabled = true
                
                // KUNCI: Izin akses file lokal (CSS/JS terpisah)
                allowFileAccess = true
                allowContentAccess = true
                allowFileAccessFromFileURLs = true
                allowUniversalAccessFromFileURLs = true
                
                // Support buat layar HP modern
                useWideViewPort = true
                loadWithOverviewMode = true
            }
            
            webViewClient = WebViewClient()
            setBackgroundColor(Color.TRANSPARENT) // Biar nggak flicker putih pas load
        }
        
        root.addView(webView)
        setContentView(root)
        
        // Ambil Data dari Intent
        val code = intent.getStringExtra("html_code") ?: ""
        val baseUri = intent.getStringExtra("base_uri") ?: ""

        if (baseUri.isNotEmpty()) {
            // Load dengan Base URL folder tempat file berada
            // Jangan lupa tambah "/" di akhir baseUri
            webView.loadDataWithBaseURL(baseUri + "/", code, "text/html", "UTF-8", null)
            
            // Tampilan URL Bar biar gaya
            val folderName = baseUri.substringAfterLast("%2F").substringAfterLast("/")
            urlBar.text = "http://xcode.local/$folderName/index.html"
        } else {
            // Fallback kalau nggak ada folder (biasanya file default main.txt)
            webView.loadDataWithBaseURL("https://", code, "text/html", "UTF-8", null)
        }
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
}

