package com.xcode.dev

import android.os.Bundle
import android.webkit.WebView
import androidx.appcompat.app.AppCompatActivity

class PreviewActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val webView = WebView(this)
        setContentView(webView)
        
        val code = intent.getStringExtra("code") ?: ""
        // Load code as HTML
        webView.settings.javaScriptEnabled = true
        webView.loadDataWithBaseURL("http://localhost:7777/", code, "text/html", "UTF-8", null)
    }
}

