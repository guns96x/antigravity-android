package com.antigravity.mobile

import android.app.Activity
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {

    private var onAccountSelectedCallback: ((String) -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val prefs = PreferencesManager(this)

        setContent {
            GoogleAntigravityHub(
                prefs = prefs,
                onSetKeepScreenOn = { keepOn ->
                    if (keepOn) {
                        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    } else {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    }
                },
                onPickAccountFromDevice = { onSelected ->
                    onAccountSelectedCallback = onSelected
                    try {
                        val pickIntent = AccountPickerHelper.createChooseAccountIntent(prefs.preferredEmail)
                        accountPickerLauncher.launch(pickIntent)
                    } catch (e: Exception) {
                        Toast.makeText(this, "Помилка виклику вибору акаунта: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                },
                onLaunchSession = {
                    CustomTabLauncher.launchAntigravity(
                        context = this,
                        baseUrl = prefs.canonicalUrl,
                        email = prefs.preferredEmail,
                        authUserIndex = prefs.authUserIndex
                    )
                }
            )
        }
    }

    private val accountPickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val email = result.data?.getStringExtra(android.accounts.AccountManager.KEY_ACCOUNT_NAME)
            if (!email.isNullOrBlank()) {
                onAccountSelectedCallback?.invoke(email)
                Toast.makeText(this, "Акаунт підхоплено: $email", Toast.LENGTH_SHORT).show()
            }
        }
        onAccountSelectedCallback = null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoogleAntigravityHub(
    prefs: PreferencesManager,
    onSetKeepScreenOn: (Boolean) -> Unit,
    onPickAccountFromDevice: (((String) -> Unit) -> Unit),
    onLaunchSession: () -> Unit
) {
    val context = LocalContext.current
    var preferredEmail by remember { mutableStateOf(prefs.preferredEmail) }
    var authUserIndex by remember { mutableIntStateOf(prefs.authUserIndex) }
    var autoLaunch by remember { mutableStateOf(prefs.autoLaunchOnOpen) }
    var keepScreenAwake by remember { mutableStateOf(prefs.keepScreenAwake) }
    var showAccountDialog by remember { mutableStateOf(false) }

    LaunchedEffect(keepScreenAwake) {
        onSetKeepScreenOn(keepScreenAwake)
    }

    // Auto launch if enabled
    LaunchedEffect(Unit) {
        if (autoLaunch) {
            onLaunchSession()
        }
    }

    // First run: prompt system account picker if empty
    LaunchedEffect(Unit) {
        if (preferredEmail.isBlank() && !prefs.hasPromptedAccountPicker) {
            prefs.hasPromptedAccountPicker = true
            onPickAccountFromDevice { picked ->
                preferredEmail = picked
                prefs.preferredEmail = picked
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color(0xFF0F1117),
        contentWindowInsets = WindowInsets.statusBars
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Google App Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF1A73E8)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("▲", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        text = "Antigravity",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Google Avatar circle in top-right (standard Google app pattern)
                val initial = if (preferredEmail.isNotBlank()) {
                    preferredEmail.first().uppercase()
                } else "G"

                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF2563EB))
                        .border(1.5.dp, Color(0xFF60A5FA), CircleShape)
                        .clickable { showAccountDialog = true },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initial,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }

            // Hero Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF181B22)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF262C36))
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1A73E8).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🚀", fontSize = 28.sp)
                    }

                    Text(
                        text = "Google Antigravity",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "Повноцінна сесія через захищений Google Chrome із системним профілем та без циклу вибору акаунта.",
                        color = Color(0xFF9CA3AF),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )

                    // Account pill
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFF202530),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF323B4A)),
                        modifier = Modifier.clickable { showAccountDialog = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("👤", fontSize = 12.sp)
                            Text(
                                text = if (preferredEmail.isNotBlank()) preferredEmail else "authuser=$authUserIndex (перший акаунт)",
                                color = Color(0xFF93C5FD),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Primary launch button
                    Button(
                        onClick = onLaunchSession,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A73E8)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                    ) {
                        Text(
                            text = "Увійти в Antigravity",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Quick Preferences Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF181B22)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF262C36))
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Швидкий запуск", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Text("Відкривати сесію одразу при старті", color = Color(0xFF9CA3AF), fontSize = 12.sp)
                        }
                        Switch(
                            checked = autoLaunch,
                            onCheckedChange = {
                                autoLaunch = it
                                prefs.autoLaunchOnOpen = it
                            }
                        )
                    }

                    HorizontalDivider(color = Color(0xFF262C36))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Не вимикати екран", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Text("Тримати екран увімкненим для моніторингу", color = Color(0xFF9CA3AF), fontSize = 12.sp)
                        }
                        Switch(
                            checked = keepScreenAwake,
                            onCheckedChange = {
                                keepScreenAwake = it
                                prefs.keepScreenAwake = it
                            }
                        )
                    }
                }
            }

            // Footer info
            Text(
                text = "Працює на базі Google Chrome Custom Tabs з нативною сесією",
                color = Color(0xFF6B7280),
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp)
            )
        }
    }

    // Google Account Switcher Dialog (like in Google Apps)
    if (showAccountDialog) {
        var emailInput by remember { mutableStateOf(preferredEmail) }
        var selectedAuthIndex by remember { mutableIntStateOf(authUserIndex) }

        AlertDialog(
            onDismissRequest = { showAccountDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Google", color = Color(0xFF4285F4), fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Text("Обліковий запис", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Medium)
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Оберіть, під яким акаунтом заходити в Antigravity. Завдяки параметру authuser Google відкриває цей профіль без AccountChooser.",
                        fontSize = 12.sp,
                        color = Color(0xFF9CA3AF),
                        lineHeight = 16.sp
                    )

                    // Button to pick account directly from device system accounts
                    Button(
                        onClick = {
                            onPickAccountFromDevice { picked ->
                                emailInput = picked
                                preferredEmail = picked
                                prefs.preferredEmail = picked
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF3B82F6)),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("📱 Підхопити акаунт з телефона", color = Color(0xFF60A5FA), fontWeight = FontWeight.SemiBold)
                    }

                    OutlinedTextField(
                        value = emailInput,
                        onValueChange = { emailInput = it },
                        label = { Text("Email акаунта") },
                        placeholder = { Text("ваша.пошта@gmail.com") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Або номер профілю в браузері (authuser):", color = Color(0xFFD1D5DB), fontSize = 13.sp)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(0, 1, 2).forEach { index ->
                            FilterChip(
                                selected = selectedAuthIndex == index,
                                onClick = { selectedAuthIndex = index },
                                label = { Text("authuser=$index") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF1A73E8),
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    preferredEmail = emailInput.trim()
                    authUserIndex = selectedAuthIndex
                    prefs.preferredEmail = preferredEmail
                    prefs.authUserIndex = authUserIndex
                    showAccountDialog = false
                    Toast.makeText(context, "Налаштування акаунта збережено", Toast.LENGTH_SHORT).show()
                }) {
                    Text("Зберегти", color = Color(0xFF1A73E8), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAccountDialog = false }) {
                    Text("Закрити", color = Color(0xFF9CA3AF))
                }
            },
            containerColor = Color(0xFF181B22)
        )
    }
}
