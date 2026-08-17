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
import com.example.ui.theme.RealityEngineDarkBg
import com.example.ui.theme.RealityEngineSurface
import com.example.ui.theme.RealityEngineSurfaceElevated
import com.example.ui.theme.RealityEngineTextMuted
import com.example.ui.theme.RealityEngineTextPrimary
import com.example.ui.theme.RealityEngineTheme

class MainActivity : ComponentActivity() {
    private var pendingTelNumber: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        StartupCrashReporter.install(this)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIntent(intent)

        val previousCrash = StartupCrashReporter.getLastReport(this)
        if (previousCrash != null) {
            setContent {
                RealityEngineTheme {
                    CrashDiagnosticsScreen(
                        report = previousCrash,
                        onDismiss = { StartupCrashReporter.clear(this); recreate() }
                    )
                }
            }
            return
        }

        setContent {
            RealityEngineTheme {
                val viewModel: RealityEngineViewModel = viewModel()
                val context = LocalContext.current
                val activity = context as? Activity

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

                pendingTelNumber?.let { number ->
                    viewModel.clearDialer()
                    number.forEach { viewModel.appendDialDigit(it.toString()) }
                    pendingTelNumber = null
                }

                val lifecycleOwner = LocalLifecycleOwner.current
                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) viewModel.refreshDefaultPhoneStatus()
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
                }

                RealityEngineApp(viewModel = viewModel, onRequestDefaultPhone = onRequestDefaultPhone)
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
        if (data != null && data.scheme == "tel") pendingTelNumber = data.schemeSpecificPart
    }
}

@Composable
private fun CrashDiagnosticsScreen(report: String, onDismiss: () -> Unit) {
    androidx.compose.foundation.layout.Column(
        modifier = Modifier.fillMaxSize().background(RealityEngineDarkBg).padding(20.dp)
    ) {
        Text("REALITY ENGINE", color = RealityEngineAmber, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Text("STARTUP FAILURE", color = RealityEngineTextPrimary, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(top = 8.dp))
        Text("The previous launch crashed. Send this report to the developer.", color = RealityEngineTextMuted, fontFamily = FontFamily.Monospace, fontSize = 12.sp, modifier = Modifier.padding(vertical = 12.dp))
        androidx.compose.foundation.text.selection.SelectionContainer {
            Text(report, color = RealityEngineTextPrimary, fontFamily = FontFamily.Monospace, fontSize = 10.sp, modifier = Modifier.fillMaxWidth().weight(1f))
        }
        androidx.compose.material3.Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
            Text("CLEAR REPORT & RETRY")
        }
    }
}

@Composable
fun RealityEngineApp(viewModel: RealityEngineViewModel, onRequestDefaultPhone: () -> Unit) {
    val callScreenState by viewModel.callScreenState.collectAsState()
    val activeCall by viewModel.activeCall.collectAsState()
    val postCallSummary by viewModel.postCallSummary.collectAsState()
    val isSettingsOpen by viewModel.isSettingsOpen.collectAsState()
    val currentTab by viewModel.currentTab.collectAsState()

    when (callScreenState) {
        CallScreenState.INCOMING -> { IncomingCallScreen(activeCall.caller, activeCall.phoneNumber, { viewModel.answerIncomingCall() }, { viewModel.declineIncomingCall() }); return }
        CallScreenState.DIALING -> { OutgoingCallScreen(activeCall.caller, activeCall.phoneNumber, activeCall.callState, activeCall.rawTwilioStatus, { viewModel.declineIncomingCall() }); return }
        CallScreenState.ACTIVE -> { ActiveCallScreen(viewModel); return }
        CallScreenState.SUMMARY -> {
            if (postCallSummary != null) { PostCallSummaryScreen(postCallSummary!!, { viewModel.saveCallSummaryAndFinish(it) }, { viewModel.discardSummaryAndFinish() }); return }
        }
        CallScreenState.IDLE -> Unit
    }

    if (isSettingsOpen) {
        SettingsScreen(viewModel, onRequestDefaultPhone, { viewModel.openSettings(false) })
        return
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(), containerColor = RealityEngineDarkBg,
        bottomBar = { RealityEngineBottomBar(currentTab) { viewModel.setTab(it) } }
    ) { innerPadding ->
        Box(Modifier.fillMaxSize().padding(innerPadding)) {
            when (currentTab) {
                AppNavTab.CALL -> DialerScreen(viewModel, onRequestDefaultPhone)
                AppNavTab.PEOPLE -> PeopleScreen(viewModel)
                AppNavTab.MEMORY -> MemoryScreen(viewModel)
                AppNavTab.SIGNALS -> SignalsScreen(viewModel)
            }
        }
    }
}

@Composable
fun RealityEngineBottomBar(currentTab: AppNavTab, onTabSelected: (AppNavTab) -> Unit) {
    NavigationBar(
        modifier = Modifier.fillMaxWidth().border(1.dp, RealityEngineBorder, RoundedCornerShape(0.dp)),
        containerColor = RealityEngineSurface, contentColor = RealityEngineTextPrimary, tonalElevation = 0.dp
    ) {
        NavigationItem(AppNavTab.CALL, "PHONE", Icons.Default.Dialpad, currentTab == AppNavTab.CALL) { onTabSelected(AppNavTab.CALL) }
        NavigationItem(AppNavTab.PEOPLE, "PEOPLE", Icons.Default.People, currentTab == AppNavTab.PEOPLE) { onTabSelected(AppNavTab.PEOPLE) }
        NavigationItem(AppNavTab.MEMORY, "MEMORY", Icons.Default.Memory, currentTab == AppNavTab.MEMORY) { onTabSelected(AppNavTab.MEMORY) }
        NavigationItem(AppNavTab.SIGNALS, "SIGNALS", Icons.Default.GraphicEq, currentTab == AppNavTab.SIGNALS) { onTabSelected(AppNavTab.SIGNALS) }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.NavigationItem(tab: AppNavTab, label: String, icon: ImageVector, isSelected: Boolean, onClick: () -> Unit) {
    NavigationBarItem(
        selected = isSelected, onClick = onClick,
        icon = { Icon(icon, label, Modifier.size(20.dp), tint = if (isSelected) RealityEngineAmber else RealityEngineTextMuted) },
        label = { Text(label, fontFamily = FontFamily.Monospace, fontSize = 10.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, color = if (isSelected) RealityEngineAmber else RealityEngineTextMuted) },
        colors = NavigationBarItemDefaults.colors(selectedIconColor = RealityEngineAmber, selectedTextColor = RealityEngineAmber, unselectedIconColor = RealityEngineTextMuted, unselectedTextColor = RealityEngineTextMuted, indicatorColor = RealityEngineSurfaceElevated),
        modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
    )
}
