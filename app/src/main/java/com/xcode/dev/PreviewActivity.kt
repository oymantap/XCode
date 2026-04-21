package com.xcode.dev

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity

class PreviewActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 1. SET STATUS BAR BLACK (Biar sinkron sama editor)
        window.statusBarColor = Color.parseColor("#1E1E1E")
        window.decorView.systemUiVisibility = 0 // Teks status bar putih

        val webView = WebView(this)
        setContentView(webView)
        
        val code = intent.getStringExtra("html_code") ?: "<h1>Gada kode buat di-run, Rycl!</h1>"
        
        // 2. KONFIGURASI WEBVIEW PRO
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true // Biar bisa simpan cache/localstorage web
            useWideViewPort = true
            loadWithOverviewMode = true
        }

        // 3. SET CLIENT (Biar link localhost tetep di sini)
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                return false // Handle semua URL di dalam WebView ini
            }
        }

        // 4. LOAD SEBAGAI LOCALHOST:7777
        webView.loadDataWithBaseURL(
            "http://localhost:7777/", 
            code, 
            "text/html", 
            "UTF-8", 
            null
        )
    }

    // Biar kalo di-back gak langsung keluar app kalo lagi browsing di preview
    override fun onBackPressed() {
        val webView = contentView as? WebView
        if (webView?.canGoBack() == true) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
}

