package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallMissed
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhoneCallback
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CallRecordEntity
import com.example.data.model.PersonEntity
import com.example.ui.RealityEngineViewModel
import com.example.ui.components.PrecisionCard
import com.example.ui.components.PrecisionSectionHeader
import com.example.ui.components.RealityEngineDialerGrid
import com.example.ui.components.RealityEngineDialerKey
import com.example.ui.components.TacticalHeader
import com.example.ui.theme.RealityEngineAmber
import com.example.ui.theme.RealityEngineBorder
import com.example.ui.theme.RealityEngineBorderSubtle
import com.example.ui.theme.RealityEngineCallGreen
import com.example.ui.theme.RealityEngineCallRed
import com.example.ui.theme.RealityEngineCyan
import com.example.ui.theme.RealityEngineDarkBg
import com.example.ui.theme.RealityEngineEmerald
import com.example.ui.theme.RealityEngineSurface
import com.example.ui.theme.RealityEngineSurfaceElevated
import com.example.ui.theme.RealityEngineTextDisabled
import com.example.ui.theme.RealityEngineTextMuted
import com.example.ui.theme.RealityEngineTextPrimary
import com.example.ui.theme.RealityEngineTextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DialerScreen(
    viewModel: RealityEngineViewModel,
    onRequestDefaultPhone: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val dialerNumber by viewModel.dialerNumber.collectAsState()
    val people by viewModel.people.collectAsState()
    val recentCalls by viewModel.recentCalls.collectAsState()
    val isDefaultPhoneApp by viewModel.isDefaultPhoneApp.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) } // 0: KEYPAD, 1: RECENTS

    // Matching contact for dialed number
    val matchingContact = remember(dialerNumber, people) {
        if (dialerNumber.isBlank()) null
        else {
            val clean = dialerNumber.replace("[^0-9]".toRegex(), "")
            people.firstOrNull { person ->
                person.phoneNumber.replace("[^0-9]".toRegex(), "").contains(clean) ||
                        person.name.contains(dialerNumber, ignoreCase = true)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(RealityEngineDarkBg)
    ) {
        TacticalHeader(
            title = "REALITY ENGINE",
            subtitle = if (isDefaultPhoneApp) "DEFAULT PHONE · COPILOT ACTIVE" else "TELEPHONY & COPILOT CORE",
            onSettingsClick = { viewModel.openSettings(true) }
        )

        // First-Run / Persistent Default Phone Setup Banner (Hidden once granted)
        if (!isDefaultPhoneApp) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(RealityEngineSurfaceElevated)
                    .border(1.dp, RealityEngineAmber.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(RealityEngineAmber, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "DEFAULT PHONE SETUP",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = RealityEngineAmber,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(RealityEngineAmber.copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "RECOMMENDED",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = RealityEngineAmber
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Set Reality Engine as your default phone to enable real-time conversational intelligence, deception analysis, and live co-pilot guidance on every call.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = RealityEngineTextSecondary,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = onRequestDefaultPhone,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(38.dp)
                            .testTag("set_as_default_phone_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = RealityEngineAmber,
                            contentColor = RealityEngineDarkBg
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp),
                            tint = RealityEngineDarkBg
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "SET AS DEFAULT PHONE",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }
        }

        // Sub-tabs: KEYPAD / RECENTS
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = RealityEngineSurface,
            contentColor = RealityEngineAmber,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = RealityEngineAmber,
                    height = 2.dp
                )
            },
            divider = { Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(RealityEngineBorder)) }
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = {
                    Text(
                        "KEYPAD",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                        color = if (selectedTab == 0) RealityEngineAmber else RealityEngineTextMuted
                    )
                }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = {
                    Text(
                        "RECENTS (${recentCalls.size})",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                        color = if (selectedTab == 1) RealityEngineAmber else RealityEngineTextMuted
                    )
                }
            )
        }

        if (selectedTab == 0) {
            KeypadView(
                dialerNumber = dialerNumber,
                matchingContact = matchingContact,
                onDigitClick = { viewModel.appendDialDigit(it) },
                onDeleteClick = { viewModel.deleteDialDigit() },
                onClearClick = { viewModel.clearDialer() },
                onCallClick = {
                    viewModel.startOutgoingCall(
                        targetNumber = dialerNumber,
                        knownPerson = matchingContact
                    )
                },
                onSimulateIncoming = {
                    viewModel.triggerIncomingCall(people.firstOrNull { it.name == "Sarah" })
                }
            )
        } else {
            RecentsView(
                recentCalls = recentCalls,
                onCallContact = { record ->
                    viewModel.startOutgoingCall(
                        targetNumber = record.phoneNumber,
                        knownPerson = people.firstOrNull { it.id == record.personId || it.name == record.personName }
                    )
                }
            )
        }
    }
}

