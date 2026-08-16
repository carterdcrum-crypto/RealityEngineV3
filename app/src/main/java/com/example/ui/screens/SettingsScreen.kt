package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.VpnKey
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
import com.example.ui.theme.RealityEngineCrimson
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
    var twilioAccessToken by remember { mutableStateOf(cryptoManager.getTwilioAccessToken()) }
    var twilioMediaStreamUrl by remember { mutableStateOf(cryptoManager.getTwilioMediaStreamUrl()) }

    var openAiKey by remember { mutableStateOf(cryptoManager.getOpenAiKey()) }
    var deepgramKey by remember { mutableStateOf(cryptoManager.getDeepgramKey()) }
    var groqKey by remember { mutableStateOf(cryptoManager.getGroqKey()) }
    var groqModel by remember { mutableStateOf(cryptoManager.getGroqModel()) }

    var supabaseUrl by remember { mutableStateOf(cryptoManager.getSupabaseUrl()) }
    var supabaseAnonKey by remember { mutableStateOf(cryptoManager.getSupabaseAnonKey()) }

    var saveConfirmationMessage by remember { mutableStateOf<String?>(null) }

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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = RealityEngineCyan,
                        modifier = Modifier.size(11.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "ENCRYPTED SHARED PREFERENCES (AES-256 GCM)",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.sp,
                            color = RealityEngineCyan
                        )
                    )
                }
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

        // Save confirmation toast bar
        AnimatedVisibility(
            visible = saveConfirmationMessage != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            saveConfirmationMessage?.let { msg ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(RealityEngineEmerald.copy(alpha = 0.15f))
                        .border(1.dp, RealityEngineEmerald.copy(alpha = 0.4f), RoundedCornerShape(0.dp))
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = RealityEngineEmerald,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = msg,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                color = RealityEngineEmerald,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                    IconButton(
                        onClick = { saveConfirmationMessage = null },
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dismiss",
                            tint = RealityEngineEmerald,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
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
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(38.dp)
                                            .testTag("settings_set_default_phone_button"),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = RealityEngineAmber,
                                            contentColor = RealityEngineDarkBg
                                        ),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Phone,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = RealityEngineDarkBg
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            "SET AS DEFAULT PHONE",
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        )
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
                                PrecisionSectionHeader(title = "SECURITY ARCHITECTURE", tag = "AES-256 GCM", tagColor = RealityEngineEmerald)
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.Security, contentDescription = "Security", tint = RealityEngineEmerald, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Vault: EncryptedSharedPreferences (AndroidKeyStore MasterKey)",
                                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, color = RealityEngineEmerald)
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "All API tokens, Twilio credentials, and endpoints are encrypted at rest using AES-256 GCM before touching disk. Keys are never logged, exported, or transmitted off-device.",
                                    style = MaterialTheme.typography.bodySmall.copy(color = RealityEngineTextSecondary, fontSize = 11.sp)
                                )
                            }
                        }
                    }
                }
                4 -> { // API CONFIGURATION (Full Key Vault Form)
                    item {
                        // VAULT SUMMARY BANNER
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(RealityEngineSurfaceElevated)
                                .border(1.dp, RealityEngineCyan.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.VpnKey,
                                    contentDescription = null,
                                    tint = RealityEngineCyan,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "SECURE CREDENTIALS VAULT",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            color = RealityEngineCyan
                                        )
                                    )
                                    Text(
                                        text = "Encrypted in hardware with AES-256-GCM via MasterKey. Enter your API credentials below and test connectivity.",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = RealityEngineTextSecondary,
                                            fontSize = 10.sp
                                        )
                                    )
                                }
                            }
                        }
                    }

                    // 1. TWILIO TELEPHONY
                    item {
                        val isTwilioSet = twilioSid.isNotBlank() && twilioToken.isNotBlank()
                        PrecisionCard {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                PrecisionSectionHeader(
                                    title = "TWILIO VOICE & SIP",
                                    tag = if (isTwilioSet) "CONFIGURED" else "REQUIRED",
                                    tagColor = if (isTwilioSet) RealityEngineEmerald else RealityEngineAmber
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                EncryptedInputField(
                                    label = "Twilio Account SID (starts with AC...)",
                                    value = twilioSid,
                                    testTag = "settings_input_twilio_sid",
                                    onValueChange = {
                                        twilioSid = it
                                        cryptoManager.saveTwilioSid(it)
                                    }
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                EncryptedInputField(
                                    label = "Twilio Auth Token",
                                    value = twilioToken,
                                    isSecret = true,
                                    testTag = "settings_input_twilio_token",
                                    onValueChange = {
                                        twilioToken = it
                                        cryptoManager.saveTwilioToken(it)
                                    }
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                EncryptedInputField(
                                    label = "Twilio Phone Number (e.g. +14155552671)",
                                    value = twilioPhone,
                                    testTag = "settings_input_twilio_phone",
                                    onValueChange = {
                                        twilioPhone = it
                                        cryptoManager.saveTwilioPhoneNumber(it)
                                    }
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                EncryptedInputField(
                                    label = "Twilio Voice Access Token (JWT for WebRTC)",
                                    value = twilioAccessToken,
                                    isSecret = true,
                                    testTag = "settings_input_twilio_access_token",
                                    onValueChange = {
                                        twilioAccessToken = it
                                        cryptoManager.saveTwilioAccessToken(it)
                                    }
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                EncryptedInputField(
                                    label = "Twilio Media Stream URL (wss://...)",
                                    value = twilioMediaStreamUrl,
                                    testTag = "settings_input_twilio_media_stream_url",
                                    onValueChange = {
                                        twilioMediaStreamUrl = it
                                        cryptoManager.saveTwilioMediaStreamUrl(it)
                                    }
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                ApiTestButton(
                                    serviceName = "TWILIO",
                                    isLoading = apiTestState.isTestingTwilio,
                                    statusText = apiTestState.twilioStatus,
                                    testTag = "settings_test_twilio_button",
                                    onClick = { viewModel.testTwilioApi(twilioSid, twilioToken) }
                                )
                            }
                        }
                    }

                    // 2. DEEPGRAM SPEECH RECOGNITION
                    item {
                        val isDeepgramSet = deepgramKey.isNotBlank()
                        PrecisionCard {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                PrecisionSectionHeader(
                                    title = "DEEPGRAM NOVA-2",
                                    tag = if (isDeepgramSet) "CONFIGURED" else "REQUIRED",
                                    tagColor = if (isDeepgramSet) RealityEngineEmerald else RealityEngineCyan
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                EncryptedInputField(
                                    label = "Deepgram API Key",
                                    value = deepgramKey,
                                    isSecret = true,
                                    testTag = "settings_input_deepgram_key",
                                    onValueChange = {
                                        deepgramKey = it
                                        cryptoManager.saveDeepgramKey(it)
                                    }
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                ApiTestButton(
                                    serviceName = "DEEPGRAM",
                                    isLoading = apiTestState.isTestingDeepgram,
                                    statusText = apiTestState.deepgramStatus,
                                    testTag = "settings_test_deepgram_button",
                                    onClick = { viewModel.testDeepgramApi(deepgramKey) }
                                )
                            }
                        }
                    }

                    // 3. GROQ LLM INFERENCE
                    item {
                        val isGroqSet = groqKey.isNotBlank()
                        PrecisionCard {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                PrecisionSectionHeader(
                                    title = "GROQ LLAMA-3.1",
                                    tag = if (isGroqSet) "CONFIGURED" else "REQUIRED",
                                    tagColor = if (isGroqSet) RealityEngineEmerald else RealityEngineAmber
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                EncryptedInputField(
                                    label = "Groq API Key (starts with gsk_...)",
                                    value = groqKey,
                                    isSecret = true,
                                    testTag = "settings_input_groq_key",
                                    onValueChange = {
                                        groqKey = it
                                        cryptoManager.saveGroqKey(it)
                                    }
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                EncryptedInputField(
                                    label = "Model Name (e.g. llama-3.1-8b-instant)",
                                    value = groqModel,
                                    testTag = "settings_input_groq_model",
                                    onValueChange = {
                                        groqModel = it
                                        cryptoManager.saveGroqModel(it)
                                    }
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                ApiTestButton(
                                    serviceName = "GROQ",
                                    isLoading = apiTestState.isTestingGroq,
                                    statusText = apiTestState.groqStatus,
                                    testTag = "settings_test_groq_button",
                                    onClick = { viewModel.testGroqApi(groqKey) }
                                )
                            }
                        }
                    }

                    // 4. SUPABASE POSTGRESQL (OPTIONAL PERSISTENCE)
                    item {
                        val isSupabaseSet = supabaseUrl.isNotBlank() && supabaseAnonKey.isNotBlank()
                        PrecisionCard {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                PrecisionSectionHeader(
                                    title = "SUPABASE POSTGRESQL",
                                    tag = if (isSupabaseSet) "CONFIGURED" else "OPTIONAL",
                                    tagColor = if (isSupabaseSet) RealityEngineEmerald else RealityEngineTextMuted
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                EncryptedInputField(
                                    label = "Supabase Project URL (https://xyz.supabase.co)",
                                    value = supabaseUrl,
                                    testTag = "settings_input_supabase_url",
                                    onValueChange = {
                                        supabaseUrl = it
                                        cryptoManager.saveSupabaseUrl(it)
                                    }
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                EncryptedInputField(
                                    label = "Supabase Anon Public Key",
                                    value = supabaseAnonKey,
                                    isSecret = true,
                                    testTag = "settings_input_supabase_key",
                                    onValueChange = {
                                        supabaseAnonKey = it
                                        cryptoManager.saveSupabaseAnonKey(it)
                                    }
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                ApiTestButton(
                                    serviceName = "SUPABASE",
                                    isLoading = apiTestState.isTestingSupabase,
                                    statusText = apiTestState.supabaseStatus,
                                    testTag = "settings_test_supabase_button",
                                    onClick = { viewModel.testSupabaseApi(supabaseUrl, supabaseAnonKey) }
                                )
                            }
                        }
                    }

                    // 5. GLOBAL VAULT ACTIONS
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    cryptoManager.saveTwilioSid(twilioSid)
                                    cryptoManager.saveTwilioToken(twilioToken)
                                    cryptoManager.saveTwilioPhoneNumber(twilioPhone)
                                    cryptoManager.saveTwilioAccessToken(twilioAccessToken)
                                    cryptoManager.saveTwilioMediaStreamUrl(twilioMediaStreamUrl)
                                    cryptoManager.saveOpenAiKey(openAiKey)
                                    cryptoManager.saveDeepgramKey(deepgramKey)
                                    cryptoManager.saveGroqKey(groqKey)
                                    cryptoManager.saveGroqModel(groqModel)
                                    cryptoManager.saveSupabaseUrl(supabaseUrl)
                                    cryptoManager.saveSupabaseAnonKey(supabaseAnonKey)
                                    saveConfirmationMessage = "All credentials saved securely to EncryptedSharedPreferences."
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                                    .testTag("settings_save_all_credentials_button"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = RealityEngineEmerald,
                                    contentColor = RealityEngineDarkBg
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Save,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = RealityEngineDarkBg
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "SAVE ALL CREDENTIALS TO VAULT",
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }

                            OutlinedButton(
                                onClick = {
                                    cryptoManager.clearAllCredentials()
                                    twilioSid = ""
                                    twilioToken = ""
                                    twilioPhone = ""
                                    twilioAccessToken = ""
                                    twilioMediaStreamUrl = ""
                                    openAiKey = ""
                                    deepgramKey = ""
                                    groqKey = ""
                                    groqModel = "llama-3.1-8b-instant"
                                    supabaseUrl = ""
                                    supabaseAnonKey = ""
                                    saveConfirmationMessage = "Encrypted Vault cleared."
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(40.dp)
                                    .testTag("settings_clear_vault_button"),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = RealityEngineCrimson
                                ),
                                border = androidx.compose.foundation.BorderStroke(1.dp, RealityEngineCrimson.copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = RealityEngineCrimson
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "CLEAR ALL VAULT CREDENTIALS",
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
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
    testTag: String = "",
    onValueChange: (String) -> Unit
) {
    var isPasswordVisible by remember { mutableStateOf(!isSecret) }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontSize = 11.sp, fontFamily = FontFamily.Monospace) },
        modifier = Modifier
            .fillMaxWidth()
            .then(if (testTag.isNotEmpty()) Modifier.testTag(testTag) else Modifier),
        textStyle = MaterialTheme.typography.bodyMedium.copy(
            fontFamily = FontFamily.Monospace,
            color = RealityEngineTextPrimary,
            fontSize = 12.sp
        ),
        visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            if (isSecret) {
                IconButton(
                    onClick = { isPasswordVisible = !isPasswordVisible },
                    modifier = Modifier.testTag("${testTag}_visibility_toggle")
                ) {
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
    serviceName: String,
    isLoading: Boolean,
    statusText: String?,
    testTag: String = "",
    onClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = onClick,
                enabled = !isLoading,
                shape = RoundedCornerShape(6.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = RealityEngineAmber),
                modifier = if (testTag.isNotEmpty()) Modifier.testTag(testTag) else Modifier
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        strokeWidth = 2.dp,
                        color = RealityEngineAmber
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("CONNECTING...", fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                } else {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = "Test", modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("TEST $serviceName CONNECTION", fontFamily = FontFamily.Monospace, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (statusText != null) {
            Spacer(modifier = Modifier.height(6.dp))
            val isSuccess = statusText.startsWith("✓")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (isSuccess) RealityEngineEmerald.copy(alpha = 0.1f) else RealityEngineAmber.copy(alpha = 0.1f))
                    .border(
                        1.dp,
                        if (isSuccess) RealityEngineEmerald.copy(alpha = 0.3f) else RealityEngineAmber.copy(alpha = 0.3f),
                        RoundedCornerShape(4.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isSuccess) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                    contentDescription = null,
                    tint = if (isSuccess) RealityEngineEmerald else RealityEngineAmber,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSuccess) RealityEngineEmerald else RealityEngineAmber
                    )
                )
            }
        }
    }
}
