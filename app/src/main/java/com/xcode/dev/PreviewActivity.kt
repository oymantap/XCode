package com.xcode.dev

import android.graphics.Color
import android.os.Bundle
import android.webkit.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.documentfile.provider.DocumentFile
import android.net.Uri

class PreviewActivity : AppCompatActivity() {
    
    private lateinit var webView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        window.statusBarColor = Color.parseColor("#1E1E1E")
        
        // UI MODERN HEADER
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            backgroundColor = Color.parseColor("#0D0D0D")
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(20, 15, 20, 15)
            backgroundColor = Color.parseColor("#1E1E1E")
            gravity = android.view.Gravity.CENTER_VERTICAL
        }

        val favicon = ImageView(this).apply {
            setImageResource(android.R.drawable.ic_menu_compass)
            layoutParams = LinearLayout.LayoutParams(40, 40)
        }

        val urlBar = TextView(this).apply {
            text = "http://localhost:7777/index.html"
            setTextColor(Color.parseColor("#808080"))
            setBackgroundResource(android.R.drawable.editbox_dropdown_light_frame)
            setPadding(30, 10, 30, 10)
            textSize = 12sp
            layoutParams = LinearLayout.LayoutParams(0, -2, 1f).apply { setMargins(20, 0, 20, 0) }
        }

        header.addView(favicon)
        header.addView(urlBar)
        root.addView(header)

        webView = WebView(this).apply {
            layoutParams = LinearLayout.LayoutParams(-1, -1)
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            webViewClient = object : WebViewClient() {
                // LOGIC BACA FILE TERPISAH (.js, .css)
                override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
                    // Di sini lo bisa tambahin logic mapping URI ke DocumentFile kalau mau full offline
                    return super.shouldInterceptRequest(view, request)
                }
            }
        }
        
        root.addView(webView)
        setContentView(root)
        
        val code = intent.getStringExtra("html_code") ?: ""
        webView.loadDataWithBaseURL("http://localhost:7777/", code, "text/html", "UTF-8", null)
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) webView.goBack() else super.onBackPressed()
    }
}

