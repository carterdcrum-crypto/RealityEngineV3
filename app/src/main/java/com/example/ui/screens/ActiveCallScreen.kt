package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import com.example.engine.Speaker
import com.example.engine.StrategyAlternative
import com.example.engine.TranscriptSegment
import com.example.ui.RealityEngineViewModel
import com.example.ui.components.PrecisionCard
import com.example.ui.components.PrecisionSectionHeader
import com.example.ui.components.RealityEngineDialerGrid
import com.example.ui.components.SignalSliderRow
import com.example.ui.theme.RealityEngineAmber
import com.example.ui.theme.RealityEngineAmberContainer
import com.example.ui.theme.RealityEngineAmberQuote
import com.example.ui.theme.RealityEngineBgWhite05
import com.example.ui.theme.RealityEngineBorder
import com.example.ui.theme.RealityEngineBorderSubtle
import com.example.ui.theme.RealityEngineBorderWhite10
import com.example.ui.theme.RealityEngineCallRed
import com.example.ui.theme.RealityEngineCrimson
import com.example.ui.theme.RealityEngineCrimsonGlow
import com.example.ui.theme.RealityEngineCrimsonLight
import com.example.ui.theme.RealityEngineCrimsonMuted
import com.example.ui.theme.RealityEngineCyan
import com.example.ui.theme.RealityEngineDarkBg
import com.example.ui.theme.RealityEngineEmerald
import com.example.ui.theme.RealityEngineSurface
import com.example.ui.theme.RealityEngineSurfaceElevated
import com.example.ui.theme.RealityEngineSurfaceHighlight
import com.example.ui.theme.RealityEngineTextDisabled
import com.example.ui.theme.RealityEngineTextMuted
import com.example.ui.theme.RealityEngineTextPrimary
import com.example.ui.theme.RealityEngineTextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveCallScreen(
    viewModel: RealityEngineViewModel,
    modifier: Modifier = Modifier
) {
    val callState by viewModel.activeCall.collectAsState()
    val transcriptListState = rememberLazyListState()
    var simInputText by remember { mutableStateOf("") }

    // Auto-scroll transcript on new turns
    LaunchedEffect(callState.transcript.size) {
        if (callState.transcript.isNotEmpty()) {
            transcriptListState.animateScrollToItem(callState.transcript.size - 1)
        }
    }

    val callerName = callState.caller?.name ?: "Sarah Jenkins"
    val durationFormatted = viewModel.formatCallDuration(callState.elapsedSeconds)
    val activeTopic = callState.objective.ifBlank { "Project X • Active" }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(RealityEngineDarkBg)
    ) {
        // ----------------------------------------------------
        // TOP CALL HEADER (Matches Elegant Dark layout)
        // ----------------------------------------------------
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "REALITY ENGINE V2",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp,
                        fontSize = 10.sp,
                        color = RealityEngineAmber
                    )
                )
                Text(
                    text = callerName,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Light,
                        fontSize = 24.sp,
                        letterSpacing = (-0.5).sp,
                        color = RealityEngineTextPrimary
                    )
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = durationFormatted,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = RealityEngineAmber.copy(alpha = 0.85f)
                    )
                )
                Text(
                    text = activeTopic.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.SansSerif,
                        letterSpacing = 1.8.sp,
                        fontSize = 10.sp,
                        color = RealityEngineTextSecondary.copy(alpha = 0.4f)
                    )
                )
            }
        }

        // ----------------------------------------------------
        // MAIN SCROLLABLE BODY
        // ----------------------------------------------------
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            state = transcriptListState,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Inconsistency Alert Banner (if present)
            if (callState.activeInconsistency != null) {
                item {
                    val inc = callState.activeInconsistency!!
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(RealityEngineSurface)
                            .border(1.dp, RealityEngineCrimsonMuted, RoundedCornerShape(16.dp))
                            .padding(16.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = "Warning",
                                        tint = RealityEngineCrimson,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "POSSIBLE INCONSISTENCY",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontFamily = FontFamily.SansSerif,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 1.5.sp,
                                            fontSize = 10.sp,
                                            color = RealityEngineCrimsonLight
                                        )
                                    )
                                }
                                Text(
                                    text = "Confidence: ${inc.confidence}%",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.sp,
                                        color = RealityEngineAmber
                                    )
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Previous: \"${inc.previousStatement}\"",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = RealityEngineTextSecondary,
                                    fontSize = 11.sp
                                )
                            )
                            Text(
                                text = "Current: \"${inc.currentStatement}\"",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = RealityEngineTextPrimary,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }
                }
            }

            // Memory Candidate Alert (if present)
            if (callState.activeMemoryAlert != null) {
                item {
                    val mem = callState.activeMemoryAlert!!
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(RealityEngineSurface)
                            .border(1.dp, RealityEngineBorder, RoundedCornerShape(16.dp))
                            .padding(16.dp)
                    ) {
                        Column {
                            PrecisionSectionHeader(
                                title = "MEMORY CANDIDATE",
                                tag = mem.suggestedState,
                                tagColor = RealityEngineCyan
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "\"${mem.statement}\"",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = RealityEngineTextPrimary,
                                    fontSize = 13.sp
                                )
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "DISMISS",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = FontFamily.SansSerif,
                                        color = RealityEngineTextMuted,
                                        fontSize = 10.sp,
                                        letterSpacing = 1.sp
                                    ),
                                    modifier = Modifier
                                        .clickable { viewModel.dismissMemoryCandidate() }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                        .testTag("dismiss_memory_btn")
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(RealityEngineAmber)
                                        .clickable { viewModel.saveMemoryCandidate(mem, "OBSERVED") }
                                        .padding(horizontal = 12.dp, vertical = 5.dp)
                                        .testTag("save_memory_btn")
                                ) {
                                    Text(
                                        text = "SAVE",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontFamily = FontFamily.SansSerif,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp,
                                            color = RealityEngineDarkBg
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ----------------------------------------------------
            // 1. LIVE TRANSCRIPT SECTION (#0F0F11, rounded-2xl)
            // ----------------------------------------------------
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(RealityEngineSurface)
                        .border(1.dp, RealityEngineBorder, RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "LIVE TRANSCRIPT",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.SansSerif,
                                letterSpacing = 2.sp,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = RealityEngineTextSecondary.copy(alpha = 0.35f)
                            )
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        if (callState.transcript.isEmpty()) {
                            Text(
                                text = "Awaiting speech stream...",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = RealityEngineTextMuted,
                                    fontSize = 12.sp
                                )
                            )
                        } else {
                            callState.transcript.forEach { segment ->
                                ElegantTranscriptTurn(segment = segment)
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                        }
                    }
                }
            }

            // ----------------------------------------------------
            // 2. AI CO-PILOT RECOMMENDATION HERO CARD (#151518)
            // ----------------------------------------------------
            item {
                val copilot = callState.copilotResult
                val activeStrat = callState.selectedAlternative?.strategy ?: copilot?.recommendedStrategy
                val activeTone = callState.selectedAlternative?.tone ?: copilot?.tone
                val activeResponse = callState.selectedAlternative?.suggestedResponse ?: copilot?.suggestedResponse ?: "Can you help me understand what changed in the timeline?"

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(RealityEngineSurfaceElevated)
                        .border(1.dp, RealityEngineAmber.copy(alpha = 0.30f), RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Card Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column {
                                Text(
                                    text = "AI CO-PILOT RECOMMENDATION",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = FontFamily.SansSerif,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.8.sp,
                                        fontSize = 10.sp,
                                        color = RealityEngineAmber
                                    )
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = activeStrat?.displayName ?: "Cognitive Probe",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontFamily = FontFamily.SansSerif,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 18.sp,
                                        color = RealityEngineTextPrimary
                                    )
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "CONFIDENCE",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = FontFamily.SansSerif,
                                        fontSize = 10.sp,
                                        letterSpacing = 1.sp,
                                        color = RealityEngineTextSecondary.copy(alpha = 0.4f)
                                    )
                                )
                                Text(
                                    text = "${copilot?.confidence ?: 84}%",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp,
                                        color = RealityEngineAmber
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Tone & Strategy Tags Grid (2 Columns)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Left Col: Tone
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "TONE",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = FontFamily.SansSerif,
                                        fontSize = 10.sp,
                                        letterSpacing = 1.sp,
                                        color = RealityEngineTextSecondary.copy(alpha = 0.4f)
                                    )
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    val tones = if (activeTone != null) listOf(activeTone.displayName) else listOf("CALM", "CURIOUS")
                                    tones.forEach { toneName ->
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(RealityEngineBgWhite05)
                                                .border(1.dp, RealityEngineBorderWhite10, RoundedCornerShape(4.dp))
                                                .padding(horizontal = 8.dp, vertical = 3.dp)
                                        ) {
                                            Text(
                                                text = toneName.uppercase(),
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontFamily = FontFamily.SansSerif,
                                                    fontSize = 10.sp,
                                                    color = RealityEngineTextPrimary
                                                )
                                            )
                                        }
                                    }
                                }
                            }

                            // Right Col: Strategies
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "STRATEGIES",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = FontFamily.SansSerif,
                                        fontSize = 10.sp,
                                        letterSpacing = 1.sp,
                                        color = RealityEngineTextSecondary.copy(alpha = 0.4f)
                                    )
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "Mirror",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontStyle = FontStyle.Italic,
                                            fontSize = 11.sp,
                                            color = RealityEngineAmber
                                        ),
                                        modifier = Modifier.clickable {
                                            viewModel.selectAlternativeStrategy(
                                                StrategyAlternative(
                                                    com.example.engine.StrategyType.MIRRORING,
                                                    "\"The meeting was never Friday?\"",
                                                    com.example.engine.ToneType.CALM
                                                )
                                            )
                                        }
                                    )
                                    Text(
                                        text = "•",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = RealityEngineTextSecondary.copy(alpha = 0.4f)
                                        )
                                    )
                                    Text(
                                        text = "Pivot",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontStyle = FontStyle.Italic,
                                            fontSize = 11.sp,
                                            color = RealityEngineTextSecondary.copy(alpha = 0.6f)
                                        ),
                                        modifier = Modifier.clickable {
                                            viewModel.selectAlternativeStrategy(
                                                StrategyAlternative(
                                                    com.example.engine.StrategyType.PIVOT,
                                                    "What date did you have in mind for the initial review then?",
                                                    com.example.engine.ToneType.DIPLOMATIC
                                                )
                                            )
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Suggested Response Box (Amber tinted box with italic quote)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(RealityEngineAmberContainer)
                                .border(1.dp, RealityEngineAmber.copy(alpha = 0.20f), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = "\"$activeResponse\"",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontStyle = FontStyle.Italic,
                                    fontSize = 14.sp,
                                    lineHeight = 20.sp,
                                    color = RealityEngineAmberQuote
                                )
                            )
                        }
                    }
                }
            }

            // ----------------------------------------------------
            // 3. LIVE SIGNALS & DECEPTION SIGNAL SECTION
            // ----------------------------------------------------
            item {
                val copilot = callState.copilotResult
                val signals = copilot?.liveSignals ?: com.example.engine.LiveSignalMeters(0.73f, 0.24f, 0.40f)
                val deception = copilot?.deceptionSignal ?: com.example.engine.DeceptionSignalState(
                    score = 73,
                    isElevated = true,
                    contributors = listOf(
                        com.example.engine.DeceptionContributor("Linguistic distancing", 18),
                        com.example.engine.DeceptionContributor("Statement inconsistency", 31)
                    )
                )

                // Pulsing dot animation for deception signal
                val pulseTransition = rememberInfiniteTransition(label = "pulse")
                val pulseAlpha by pulseTransition.animateFloat(
                    initialValue = 0.3f,
                    targetValue = 1.0f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(600, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "dotPulse"
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(RealityEngineSurface)
                        .border(1.dp, RealityEngineBorder, RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "LIVE SIGNALS",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.SansSerif,
                                    letterSpacing = 2.sp,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = RealityEngineTextSecondary.copy(alpha = 0.4f)
                                )
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(RealityEngineCrimsonGlow)
                                    .border(1.dp, RealityEngineCrimsonMuted.copy(alpha = 0.5f), RoundedCornerShape(3.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "EXPERIMENTAL",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = FontFamily.SansSerif,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = RealityEngineCrimson
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Sliders (Linguistic, Factual)
                        SignalSliderRow(
                            label = "Linguistic",
                            position = signals.linguisticPosition,
                            accentColor = RealityEngineAmber
                        )
                        SignalSliderRow(
                            label = "Factual",
                            position = signals.factualPosition,
                            accentColor = RealityEngineAmber
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(RealityEngineBgWhite05))
                        Spacer(modifier = Modifier.height(10.dp))

                        // Bottom Deception Indicator Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(RealityEngineCrimson.copy(alpha = pulseAlpha), CircleShape)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "DECEPTION SIGNAL: ${deception.score}%",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = FontFamily.SansSerif,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        color = RealityEngineCrimsonLight
                                    )
                                )
                            }

                            Text(
                                text = "Inconsistency Detected",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontStyle = FontStyle.Italic,
                                    fontSize = 9.sp,
                                    color = RealityEngineTextSecondary.copy(alpha = 0.4f)
                                )
                            )
                        }
                    }
                }
            }

            // Interactive Turn Simulator & Testing
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(RealityEngineSurface)
                        .border(1.dp, RealityEngineBorder, RoundedCornerShape(16.dp))
                        .padding(14.dp)
                ) {
                    Column {
                        Text(
                            text = "SPEECH SIMULATION INPUT",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.SansSerif,
                                fontSize = 9.sp,
                                letterSpacing = 1.5.sp,
                                color = RealityEngineTextSecondary.copy(alpha = 0.4f)
                            )
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = simInputText,
                                onValueChange = { simInputText = it },
                                placeholder = {
                                    Text(
                                        "Type speech utterance (e.g. Started in May)...",
                                        color = RealityEngineTextMuted,
                                        fontSize = 11.sp
                                    )
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("transcript_sim_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = RealityEngineAmber,
                                    unfocusedBorderColor = RealityEngineBorder,
                                    focusedTextColor = RealityEngineTextPrimary,
                                    unfocusedTextColor = RealityEngineTextPrimary,
                                    focusedContainerColor = RealityEngineDarkBg,
                                    unfocusedContainerColor = RealityEngineDarkBg
                                ),
                                singleLine = true
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(
                                onClick = {
                                    if (simInputText.isNotBlank()) {
                                        viewModel.sendManualUtterance(simInputText, isYou = false)
                                        simInputText = ""
                                    }
                                },
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(RealityEngineAmber)
                                    .testTag("send_other_speech")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Send,
                                    contentDescription = "Send",
                                    tint = RealityEngineDarkBg,
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        // ----------------------------------------------------
        // 4. FOOTER CALL CONTROLS (Matches Design HTML footer)
        // ----------------------------------------------------
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Mute / Mic
            ElegantCallButton(
                icon = if (callState.isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                isActive = callState.isMuted,
                onClick = { viewModel.toggleMute() }
            )

            // Speaker / Volume
            ElegantCallButton(
                icon = if (callState.isSpeakerOn) Icons.Default.VolumeUp else Icons.Default.VolumeDown,
                isActive = callState.isSpeakerOn,
                onClick = { viewModel.toggleSpeaker() }
            )

            // Prominent Red End Call Button (60dp circle, Red-600)
            Button(
                onClick = { viewModel.endActiveCall() },
                modifier = Modifier
                    .size(60.dp)
                    .testTag("end_active_call_button"),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = RealityEngineCallRed,
                    contentColor = Color.White
                ),
                contentPadding = PaddingValues(0.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CallEnd,
                    contentDescription = "End Call",
                    modifier = Modifier.size(28.dp)
                )
            }

            // Keypad
            ElegantCallButton(
                icon = Icons.Default.Dialpad,
                isActive = callState.isKeypadOpen,
                onClick = { viewModel.toggleKeypad() }
            )

            // Hold / Pause
            ElegantCallButton(
                icon = if (callState.isHold) Icons.Default.PlayArrow else Icons.Default.Pause,
                isActive = callState.isHold,
                onClick = { viewModel.toggleHold() }
            )
        }

        // In-Call 3x4 DTMF Keypad Bottom Sheet
        if (callState.isKeypadOpen) {
            ModalBottomSheet(
                onDismissRequest = { viewModel.toggleKeypad() },
                containerColor = RealityEngineSurface,
                scrimColor = Color.Black.copy(alpha = 0.75f),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "IN-CALL DTMF KEYPAD",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = RealityEngineAmber,
                            letterSpacing = 1.5.sp
                        )
                        IconButton(
                            onClick = { viewModel.toggleKeypad() },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close Keypad",
                                tint = RealityEngineTextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // DTMF Output Display
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(RealityEngineDarkBg)
                            .border(1.dp, RealityEngineBorder, RoundedCornerShape(8.dp))
                            .padding(horizontal = 14.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = if (callState.keypadDtmf.isEmpty()) "TOUCH TONES..." else callState.keypadDtmf,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = if (callState.keypadDtmf.isEmpty()) RealityEngineTextMuted else RealityEngineCyan,
                            letterSpacing = 2.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 3x4 Grid
                    RealityEngineDialerGrid(
                        onDigitClick = { digit ->
                            viewModel.appendDtmf(digit)
                        },
                        keySize = 68.dp,
                        spacing = 10.dp,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ElegantTranscriptTurn(segment: TranscriptSegment) {
    val isYou = segment.speaker == Speaker.YOU

    if (isYou) {
        // You (Right aligned bubble: bg-[#1A1A1C], border-[#2A2A2C])
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = "YOU",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 10.sp,
                    letterSpacing = 1.sp,
                    color = RealityEngineTextSecondary
                )
            )
            Spacer(modifier = Modifier.height(3.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp, bottomEnd = 12.dp, topEnd = 0.dp))
                    .background(RealityEngineSurfaceHighlight)
                    .border(1.dp, RealityEngineBorderSubtle, RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp, bottomEnd = 12.dp, topEnd = 0.dp))
                    .padding(12.dp)
            ) {
                Text(
                    text = "\"${segment.text}\"",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = RealityEngineTextPrimary,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                )
            }
        }
    } else {
        // Other (Left aligned: amber name, text-[#A0A0A0])
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = segment.speakerName.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 10.sp,
                    letterSpacing = 1.sp,
                    color = RealityEngineAmber
                )
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = "\"${segment.text}\"",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = RealityEngineTextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 19.sp
                )
            )
        }
    }
}

@Composable
private fun ElegantCallButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isActive: Boolean,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(if (isActive) RealityEngineAmber.copy(alpha = 0.2f) else RealityEngineBgWhite05)
            .border(
                1.dp,
                if (isActive) RealityEngineAmber else RealityEngineBorderWhite10,
                CircleShape
            )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isActive) RealityEngineAmber else RealityEngineTextPrimary.copy(alpha = 0.6f),
            modifier = Modifier.size(20.dp)
        )
    }
}

