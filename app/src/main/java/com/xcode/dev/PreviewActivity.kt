package com.xcode.dev

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity

class PreviewActivity : AppCompatActivity() {
    
    private lateinit var mainWebView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 1. SET STATUS BAR BLACK
        window.statusBarColor = Color.parseColor("#1E1E1E")
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = 0 

        mainWebView = WebView(this)
        setContentView(mainWebView)
        
        val code = intent.getStringExtra("html_code") ?: "<h1>Gada kode buat di-run, Rycl!</h1>"
        
        // 2. KONFIGURASI WEBVIEW PRO
        mainWebView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true 
            useWideViewPort = true
            loadWithOverviewMode = true
        }

        // 3. SET CLIENT
        mainWebView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                return false 
            }
        }

        // 4. LOAD SEBAGAI LOCALHOST:7777
        mainWebView.loadDataWithBaseURL(
            "http://localhost:7777/", 
            code, 
            "text/html", 
            "UTF-8", 
            null
        )
    }

    override fun onBackPressed() {
        if (mainWebView.canGoBack()) {
            mainWebView.goBack()
        } else {
            super.onBackPressed()
        }
    }
}