@Composable
private fun KeypadView(
    dialerNumber: String,
    matchingContact: PersonEntity?,
    onDigitClick: (String) -> Unit,
    onDeleteClick: () -> Unit,
    onClearClick: () -> Unit,
    onCallClick: () -> Unit,
    onSimulateIncoming: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Dialed Number Display Area
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp, bottom = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (matchingContact != null) {
                Text(
                    text = matchingContact.name.uppercase(),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Bold,
                        color = RealityEngineCyan,
                        letterSpacing = 1.2.sp
                    )
                )
                Text(
                    text = "${matchingContact.relationship} · ${matchingContact.organization}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = RealityEngineTextSecondary,
                        fontSize = 11.sp
                    )
                )
            } else {
                Text(
                    text = if (dialerNumber.isEmpty()) "NUMERICAL DIALER" else "DIALER",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.8.sp,
                        color = RealityEngineTextMuted
                    )
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (dialerNumber.isEmpty()) "" else dialerNumber,
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = if (dialerNumber.length > 12) 22.sp else 28.sp,
                        color = RealityEngineTextPrimary,
                        letterSpacing = 2.sp
                    ),
                    maxLines = 1,
                    textAlign = TextAlign.Center
                )

                if (dialerNumber.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier
                            .size(38.dp)
                            .testTag("dialer_backspace")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Backspace,
                            contentDescription = "Backspace",
                            tint = RealityEngineAmber,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // 3x4 Numerical Keypad Grid (1-9, *, 0, #) with custom styling
        RealityEngineDialerGrid(
            onDigitClick = onDigitClick,
            keySize = 72.dp,
            spacing = 10.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        )

        // Call Action Dock (Incoming simulation, Call button, Quick contact)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Incoming Call Test Trigger
                IconButton(
                    onClick = onSimulateIncoming,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(RealityEngineSurfaceElevated)
                        .border(1.dp, RealityEngineBorder, CircleShape)
                        .testTag("simulate_incoming_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.PhoneCallback,
                        contentDescription = "Simulate Incoming Call from Sarah",
                        tint = RealityEngineCyan,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Primary Call Trigger
                Button(
                    onClick = onCallClick,
                    modifier = Modifier
                        .size(68.dp)
                        .testTag("dialer_call_button"),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RealityEngineCallGreen,
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = "Initiate Call",
                        modifier = Modifier.size(30.dp)
                    )
                }

                // Quick Sarah Shortcut
                IconButton(
                    onClick = {
                        onDigitClick("+14158902134")
                    },
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(RealityEngineSurfaceElevated)
                        .border(1.dp, RealityEngineBorder, CircleShape)
                        .testTag("quick_sarah_button")
                ) {
                    Text(
                        text = "SARAH",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = RealityEngineAmber
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Tap 0 long for + · Green to call · Blue to simulate",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp,
                    color = RealityEngineTextMuted
                )
            )
        }
    }
}

@Composable
fun KeypadButton(
    digit: String,
    letters: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    RealityEngineDialerKey(
        keyItem = com.example.ui.components.KeypadKeyItem(
            digit = digit,
            letters = letters,
            longPressDigit = if (digit == "0") "+" else null
        ),
        onClick = onClick,
        onLongClick = {
            if (digit == "0") onClick() else onClick()
        },
        modifier = modifier,
        buttonSize = 72.dp
    )
}

@Composable
private fun RecentsView(
    recentCalls: List<CallRecordEntity>,
    onCallContact: (CallRecordEntity) -> Unit
) {
    if (recentCalls.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "NO RECENT CALLS RECORDED",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace,
                    color = RealityEngineTextMuted,
                    letterSpacing = 1.5.sp
                )
            )
        }
        return
    }

    val dateFormat = remember { SimpleDateFormat("MMM d, HH:mm", Locale.US) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(recentCalls, key = { it.id }) { call ->
            PrecisionCard(
                modifier = Modifier.clickable { onCallContact(call) }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        val (icon, tint) = when (call.callType) {
                            "MISSED" -> Icons.AutoMirrored.Filled.CallMissed to RealityEngineCallRed
                            "INCOMING" -> Icons.AutoMirrored.Filled.CallReceived to RealityEngineCyan
                            else -> Icons.AutoMirrored.Filled.CallMade to RealityEngineCallGreen
                        }
                        Icon(
                            imageVector = icon,
                            contentDescription = call.callType,
                            tint = tint,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = call.personName,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = RealityEngineTextPrimary
                                )
                            )
                            Text(
                                text = if (call.topic.isNotBlank()) call.topic else call.phoneNumber,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = RealityEngineTextSecondary,
                                    fontSize = 11.sp
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (call.durationSeconds > 0) {
                                val mins = call.durationSeconds / 60
                                val secs = call.durationSeconds % 60
                                Text(
                                    text = "Duration: ${String.format(Locale.US, "%02d:%02d", mins, secs)} · Deception Score: ${call.deceptionAvgScore}%",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        color = RealityEngineTextMuted,
                                        fontSize = 9.sp
                                    )
                                )
                            }
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = dateFormat.format(Date(call.timestamp)),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                color = RealityEngineTextMuted,
                                fontSize = 9.sp
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        IconButton(
                            onClick = { onCallContact(call) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Call,
                                contentDescription = "Call",
                                tint = RealityEngineCallGreen,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
