package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.RealityEngineViewModel
import com.example.ui.components.PrecisionCard
import com.example.ui.components.PrecisionSectionHeader
import com.example.ui.theme.RealityEngineAmber
import com.example.ui.theme.RealityEngineBorder
import com.example.ui.theme.RealityEngineCyan
import com.example.ui.theme.RealityEngineDarkBg
import com.example.ui.theme.RealityEngineEmerald
import com.example.ui.theme.RealityEngineSurface
import com.example.ui.theme.RealityEngineSurfaceElevated
import com.example.ui.theme.RealityEngineTextDisabled
import com.example.ui.theme.RealityEngineTextMuted
import com.example.ui.theme.RealityEngineTextPrimary
import com.example.ui.theme.RealityEngineTextSecondary

@Composable
fun SettingsScreen(
    viewModel: RealityEngineViewModel,
    onRequestDefaultPhone: () -> Unit = {},
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cryptoManager = viewModel.cryptoManager
    val apiTestState by viewModel.apiTestState.collectAsState()
    val isDefaultPhoneApp by viewModel.isDefaultPhoneApp.collectAsState()

    var selectedTab by remember { mutableIntStateOf(4) } // Default to API CONFIGURATION (Tab 4)
    val tabs = listOf("GENERAL", "CALLS", "AI", "PRIVACY", "API CONFIG")

    // State bindings for API inputs
    var twilioSid by remember { mutableStateOf(cryptoManager.getTwilioSid()) }
    var twilioToken by remember { mutableStateOf(cryptoManager.getTwilioToken()) }
    var twilioPhone by remember { mutableStateOf(cryptoManager.getTwilioPhoneNumber()) }

    var deepgramKey by remember { mutableStateOf(cryptoManager.getDeepgramKey()) }
    var groqKey by remember { mutableStateOf(cryptoManager.getGroqKey()) }
    var groqModel by remember { mutableStateOf(cryptoManager.getGroqModel()) }

    var supabaseUrl by remember { mutableStateOf(cryptoManager.getSupabaseUrl()) }
    var supabaseAnonKey by remember { mutableStateOf(cryptoManager.getSupabaseAnonKey()) }

    // Toggles
    var aiEnabled by remember { mutableStateOf(cryptoManager.isAiAnalysisEnabled) }
    var deceptionEnabled by remember { mutableStateOf(cryptoManager.isDeceptionSignalEnabled) }
    var liveTranscriptionEnabled by remember { mutableStateOf(cryptoManager.isLiveTranscriptionEnabled) }
    var memoryEnabled by remember { mutableStateOf(cryptoManager.isMemorySystemEnabled) }
    var autoSummaryEnabled by remember { mutableStateOf(cryptoManager.isAutoPostCallSummaryEnabled) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(RealityEngineDarkBg)
    ) {
        // Settings Header with Back Navigation
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(RealityEngineSurface)
                .border(1.dp, RealityEngineBorder, RoundedCornerShape(0.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(RealityEngineSurfaceElevated)
                    .testTag("settings_back_button")
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = RealityEngineAmber,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "ENGINE CONFIGURATION",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp,
                        color = RealityEngineTextPrimary
                    )
                )
                Text(
                    text = "HARDWARE KEYSTORE ENCRYPTED",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        color = RealityEngineCyan
                    )
                )
            }
        }

        // Section Tabs
        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            containerColor = RealityEngineSurface,
            contentColor = RealityEngineAmber,
            edgePadding = 16.dp,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = RealityEngineAmber,
                    height = 2.dp
                )
            },
            divider = { Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(RealityEngineBorder)) }
        ) {
            tabs.forEachIndexed { index, tabName ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            text = tabName,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == index) RealityEngineAmber else RealityEngineTextMuted
                        )
                    }
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            when (selectedTab) {
                0 -> { // GENERAL
                    item {
                        PrecisionCard {
                            Column {
                                PrecisionSectionHeader(
                                    title = "DEFAULT PHONE ROLE",
                                    tag = if (isDefaultPhoneApp) "ACTIVE" else "NOT SET",
                                    tagColor = if (isDefaultPhoneApp) RealityEngineEmerald else RealityEngineAmber
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                if (isDefaultPhoneApp) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = RealityEngineEmerald,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Reality Engine is the active default phone app.",
                                            style = MaterialTheme.typography.bodyMedium.copy(color = RealityEngineTextPrimary)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Inbound & outbound calls are routed directly through Reality Engine InCallService with live co-pilot enabled.",
                                        style = MaterialTheme.typography.bodySmall.copy(color = RealityEngineTextSecondary, fontSize = 11.sp)
                                    )
                                } else {
                                    Text(
                                        text = "Reality Engine is not currently set as the default phone application.",
                                        style = MaterialTheme.typography.bodyMedium.copy(color = RealityEngineTextPrimary)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "To handle all native incoming calls with live transcription & deception alerts, set Reality Engine as the default dialer.",
                                        style = MaterialTheme.typography.bodySmall.copy(color = RealityEngineTextSecondary, fontSize = 11.sp)
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Button(
                                        onClick = onRequestDefaultPhone,
                                        modifier = Modifier.fillMaxWidth().height(38.dp).testTag("settings_set_default_phone_button"),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = RealityEngineAmber,
                                            contentColor = RealityEngineDarkBg
                                        ),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(16.dp), tint = RealityEngineDarkBg)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("SET AS DEFAULT PHONE", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                    item {
                        PrecisionCard {
                            Column {
                                PrecisionSectionHeader(title = "DEVICE IDENTIFIER", tag = "S25 ULTRA", tagColor = RealityEngineCyan)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Target Hardware: Samsung Galaxy S25 Ultra (SM-S938B)",
                                    style = MaterialTheme.typography.bodyMedium.copy(color = RealityEngineTextPrimary)
                                )
                                Text(
                                    text = "Audio Pipeline: Low-Latency High-Pass Stream",
                                    style = MaterialTheme.typography.bodySmall.copy(color = RealityEngineTextSecondary, fontSize = 11.sp)
                                )
                            }
                        }
                    }
                }
                1 -> { // CALLS
                    item {
                        PrecisionCard {
                            Column {
                                PrecisionSectionHeader(title = "TELEPHONY ENGINE", tag = "TWILIO VOICE")
                                Spacer(modifier = Modifier.height(6.dp))
                                SettingToggleRow(
                                    title = "Live Audio Streaming",
                                    subtitle = "Stream bi-directional audio to Deepgram WebSocket",
                                    checked = liveTranscriptionEnabled,
                                    onCheckedChange = {
                                        liveTranscriptionEnabled = it
                                        cryptoManager.isLiveTranscriptionEnabled = it
                                    }
                                )
                                SettingToggleRow(
                                    title = "Automatic Call Summaries",
                                    subtitle = "Extract commitments, claims, and facts post-call",
                                    checked = autoSummaryEnabled,
                                    onCheckedChange = {
                                        autoSummaryEnabled = it
                                        cryptoManager.isAutoPostCallSummaryEnabled = it
                                    }
                                )
                            }
                        }
                    }
                }
                2 -> { // AI
                    item {
                        PrecisionCard {
                            Column {
                                PrecisionSectionHeader(title = "INTELLIGENCE ENGINE", tag = "GROQ LLAMA", tagColor = RealityEngineAmber)
                                Spacer(modifier = Modifier.height(6.dp))
                                SettingToggleRow(
                                    title = "Real-time AI Co-Pilot",
                                    subtitle = "Tactical strategy, cognitive probe, and tone recommendation",
                                    checked = aiEnabled,
                                    onCheckedChange = {
                                        aiEnabled = it
                                        cryptoManager.isAiAnalysisEnabled = it
                                    }
                                )
                                SettingToggleRow(
                                    title = "Deception & Contradiction Signal",
                                    subtitle = "Linguistic distancing, factual inconsistency, and acoustic stress",
                                    checked = deceptionEnabled,
                                    onCheckedChange = {
                                        deceptionEnabled = it
                                        cryptoManager.isDeceptionSignalEnabled = it
                                    }
                                )
                                SettingToggleRow(
                                    title = "Structured Memory Engine",
                                    subtitle = "Auto-detect facts (Observed, Inferred, Confirmed)",
                                    checked = memoryEnabled,
                                    onCheckedChange = {
                                        memoryEnabled = it
                                        cryptoManager.isMemorySystemEnabled = it
                                    }
                                )
                            }
                        }
                    }
                }
                3 -> { // PRIVACY
                    item {
                        PrecisionCard {
                            Column {
                                PrecisionSectionHeader(title = "SECURITY ARCHITECTURE", tag = "AES-GCM", tagColor = RealityEngineEmerald)
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.Lock, contentDescription = "Lock", tint = RealityEngineEmerald, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Keystore Key Alias: reality_engine_master_key",
                                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, color = RealityEngineEmerald)
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "All API tokens are encrypted in hardware-backed Android KeyStore before touching disk. Keys are never logged or exported.",
                                    style = MaterialTheme.typography.bodySmall.copy(color = RealityEngineTextSecondary, fontSize = 11.sp)
                                )
                            }
                        }
                    }
                }
                4 -> { // API CONFIGURATION (Full Key Vault)
                    // TWILIO
                    item {
                        PrecisionCard {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                PrecisionSectionHeader(title = "TWILIO VOICE & SIP", tag = "TELEPHONY", tagColor = RealityEngineAmber)
                                Spacer(modifier = Modifier.height(8.dp))
                                EncryptedInputField(
                                    label = "Twilio Account SID (AC...)",
                                    value = twilioSid,
                                    onValueChange = {
                                        twilioSid = it
                                        cryptoManager.saveTwilioSid(it)
                                    }
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                EncryptedInputField(
                                    label = "Twilio Auth Token",
                                    value = twilioToken,
                                    isSecret = true,
                                    onValueChange = {
                                        twilioToken = it
                                        cryptoManager.saveTwilioToken(it)
                                    }
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                EncryptedInputField(
                                    label = "Twilio Phone Number (+1...)",
                                    value = twilioPhone,
                                    onValueChange = {
                                        twilioPhone = it
                                        cryptoManager.saveTwilioPhoneNumber(it)
                                    }
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                ApiTestButton(
                                    isLoading = apiTestState.isTestingTwilio,
                                    statusText = apiTestState.twilioStatus,
                                    onClick = { viewModel.testTwilioApi(twilioSid, twilioToken) }
                                )
                            }
                        }
                    }

                    // DEEPGRAM
                    item {
                        PrecisionCard {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                PrecisionSectionHeader(title = "DEEPGRAM NOVA-2", tag = "TRANSCRIPTION", tagColor = RealityEngineCyan)
                                Spacer(modifier = Modifier.height(8.dp))
                                EncryptedInputField(
                                    label = "Deepgram API Key",
                                    value = deepgramKey,
                                    isSecret = true,
                                    onValueChange = {
                                        deepgramKey = it
                                        cryptoManager.saveDeepgramKey(it)
                                    }
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                ApiTestButton(
                                    isLoading = apiTestState.isTestingDeepgram,
                                    statusText = apiTestState.deepgramStatus,
                                    onClick = { viewModel.testDeepgramApi(deepgramKey) }
                                )
                            }
                        }
                    }

                    // GROQ
                    item {
                        PrecisionCard {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                PrecisionSectionHeader(title = "GROQ LLAMA-3.1", tag = "AI ENGINE", tagColor = RealityEngineAmber)
                                Spacer(modifier = Modifier.height(8.dp))
                                EncryptedInputField(
                                    label = "Groq API Key (gsk_...)",
                                    value = groqKey,
                                    isSecret = true,
                                    onValueChange = {
                                        groqKey = it
                                        cryptoManager.saveGroqKey(it)
                                    }
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                EncryptedInputField(
                                    label = "Model (e.g. llama-3.1-8b-instant)",
                                    value = groqModel,
                                    onValueChange = {
                                        groqModel = it
                                        cryptoManager.saveGroqModel(it)
                                    }
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                ApiTestButton(
                                    isLoading = apiTestState.isTestingGroq,
                                    statusText = apiTestState.groqStatus,
                                    onClick = { viewModel.testGroqApi(groqKey) }
                                )
                            }
                        }
                    }

                    // SUPABASE
                    item {
                        PrecisionCard {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                PrecisionSectionHeader(title = "SUPABASE POSTGRESQL", tag = "PERSISTENCE", tagColor = RealityEngineEmerald)
                                Spacer(modifier = Modifier.height(8.dp))
                                EncryptedInputField(
                                    label = "Supabase Project URL (https://...supabase.co)",
                                    value = supabaseUrl,
                                    onValueChange = {
                                        supabaseUrl = it
                                        cryptoManager.saveSupabaseUrl(it)
                                    }
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                EncryptedInputField(
                                    label = "Supabase Anon Public Key",
                                    value = supabaseAnonKey,
                                    isSecret = true,
                                    onValueChange = {
                                        supabaseAnonKey = it
                                        cryptoManager.saveSupabaseAnonKey(it)
                                    }
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                ApiTestButton(
                                    isLoading = apiTestState.isTestingSupabase,
                                    statusText = apiTestState.supabaseStatus,
                                    onClick = { viewModel.testSupabaseApi(supabaseUrl, supabaseAnonKey) }
                                )
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

@Composable
private fun EncryptedInputField(
    label: String,
    value: String,
    isSecret: Boolean = false,
    onValueChange: (String) -> Unit
) {
    var isPasswordVisible by remember { mutableStateOf(!isSecret) }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontSize = 11.sp, fontFamily = FontFamily.Monospace) },
        modifier = Modifier.fillMaxWidth(),
        textStyle = MaterialTheme.typography.bodyMedium.copy(
            fontFamily = FontFamily.Monospace,
            color = RealityEngineTextPrimary,
            fontSize = 12.sp
        ),
        visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            if (isSecret) {
                IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                    Icon(
                        imageVector = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = "Toggle Secret",
                        tint = RealityEngineTextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = RealityEngineAmber,
            unfocusedBorderColor = RealityEngineBorder,
            focusedContainerColor = RealityEngineSurface,
            unfocusedContainerColor = RealityEngineSurface
        ),
        singleLine = true
    )
}

@Composable
private fun SettingToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = RealityEngineTextPrimary
                )
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = RealityEngineTextSecondary,
                    fontSize = 11.sp
                )
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = RealityEngineDarkBg,
                checkedTrackColor = RealityEngineAmber,
                uncheckedThumbColor = RealityEngineTextMuted,
                uncheckedTrackColor = RealityEngineSurfaceElevated
            )
        )
    }
}

@Composable
private fun ApiTestButton(
    isLoading: Boolean,
    statusText: String?,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedButton(
            onClick = onClick,
            enabled = !isLoading,
            shape = RoundedCornerShape(6.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = RealityEngineAmber)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    strokeWidth = 2.dp,
                    color = RealityEngineAmber
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("TESTING...", fontFamily = FontFamily.Monospace, fontSize = 10.sp)
            } else {
                Icon(imageVector = Icons.Default.Refresh, contentDescription = "Test", modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("TEST CONNECTION", fontFamily = FontFamily.Monospace, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }

        if (statusText != null) {
            val isSuccess = statusText.startsWith("✓")
            Text(
                text = statusText,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSuccess) RealityEngineEmerald else RealityEngineAmber
                ),
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}
