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
            setBackgroundColor(Color.WHITE)
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(25, 20, 25, 20)
            setBackgroundColor(Color.parseColor("#121212"))
            gravity = Gravity.CENTER_VERTICAL
        }

        val urlBar = TextView(this).apply {
            text = "http://localhost:8080/index.html"
            setTextColor(Color.parseColor("#BBBBBB"))
            setBackgroundColor(Color.parseColor("#252525"))
            setPadding(40, 15, 40, 15)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            layoutParams = LinearLayout.LayoutParams(0, -2, 1f).apply { setMargins(20, 0, 20, 0) }
        }

        header.addView(ImageView(this).apply { 
            setImageResource(android.R.drawable.ic_menu_compass)
            setColorFilter(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(50, 50) 
        })
        header.addView(urlBar)
        root.addView(header)

        webView = WebView(this).apply {
            layoutParams = LinearLayout.LayoutParams(-1, -1)
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowContentAccess = true
            settings.allowFileAccess = true
            webViewClient = WebViewClient()
        }
        
        root.addView(webView)
        setContentView(root)
        
        val code = intent.getStringExtra("html_code") ?: ""
        // Pake loadData biar CSS & JS external jalan
        webView.loadDataWithBaseURL("https://", code, "text/html", "UTF-8", null)
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) webView.goBack() else super.onBackPressed()
    }
}

