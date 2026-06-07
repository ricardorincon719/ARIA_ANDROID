package com.ricardo.aria

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.core.app.ActivityCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.ricardo.aria.ui.theme.AriaTheme


class PearlWebActivity : ComponentActivity() {
    private var pearlWebView: WebView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestSceneNotificationPermission()
        PearlScenePromptScheduler.ensureScheduled(applicationContext)

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    val webView = pearlWebView
                    if (webView?.canGoBack() == true) {
                        webView.goBack()
                    } else {
                        finish()
                    }
                }
            }
        )

        setContent {
            AriaTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    PearlWebApp(
                        onWebViewReady = { pearlWebView = it }
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        pearlWebView?.destroy()
        pearlWebView = null
        super.onDestroy()
    }

    private fun requestSceneNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1007)
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun PearlWebApp(onWebViewReady: (WebView) -> Unit) {
    val context = LocalContext.current
    val coreUrls = remember { PearlCoreEndpoints.urls(context) }
    var activeCoreIndex by remember { mutableStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }
    var isUnavailable by remember { mutableStateOf(false) }
    var reloadTick by remember { mutableStateOf(0) }
    var appliedReloadTick by remember { mutableStateOf(0) }

    fun currentCoreUrl(): String = coreUrls.getOrElse(activeCoreIndex) { coreUrls.first() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF05070A))
            .safeDrawingPadding()
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { factoryContext ->
                WebView(factoryContext).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.cacheMode = WebSettings.LOAD_DEFAULT
                    settings.mediaPlaybackRequiresUserGesture = false
                    settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                    addJavascriptInterface(PearlNativeBridge(factoryContext), "PearlAndroid")
                    webViewClient = object : WebViewClient() {
                        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                            isLoading = true
                            isUnavailable = false
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            view?.evaluateJavascript("Boolean(window.PEARL_CONFIG)") { result ->
                                if (result == "true") {
                                    isLoading = false
                                    val origin = PearlCoreEndpoints.originFromUrl(url)
                                    PearlNativeSessionStore(factoryContext).saveLastCoreUrl(origin)
                                } else if (activeCoreIndex < coreUrls.lastIndex) {
                                    activeCoreIndex += 1
                                    isLoading = true
                                    isUnavailable = false
                                    view.loadUrl(currentCoreUrl())
                                } else {
                                    isLoading = false
                                    isUnavailable = true
                                }
                            }
                        }

                        override fun onReceivedError(
                            view: WebView?,
                            request: WebResourceRequest?,
                            error: WebResourceError?
                        ) {
                            if (request?.isForMainFrame != true) return
                            if (activeCoreIndex < coreUrls.lastIndex) {
                                activeCoreIndex += 1
                                isLoading = true
                                isUnavailable = false
                                view?.loadUrl(currentCoreUrl())
                                return
                            }
                            isLoading = false
                            isUnavailable = true
                        }
                    }
                    onWebViewReady(this)
                    loadUrl(currentCoreUrl())
                }
            },
            update = { webView ->
                val targetUrl = currentCoreUrl()
                if (reloadTick != appliedReloadTick) {
                    appliedReloadTick = reloadTick
                    webView.loadUrl(targetUrl)
                } else if (webView.url == null) {
                    webView.loadUrl(targetUrl)
                }
            }
        )

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xCC05070A)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color.White)
            }
        }

        if (isUnavailable) {
            PearlUnavailableScreen(
                onRetry = {
                    activeCoreIndex = 0
                    isUnavailable = false
                    isLoading = true
                    reloadTick += 1
                }
            )
        }
    }
}


@Composable
private fun PearlUnavailableScreen(onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF070A0F))
            .padding(28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "PEARL HOME",
            color = Color.White,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "No pude conectar con PEARL Core.",
            color = Color(0xFFB8C0CC),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(20.dp))
        Button(onClick = onRetry) {
            Text("Reintentar")
        }
    }
}
