package com.antigravity.mobile

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.CookieManager
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView

class MainActivity : ComponentActivity() {

    private var filePathCallback: ValueCallback<Array<Uri>>? = null

    companion object {
        const val MOBILE_USER_AGENT =
            "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36"
        const val DESKTOP_USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val prefs = PreferencesManager(this)

        setContent {
            AntigravityScreen(
                prefs = prefs,
                onSetKeepScreenOn = { keepOn ->
                    if (keepOn) {
                        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    } else {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    }
                },
                onOpenFileChooser = { callback, params ->
                    filePathCallback?.onReceiveValue(null)
                    filePathCallback = callback
                    val intent = params?.createIntent() ?: Intent(Intent.ACTION_GET_CONTENT).apply {
                        type = "*/*"
                        addCategory(Intent.CATEGORY_OPENABLE)
                    }
                    filePickerLauncher.launch(intent)
                    true
                }
            )
        }
    }

    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val intent = result.data
            val results = WebChromeClient.FileChooserParams.parseResult(result.resultCode, intent)
            filePathCallback?.onReceiveValue(results)
        } else {
            filePathCallback?.onReceiveValue(null)
        }
        filePathCallback = null
    }
}

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AntigravityScreen(
    prefs: PreferencesManager,
    onSetKeepScreenOn: (Boolean) -> Unit,
    onOpenFileChooser: (ValueCallback<Array<Uri>>?, WebChromeClient.FileChooserParams?) -> Boolean
) {
    val context = LocalContext.current
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var progress by remember { mutableIntStateOf(0) }
    var isDesktopMode by remember { mutableStateOf(prefs.isDesktopMode) }
    var keepScreenAwake by remember { mutableStateOf(prefs.keepScreenAwake) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var isControlsExpanded by remember { mutableStateOf(false) }
    var currentTitle by remember { mutableStateOf("Antigravity") }
    var backPressedOnce by remember { mutableStateOf(false) }

    LaunchedEffect(keepScreenAwake) {
        onSetKeepScreenOn(keepScreenAwake)
    }

    // Double back to exit handler
    BackHandler {
        val wv = webViewRef
        if (wv != null && wv.canGoBack()) {
            wv.goBack()
        } else {
            if (backPressedOnce) {
                (context as? Activity)?.finish()
            } else {
                backPressedOnce = true
                Toast.makeText(context, "Натисніть назад ще раз для виходу", Toast.LENGTH_SHORT).show()
            }
        }
    }

    LaunchedEffect(backPressedOnce) {
        if (backPressedOnce) {
            kotlinx.coroutines.delay(2000)
            backPressedOnce = false
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color(0xFF0F1117),
        contentWindowInsets = WindowInsets.statusBars
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Reusable WebView host
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    WebView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )

                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            databaseEnabled = true
                            useWideViewPort = true
                            loadWithOverviewMode = true
                            builtInZoomControls = true
                            displayZoomControls = false
                            mediaPlaybackRequiresUserGesture = false
                            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                            userAgentString = if (isDesktopMode) {
                                MainActivity.DESKTOP_USER_AGENT
                            } else {
                                MainActivity.MOBILE_USER_AGENT
                            }
                        }

                        val cookieManager = CookieManager.getInstance()
                        cookieManager.setAcceptCookie(true)
                        cookieManager.setAcceptThirdPartyCookies(this, true)

                        addJavascriptInterface(
                            AccountChooserBridge { detectedEmail ->
                                if (prefs.preferredEmail.isBlank()) {
                                    prefs.preferredEmail = detectedEmail
                                }
                            },
                            "AntigravityNative"
                        )

                        webViewClient = AntigravityWebViewClient(
                            context = ctx,
                            prefs = prefs,
                            onPageStartedCallback = {},
                            onPageFinishedCallback = { _ -> },
                            onErrorCallback = { _ -> },
                            onRendererCrashCallback = {
                                reload()
                            }
                        )

                        webChromeClient = AntigravityWebChromeClient(
                            onProgressChange = { p -> progress = p },
                            onTitleChange = { t -> currentTitle = t },
                            onFileChooser = onOpenFileChooser
                        )

                        loadUrl(prefs.canonicalUrl)
                        webViewRef = this
                    }
                },
                update = { wv ->
                    val expectedAgent = if (isDesktopMode) {
                        MainActivity.DESKTOP_USER_AGENT
                    } else {
                        MainActivity.MOBILE_USER_AGENT
                    }
                    if (wv.settings.userAgentString != expectedAgent) {
                        wv.settings.userAgentString = expectedAgent
                        wv.reload()
                    }
                }
            )

            // Top Progress Indicator
            if (progress in 1..99) {
                LinearProgressIndicator(
                    progress = { progress / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .align(Alignment.TopCenter),
                    color = Color(0xFF1A73E8),
                    trackColor = Color.Transparent
                )
            }

            // Quick Floating Control Pill (Neobank / Fintech utility style)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(end = 16.dp, bottom = 16.dp)
            ) {
                if (isControlsExpanded) {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1F2430)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2E3440)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Home button
                            IconButton(onClick = {
                                webViewRef?.loadUrl(prefs.canonicalUrl)
                                isControlsExpanded = false
                            }) {
                                Text("🏠", fontSize = 16.sp)
                            }

                            // Refresh button
                            IconButton(onClick = {
                                webViewRef?.reload()
                                isControlsExpanded = false
                            }) {
                                Text("🔄", fontSize = 16.sp)
                            }

                            // Desktop toggle button
                            IconButton(onClick = {
                                isDesktopMode = !isDesktopMode
                                prefs.isDesktopMode = isDesktopMode
                                isControlsExpanded = false
                            }) {
                                Text(if (isDesktopMode) "📱" else "💻", fontSize = 16.sp)
                            }

                            // Screen awake button
                            IconButton(onClick = {
                                keepScreenAwake = !keepScreenAwake
                                prefs.keepScreenAwake = keepScreenAwake
                            }) {
                                Text(if (keepScreenAwake) "☀️" else "🌙", fontSize = 16.sp)
                            }

                            // Settings
                            IconButton(onClick = {
                                showSettingsDialog = true
                                isControlsExpanded = false
                            }) {
                                Text("⚙️", fontSize = 16.sp)
                            }

                            // Collapse button
                            IconButton(onClick = { isControlsExpanded = false }) {
                                Text("✕", fontSize = 14.sp, color = Color(0xFF9CA3AF))
                            }
                        }
                    }
                } else {
                    // Floating minimal pill button
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1F2430).copy(alpha = 0.85f))
                            .border(1.dp, Color(0xFF374151), CircleShape)
                            .clickable { isControlsExpanded = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "⚡",
                            fontSize = 18.sp
                        )
                    }
                }
            }
        }
    }

    // Settings Modal Sheet
    if (showSettingsDialog) {
        var emailInput by remember { mutableStateOf(prefs.preferredEmail) }
        var urlInput by remember { mutableStateOf(prefs.canonicalUrl) }
        var autoSkip by remember { mutableStateOf(prefs.autoSkipAccountChooser) }

        AlertDialog(
            onDismissRequest = { showSettingsDialog = false },
            title = {
                Text(
                    text = "Налаштування Antigravity",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color.White
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Автоматичний вхід та сесія",
                        fontSize = 12.sp,
                        color = Color(0xFF9CA3AF)
                    )

                    OutlinedTextField(
                        value = emailInput,
                        onValueChange = { emailInput = it },
                        label = { Text("Google Account Email") },
                        placeholder = { Text("your.email@gmail.com") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF1A73E8),
                            focusedLabelColor = Color(0xFF1A73E8)
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Авто-пропуск AccountChooser", fontSize = 14.sp, color = Color.White)
                        Switch(
                            checked = autoSkip,
                            onCheckedChange = { autoSkip = it }
                        )
                    }

                    OutlinedTextField(
                        value = urlInput,
                        onValueChange = { urlInput = it },
                        label = { Text("Канонічна URL адреса") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            val cm = CookieManager.getInstance()
                            cm.removeAllCookies {
                                cm.flush()
                                webViewRef?.clearCache(true)
                                webViewRef?.clearHistory()
                                prefs.clearSession()
                                webViewRef?.loadUrl(prefs.canonicalUrl)
                                Toast.makeText(context, "Сесію та cookies очищено", Toast.LENGTH_SHORT).show()
                                showSettingsDialog = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Очистити сесію та Cookies", color = Color.White)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    prefs.preferredEmail = emailInput
                    prefs.canonicalUrl = urlInput.ifBlank { PreferencesManager.DEFAULT_URL }
                    prefs.autoSkipAccountChooser = autoSkip
                    showSettingsDialog = false
                    Toast.makeText(context, "Налаштування збережено", Toast.LENGTH_SHORT).show()
                }) {
                    Text("Зберегти", color = Color(0xFF1A73E8), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSettingsDialog = false }) {
                    Text("Скасувати", color = Color(0xFF9CA3AF))
                }
            },
            containerColor = Color(0xFF181B22)
        )
    }
}
