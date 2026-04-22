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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.parseColor("#121212")
        
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#121212")) // Fix Konsisten Item
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(30, 20, 30, 20)
            setBackgroundColor(Color.parseColor("#1E1E1E"))
            gravity = Gravity.CENTER_VERTICAL
        }

        val urlBar = TextView(this).apply {
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

        webView = WebView(this).apply {
            layoutParams = LinearLayout.LayoutParams(-1, -1)
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = true
            settings.databaseEnabled = true
            webViewClient = WebViewClient()
            setBackgroundColor(Color.WHITE) // Konten HTML tetep putih biar keliatan
        }
        
        root.addView(webView)
        setContentView(root)
        
        val code = intent.getStringExtra("html_code") ?: ""
        webView.loadDataWithBaseURL("https://", code, "text/html", "UTF-8", null)
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) webView.goBack() else super.onBackPressed()
    }
}

