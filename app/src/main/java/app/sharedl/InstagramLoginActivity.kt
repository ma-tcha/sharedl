package app.sharedl

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class InstagramLoginActivity : Activity() {
    private lateinit var status: TextView
    private lateinit var continueButton: Button
    private lateinit var webView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 12, 16, 12)
        }
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(16 + bars.left, 12 + bars.top, 16 + bars.right, 12 + bars.bottom)
            insets
        }
        status = TextView(this).apply {
            text = "Connecte-toi à Instagram pour autoriser le téléchargement des liens qui demandent une session."
            textSize = 16f
            setPadding(0, 8, 0, 12)
        }
        root.addView(status)

        continueButton = Button(this).apply {
            text = "Utiliser cette session"
            visibility = View.GONE
            setOnClickListener {
                if (InstagramSession.captureCookies(this@InstagramLoginActivity)) {
                    setResult(RESULT_OK)
                    finish()
                }
            }
        }
        root.addView(continueButton)

        webView = WebView(this)
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                if (InstagramSession.captureCookies(this@InstagramLoginActivity)) {
                    status.text = "Session Instagram détectée. Appuie sur « Utiliser cette session » pour continuer."
                    continueButton.visibility = View.VISIBLE
                }
            }
        }
        root.addView(webView, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        setContentView(root)
        webView.loadUrl("https://www.instagram.com/accounts/login/")
    }

    override fun onDestroy() {
        if (::webView.isInitialized) webView.destroy()
        super.onDestroy()
    }
}
