package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.RealityEngineViewModel
import com.example.ui.components.PrecisionCard
import com.example.ui.components.PrecisionSectionHeader
import com.example.ui.components.SignalSliderRow
import com.example.ui.components.TacticalHeader
import com.example.ui.theme.RealityEngineAmber
import com.example.ui.theme.RealityEngineBorder
import com.example.ui.theme.RealityEngineCrimson
import com.example.ui.theme.RealityEngineCyan
import com.example.ui.theme.RealityEngineDarkBg
import com.example.ui.theme.RealityEngineEmerald
import com.example.ui.theme.RealityEngineSurface
import com.example.ui.theme.RealityEngineSurfaceElevated
import com.example.ui.theme.RealityEngineTextMuted
import com.example.ui.theme.RealityEngineTextPrimary
import com.example.ui.theme.RealityEngineTextSecondary

@Composable
fun SignalsScreen(
    viewModel: RealityEngineViewModel,
    modifier: Modifier = Modifier
) {
    val claims by viewModel.allClaims.collectAsState()
    val inconsistentClaims by viewModel.inconsistentClaims.collectAsState()
    val signalsHistory by viewModel.allSignals.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(RealityEngineDarkBg)
    ) {
        TacticalHeader(
            title = "REALITY ENGINE",
            subtitle = "CLAIM CONSISTENCY & SIGNALS",
            onSettingsClick = { viewModel.openSettings(true) }
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Mandatory Human Interpretation Notice Card
            item {
                PrecisionCard(
                    borderColor = RealityEngineAmber.copy(alpha = 0.5f),
                    backgroundColor = RealityEngineSurfaceElevated
                ) {
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Notice",
                            tint = RealityEngineAmber,
                            modifier = Modifier.size(18.dp).padding(top = 2.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "EXPERIMENTAL SIGNAL ENGINE",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.2.sp,
                                    color = RealityEngineAmber
                                )
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "All deception scoring and statement contradiction markers require human interpretation and contextual verification. Signals are derived from linguistic distance, acoustic prosody, and comparative timeline baselines.",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = RealityEngineTextSecondary,
                                    fontSize = 11.sp,
                                    lineHeight = 16.sp
                                )
                            )
                        }
                    }
                }
            }

            // Inconsistent Claims Section
            item {
                PrecisionSectionHeader(
                    title = "STATEMENT INCONSISTENCY TRACKER",
                    tag = "${inconsistentClaims.size} DETECTED",
                    tagColor = RealityEngineCrimson
                )
            }

            if (inconsistentClaims.isEmpty()) {
                item {
                    PrecisionCard {
                        Text(
                            text = "NO CONTRADICTIONS DETECTED IN CURRENT CORPUS",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                color = RealityEngineTextMuted
                            )
                        )
                    }
                }
            } else {
                items(inconsistentClaims, key = { it.id }) { claim ->
                    PrecisionCard(
                        borderColor = RealityEngineCrimson,
                        backgroundColor = RealityEngineSurfaceElevated
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
                                        contentDescription = "Contradiction",
                                        tint = RealityEngineCrimson,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = claim.personName.uppercase(),
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            color = RealityEngineTextPrimary
                                        )
                                    )
                                }
                                Text(
                                    text = "Confidence: ${claim.inconsistencyConfidence}%",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        color = RealityEngineAmber,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(RealityEngineSurface)
                                    .border(1.dp, RealityEngineBorder, RoundedCornerShape(6.dp))
                                    .padding(8.dp)
                            ) {
                                Column {
                                    Text(
                                        text = "PREVIOUS BASELINE STATEMENT:",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 9.sp,
                                            color = RealityEngineTextMuted
                                        )
                                    )
                                    Text(
                                        text = "\"${claim.previousStatement}\"",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = RealityEngineTextSecondary,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    )

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Text(
                                        text = "CURRENT CONTRADICTING STATEMENT:",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 9.sp,
                                            color = RealityEngineCrimson
                                        )
                                    )
                                    Text(
                                        text = "\"${claim.currentStatement}\"",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = RealityEngineTextPrimary,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    )
                                }
                            }

                            if (claim.context.isNotBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Context: ${claim.context}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.sp,
                                        color = RealityEngineTextMuted
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // All Extracted Claims
            item {
                PrecisionSectionHeader(
                    title = "EXTRACTED CLAIMS CORPUS",
                    tag = "${claims.size} TOTAL",
                    tagColor = RealityEngineCyan
                )
            }

            items(claims, key = { it.id }) { claim ->
                PrecisionCard {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = claim.personName.uppercase(),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = RealityEngineCyan
                                )
                            )
                            Text(
                                text = claim.category.uppercase(),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 9.sp,
                                    color = RealityEngineTextMuted
                                )
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "\"${claim.currentStatement}\"",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = RealityEngineTextPrimary
                            )
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}
