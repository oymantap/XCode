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
        window.statusBarColor = Color.parseColor("#1E1E1E")
        
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#0D0D0D"))
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(20, 15, 20, 15)
            setBackgroundColor(Color.parseColor("#1E1E1E"))
            gravity = Gravity.CENTER_VERTICAL
        }

        val urlBar = TextView(this).apply {
            text = "http://localhost:7777/index.html"
            setTextColor(Color.parseColor("#808080"))
            setBackgroundResource(android.R.drawable.editbox_dropdown_light_frame)
            setPadding(30, 10, 30, 10)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            layoutParams = LinearLayout.LayoutParams(0, -2, 1f).apply { setMargins(20, 0, 20, 0) }
        }

        header.addView(ImageView(this).apply { 
            setImageResource(android.R.drawable.ic_menu_compass)
            layoutParams = LinearLayout.LayoutParams(48, 48) 
        })
        header.addView(urlBar)
        root.addView(header)

        webView = WebView(this).apply {
            layoutParams = LinearLayout.LayoutParams(-1, -1)
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.setSupportZoom(true)
            settings.builtInZoomControls = true
            settings.displayZoomControls = false
            webViewClient = WebViewClient()
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
