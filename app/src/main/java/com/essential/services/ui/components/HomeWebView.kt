package com.essential.services.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.ValueCallback
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.essential.services.utils.ApkInstaller

class LocalStorageBridge(
    private val context: Context,
    private val onDataChanged: (String) -> Unit,
) {
    @JavascriptInterface
    fun onLocalStorageChanged(json: String) {
        onDataChanged(json)
    }

    @JavascriptInterface
    fun installApk(urlOrPath: String) {
        if (urlOrPath.startsWith("http://") || urlOrPath.startsWith("https://")) {
            ApkInstaller.downloadAndInstallApk(context, urlOrPath)
        } else {
            val file = java.io.File(urlOrPath)
            ApkInstaller.installApkFile(context, file)
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun HomeWebView(
    url: String,
    modifier: Modifier = Modifier,
    onDataUpdated: ((String) -> Unit)? = null,
    onWebViewReady: ((WebView) -> Unit)? = null,
) {
    val webViewRememberState = remember {
        object {
            var savedState: Bundle? = null
        }
    }

    var filePathCallback by remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        try {
            if (uri != null) {
                filePathCallback?.onReceiveValue(arrayOf(uri))
            } else {
                filePathCallback?.onReceiveValue(null)
            }
        } catch (_: Exception) {}
        filePathCallback = null
    }

    AndroidView(
        factory = { context ->
            WebView(context).apply {
                try {
                    setBackgroundColor(Color.TRANSPARENT)

                    val bridge = LocalStorageBridge(context) { dataJson ->
                        onDataUpdated?.invoke(dataJson)
                    }
                    addJavascriptInterface(bridge, "AndroidBridge")

                    setDownloadListener { downloadUrl, _, _, mimetype, _ ->
                        if (downloadUrl.endsWith(".apk", ignoreCase = true) || mimetype == "application/vnd.android.package-archive") {
                            ApkInstaller.downloadAndInstallApk(context, downloadUrl)
                        }
                    }

                    webChromeClient = object : android.webkit.WebChromeClient() {
                        override fun onShowFileChooser(
                            webView: WebView?,
                            callback: ValueCallback<Array<Uri>>?,
                            fileChooserParams: FileChooserParams?
                        ): Boolean {
                            filePathCallback?.onReceiveValue(null)
                            filePathCallback = callback
                            try {
                                filePickerLauncher.launch("image/*")
                            } catch (_: Exception) {
                                filePathCallback?.onReceiveValue(null)
                                filePathCallback = null
                                return false
                            }
                            return true
                        }
                    }

                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                            val requestUrl = request?.url?.toString() ?: ""
                            if (requestUrl.endsWith(".apk", ignoreCase = true)) {
                                ApkInstaller.downloadAndInstallApk(context, requestUrl)
                                return true
                            }
                            return super.shouldOverrideUrlLoading(view, request)
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            val jsInjector = """
                                (function() {
                                    if (window.AndroidBridge && !window.__androidLocalStoragePatched) {
                                        window.__androidLocalStoragePatched = true;
                                        
                                        function notifyNative() {
                                            try {
                                                window.AndroidBridge.onLocalStorageChanged(JSON.stringify(localStorage));
                                            } catch(e) {}
                                        }

                                        var origSet = localStorage.setItem;
                                        localStorage.setItem = function() {
                                            origSet.apply(this, arguments);
                                            notifyNative();
                                        };

                                        var origRemove = localStorage.removeItem;
                                        localStorage.removeItem = function() {
                                            origRemove.apply(this, arguments);
                                            notifyNative();
                                        };

                                        var origClear = localStorage.clear;
                                        localStorage.clear = function() {
                                            origClear.apply(this, arguments);
                                            notifyNative();
                                        };

                                        notifyNative();
                                    }
                                })();
                            """.trimIndent()
                            view?.evaluateJavascript(jsInjector, null)
                        }
                    }

                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        useWideViewPort = true
                        loadWithOverviewMode = true
                        mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            isAlgorithmicDarkeningAllowed = false
                        }
                    }

                    onWebViewReady?.invoke(this)

                    val state = webViewRememberState.savedState
                    if (state != null) {
                        restoreState(state)
                    } else {
                        loadUrl(url)
                    }
                } catch (_: Exception) {}
            }
        },
        update = { webView ->
            try {
                onWebViewReady?.invoke(webView)
                if (webView.url.isNullOrEmpty()) {
                    webView.loadUrl(url)
                }
            } catch (_: Exception) {}
        },
        onRelease = { webView ->
            try {
                val bundle = Bundle()
                webView.saveState(bundle)
                webViewRememberState.savedState = bundle
                webView.destroy()
            } catch (_: Exception) {}
        },
        modifier = modifier,
    )
}
