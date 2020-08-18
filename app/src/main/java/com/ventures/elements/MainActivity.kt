package com.ventures.elements

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.view.KeyEvent
import android.webkit.*
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricConstants
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.activity_main.*
import java.util.concurrent.Executor


class MainActivity : AppCompatActivity(R.layout.activity_main) {

    private lateinit var executor: Executor
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo
    private lateinit var storage: SharedPreferences

    private var deepLinking: Uri? = null

    @Suppress("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        deepLinking = intent?.data

        webView.setBackgroundColor(Color.BLACK)

        storage = getSharedPreferences(APP_STORAGE, Context.MODE_PRIVATE)

        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true

        webView.addJavascriptInterface(
            WebAppInterface(
                this
            ), "Android-App")
        webView.webViewClient = object: WebViewClient() {

            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                if (Uri.parse(url).host?.contains("spaces") == true) {
                    // This is my web site, so do not override; let my WebView load the page
                    return false
                }
                // Otherwise, the link is not for a page on my site, so launch another Activity that handles URLs
                Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                    startActivity(this)
                }
                return false
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
            }
        }
        webView.webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                consoleMessage?.apply {

                }
                return true
            }

            override fun onShowFileChooser(
                webView: WebView?,
                uploadMsg: ValueCallback<Array<Uri?>?>?,
                fileChooserParams: FileChooserParams?
            ): Boolean {
                openChooserDialog()
                return true
            }
        }

        showBiometric()
    }

    private fun openChooserDialog() {
        startActivityForResult(Intent(Intent.ACTION_PICK,
            android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI) , 101)
    }

    private fun showBiometric() {
        val showBiometric = storage.getBoolean(SHOW_BIOMETRICS_PROMPT, true) || storage.getBoolean(
            USER_ADDED_BIOMETRIC, false)

        if (showBiometric) {
            Handler().postDelayed({
                initializeBiometric()
                biometricPrompt.authenticate(promptInfo)
            }, 300)
        }
    }

    private fun initializeBiometric() {
        executor = ContextCompat.getMainExecutor(this)
        biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    if (errorCode == BiometricConstants.ERROR_NEGATIVE_BUTTON) {
                        // Don't show again
                        storage.edit().apply {
                            putBoolean(SHOW_BIOMETRICS_PROMPT, false)
                            apply()
                        }
                        loadUrl()
                    } else {
                        if (storage.getBoolean(USER_ADDED_BIOMETRIC, false)) {
                            finish()
                        } else {
                            loadUrl()
                        }
                    }
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    storage.edit().apply {
                        putBoolean(USER_ADDED_BIOMETRIC, true)
                        apply()
                    }
                    loadUrl()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                }
            }
        )

        promptInfo = if (storage.getBoolean(USER_ADDED_BIOMETRIC, false)) {
            BiometricPrompt.PromptInfo.Builder()
                .setTitle(getString(R.string.login_title))
                .setSubtitle(getString(R.string.login_subtitle))
                .setNegativeButtonText(" ")
                .build()
        } else {
            BiometricPrompt.PromptInfo.Builder()
                .setTitle(getString(R.string.authorize_login_title))
                .setSubtitle(getString(R.string.authorize_login_subtitle))
                .setNegativeButtonText(getString(R.string.authorize_login_cancel))
                .build()
        }
    }

    private fun loadUrl() {
        webView.loadUrl(deepLinking?.toString() ?: URL)
        deepLinking = null
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            finish()
            return true
        }

        return super.onKeyDown(keyCode, event)
    }

    class WebAppInterface(private val context: Context) {

        @JavascriptInterface
        fun loginSuccess(token: String) {

        }

        @JavascriptInterface
        fun logout() {

        }
    }

    companion object {
        const val APP_STORAGE = "APP_STORAGE"
        const val SHOW_BIOMETRICS_PROMPT = "SHOW_BIOMETRICS_PROMPT"
        const val USER_ADDED_BIOMETRIC = "USER_ADDED_BIOMETRIC"
        const val URL = "https://spaces.elements.one/"
    }
}