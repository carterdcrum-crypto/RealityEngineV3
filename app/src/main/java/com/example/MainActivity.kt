package com.example

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.telecom.TelecomRoleManager
import com.example.ui.AppNavTab
import com.example.ui.CallScreenState
import com.example.ui.RealityEngineViewModel
import com.example.ui.screens.ActiveCallScreen
import com.example.ui.screens.DialerScreen
import com.example.ui.screens.IncomingCallScreen
import com.example.ui.screens.MemoryScreen
import com.example.ui.screens.OutgoingCallScreen
import com.example.ui.screens.PeopleScreen
import com.example.ui.screens.PostCallSummaryScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SignalsScreen
import com.example.ui.theme.RealityEngineAmber
import com.example.ui.theme.RealityEngineBorder
import com.example.ui.theme.RealityEngineCyan
import com.example.ui.theme.RealityEngineDarkBg
import com.example.ui.theme.RealityEngineSurface
import com.example.ui.theme.RealityEngineSurfaceElevated
import com.example.ui.theme.RealityEngineTextMuted
import com.example.ui.theme.RealityEngineTextPrimary
import com.example.ui.theme.RealityEngineTheme

class MainActivity : ComponentActivity() {
    private var pendingTelNumber: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIntent(intent)
        setContent {
            RealityEngineTheme {
                val viewModel: RealityEngineViewModel = viewModel()
                val context = LocalContext.current
                val activity = context as? Activity

                // RoleManager result launcher for requesting default dialer role
                val defaultDialerLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.StartActivityForResult()
                ) {
                    viewModel.refreshDefaultPhoneStatus()
                }

                val onRequestDefaultPhone = {
                    if (activity != null) {
                        TelecomRoleManager.requestDefaultDialer(activity, defaultDialerLauncher)
                    }
                }

                // Handle intent dial requests
                pendingTelNumber?.let { number ->
                    viewModel.clearDialer()
                    number.forEach { viewModel.appendDialDigit(it.toString()) }
                    pendingTelNumber = null
                }

                // Refresh default phone status on lifecycle resume
                val lifecycleOwner = LocalLifecycleOwner.current
                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                            viewModel.refreshDefaultPhoneStatus()
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose {
                        lifecycleOwner.lifecycle.removeObserver(observer)
                    }
                }

                RealityEngineApp(
                    viewModel = viewModel,
                    onRequestDefaultPhone = onRequestDefaultPhone
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return
        val data: Uri? = intent.data
        if (data != null && data.scheme == "tel") {
            pendingTelNumber = data.schemeSpecificPart
        }
    }
}

@Composable
fun RealityEngineApp(
    viewModel: RealityEngineViewModel,
    onRequestDefaultPhone: () -> Unit
) {
    val callScreenState by viewModel.callScreenState.collectAsState()
    val activeCall by viewModel.activeCall.collectAsState()
    val postCallSummary by viewModel.postCallSummary.collectAsState()
    val isSettingsOpen by viewModel.isSettingsOpen.collectAsState()
    val currentTab by viewModel.currentTab.collectAsState()

    // 1. Full Screen Active Call Experiences
    when (callScreenState) {
        CallScreenState.INCOMING -> {
            IncomingCallScreen(
                caller = activeCall.caller,
                phoneNumber = activeCall.phoneNumber,
                onAnswer = { viewModel.answerIncomingCall() },
                onDecline = { viewModel.declineIncomingCall() }
            )
            return
        }
        CallScreenState.DIALING -> {
            OutgoingCallScreen(
                caller = activeCall.caller,
                phoneNumber = activeCall.phoneNumber,
                onEndCall = { viewModel.declineIncomingCall() }
            )
            return
        }
        CallScreenState.ACTIVE -> {
            ActiveCallScreen(viewModel = viewModel)
            return
        }
        CallScreenState.SUMMARY -> {
            if (postCallSummary != null) {
                PostCallSummaryScreen(
                    initialSummary = postCallSummary!!,
                    onSave = { updated -> viewModel.saveCallSummaryAndFinish(updated) },
                    onDiscard = { viewModel.discardSummaryAndFinish() }
                )
                return
            }
        }
        CallScreenState.IDLE -> { /* Proceed to main app scaffold */ }
    }

    // 2. Settings Screen Overlay
    if (isSettingsOpen) {
        SettingsScreen(
            viewModel = viewModel,
            onRequestDefaultPhone = onRequestDefaultPhone,
            onBack = { viewModel.openSettings(false) }
        )
        return
    }

    // 3. Main Phone Application Layout
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = RealityEngineDarkBg,
        bottomBar = {
            RealityEngineBottomBar(
                currentTab = currentTab,
                onTabSelected = { viewModel.setTab(it) }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                AppNavTab.CALL -> DialerScreen(
                    viewModel = viewModel,
                    onRequestDefaultPhone = onRequestDefaultPhone
                )
                AppNavTab.PEOPLE -> PeopleScreen(viewModel = viewModel)
                AppNavTab.MEMORY -> MemoryScreen(viewModel = viewModel)
                AppNavTab.SIGNALS -> SignalsScreen(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun RealityEngineBottomBar(
    currentTab: AppNavTab,
    onTabSelected: (AppNavTab) -> Unit
) {
    NavigationBar(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, RealityEngineBorder, RoundedCornerShape(0.dp)),
        containerColor = RealityEngineSurface,
        contentColor = RealityEngineTextPrimary,
        tonalElevation = 0.dp
    ) {
        NavigationItem(
            tab = AppNavTab.CALL,
            label = "PHONE",
            icon = Icons.Default.Dialpad,
            isSelected = currentTab == AppNavTab.CALL,
            onClick = { onTabSelected(AppNavTab.CALL) }
        )
        NavigationItem(
            tab = AppNavTab.PEOPLE,
            label = "PEOPLE",
            icon = Icons.Default.People,
            isSelected = currentTab == AppNavTab.PEOPLE,
            onClick = { onTabSelected(AppNavTab.PEOPLE) }
        )
        NavigationItem(
            tab = AppNavTab.MEMORY,
            label = "MEMORY",
            icon = Icons.Default.Memory,
            isSelected = currentTab == AppNavTab.MEMORY,
            onClick = { onTabSelected(AppNavTab.MEMORY) }
        )
        NavigationItem(
            tab = AppNavTab.SIGNALS,
            label = "SIGNALS",
            icon = Icons.Default.GraphicEq,
            isSelected = currentTab == AppNavTab.SIGNALS,
            onClick = { onTabSelected(AppNavTab.SIGNALS) }
        )
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.NavigationItem(
    tab: AppNavTab,
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    NavigationBarItem(
        selected = isSelected,
        onClick = onClick,
        icon = {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(20.dp),
                tint = if (isSelected) RealityEngineAmber else RealityEngineTextMuted
            )
        },
        label = {
            Text(
                text = label,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) RealityEngineAmber else RealityEngineTextMuted
            )
        },
        colors = NavigationBarItemDefaults.colors(
            selectedIconColor = RealityEngineAmber,
            selectedTextColor = RealityEngineAmber,
            unselectedIconColor = RealityEngineTextMuted,
            unselectedTextColor = RealityEngineTextMuted,
            indicatorColor = RealityEngineSurfaceElevated
        ),
        modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
    )
}
