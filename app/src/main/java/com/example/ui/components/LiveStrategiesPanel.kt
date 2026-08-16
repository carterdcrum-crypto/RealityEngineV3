package com.example.ui.components

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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.StrategyRecommendation
import com.example.engine.StrategyType
import com.example.ui.theme.RealityEngineAmber
import com.example.ui.theme.RealityEngineAmberContainer
import com.example.ui.theme.RealityEngineAmberQuote
import com.example.ui.theme.RealityEngineBgWhite05
import com.example.ui.theme.RealityEngineBorder
import com.example.ui.theme.RealityEngineBorderSubtle
import com.example.ui.theme.RealityEngineBorderWhite10
import com.example.ui.theme.RealityEngineCyan
import com.example.ui.theme.RealityEngineEmerald
import com.example.ui.theme.RealityEngineSurfaceElevated
import com.example.ui.theme.RealityEngineTextMuted
import com.example.ui.theme.RealityEngineTextPrimary
import com.example.ui.theme.RealityEngineTextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun LiveStrategiesPanel(
    strategies: List<StrategyRecommendation>,
    selectedStrategy: StrategyRecommendation?,
    primaryStrategyType: StrategyType?,
    onSelectStrategy: (StrategyRecommendation) -> Unit,
    onSendUtterance: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()
    var copiedStrategyId by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Section Header
        PrecisionSectionHeader(
            title = "LIVE STRATEGIES",
            subtitle = "DYNAMIC TACTICAL ACTION REGISTRY"
        )

        if (strategies.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(RealityEngineSurfaceElevated)
                    .border(1.dp, RealityEngineBorderSubtle, RoundedCornerShape(12.dp))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Awaiting live transcript to evaluate strategic vectors...",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.SansSerif,
                        color = RealityEngineTextMuted
                    )
                )
            }
        } else {
            strategies.forEach { strat ->
                val isSelected = selectedStrategy?.type == strat.type ||
                        (selectedStrategy == null && strat.type == primaryStrategyType)
                val isPrimary = strat.type == primaryStrategyType || strat.isPrimaryRecommended
                val isCopied = copiedStrategyId == strat.id

                StrategyCard(
                    strategy = strat,
                    isSelected = isSelected,
                    isPrimary = isPrimary,
                    isCopied = isCopied,
                    onSelect = { onSelectStrategy(strat) },
                    onCopy = {
                        clipboardManager.setText(AnnotatedString(strat.suggestedResponse))
                        copiedStrategyId = strat.id
                        coroutineScope.launch {
                            delay(2000)
                            if (copiedStrategyId == strat.id) {
                                copiedStrategyId = null
                            }
                        }
                    },
                    onSend = {
                        onSendUtterance(strat.suggestedResponse)
                    }
                )
            }
        }
    }
}

@Composable
fun StrategyCard(
    strategy: StrategyRecommendation,
    isSelected: Boolean,
    isPrimary: Boolean,
    isCopied: Boolean,
    onSelect: () -> Unit,
    onCopy: () -> Unit,
    onSend: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tagSlug = strategy.type.name.lowercase()
    val borderColor = when {
        isSelected -> RealityEngineAmber.copy(alpha = 0.85f)
        isPrimary -> RealityEngineAmber.copy(alpha = 0.35f)
        else -> RealityEngineBorderWhite10
    }
    val containerBg = if (isSelected) {
        RealityEngineSurfaceElevated
    } else {
        RealityEngineSurfaceElevated.copy(alpha = 0.80f)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(containerBg)
            .border(if (isSelected) 1.5.dp else 1.dp, borderColor, RoundedCornerShape(14.dp))
            .testTag("strategy_card_$tagSlug")
            .clickable { onSelect() }
            .padding(14.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header Row: Strategy Name + Badges + Confidence
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = strategy.name,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp,
                            fontSize = 13.sp,
                            color = if (isSelected) RealityEngineAmber else RealityEngineTextPrimary
                        )
                    )

                    // Tone Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(RealityEngineBgWhite05)
                            .border(1.dp, RealityEngineBorderWhite10, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = strategy.tone.displayName,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.SansSerif,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Medium,
                                letterSpacing = 0.5.sp,
                                color = RealityEngineTextSecondary
                            )
                        )
                    }

                    // Primary Tag if applicable
                    if (isPrimary) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(RealityEngineAmber.copy(alpha = 0.15f))
                                .border(1.dp, RealityEngineAmber.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "RECOMMENDED",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.SansSerif,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp,
                                    color = RealityEngineAmber
                                )
                            )
                        }
                    }

                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(RealityEngineEmerald.copy(alpha = 0.15f))
                                .border(1.dp, RealityEngineEmerald.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "ACTIVE",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.SansSerif,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp,
                                    color = RealityEngineEmerald
                                )
                            )
                        }
                    }
                }

                // Confidence / Relevance Score
                Text(
                    text = "${strategy.confidence}% RELEVANCE",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isSelected) RealityEngineAmber else RealityEngineTextSecondary.copy(alpha = 0.7f)
                    )
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Short Explanation / Purpose
            Text(
                text = strategy.purpose.ifBlank { strategy.description },
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    color = RealityEngineTextSecondary
                )
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Reason for recommendation
            if (strategy.recommendationReason.isNotBlank()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "WHY:",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            letterSpacing = 0.8.sp,
                            color = RealityEngineCyan
                        )
                    )
                    Text(
                        text = strategy.recommendationReason,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.SansSerif,
                            fontSize = 11.sp,
                            lineHeight = 15.sp,
                            color = RealityEngineTextMuted
                        )
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
            } else {
                Spacer(modifier = Modifier.height(6.dp))
            }

            // Suggested Response Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isSelected) RealityEngineAmberContainer else RealityEngineBgWhite05)
                    .border(
                        1.dp,
                        if (isSelected) RealityEngineAmber.copy(alpha = 0.35f) else RealityEngineBorderSubtle,
                        RoundedCornerShape(10.dp)
                    )
                    .padding(12.dp)
            ) {
                Text(
                    text = strategy.suggestedResponse.ifBlank { "\"Awaiting verbal context...\"" },
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontStyle = FontStyle.Italic,
                        fontSize = 13.sp,
                        lineHeight = 19.sp,
                        color = if (isSelected) RealityEngineAmberQuote else RealityEngineTextPrimary
                    )
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Buttons Row: Select Strategy, Copy, Send
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Use/Select Strategy Button
                Button(
                    onClick = onSelect,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSelected) RealityEngineAmber else RealityEngineBgWhite05,
                        contentColor = if (isSelected) Color.Black else RealityEngineTextPrimary
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier
                        .height(34.dp)
                        .testTag("strategy_select_btn_$tagSlug")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Active",
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Text(
                            text = if (isSelected) "ACTIVE STRATEGY" else "USE STRATEGY",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                letterSpacing = 0.5.sp
                            )
                        )
                    }
                }

                // Copy & Send Action Icons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Copy Button
                    IconButton(
                        onClick = onCopy,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(RealityEngineBgWhite05)
                            .border(1.dp, RealityEngineBorderWhite10, RoundedCornerShape(8.dp))
                            .testTag("strategy_copy_btn_$tagSlug")
                    ) {
                        if (isCopied) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Copied",
                                tint = RealityEngineEmerald,
                                modifier = Modifier.size(16.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy Prompt",
                                tint = RealityEngineTextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    // Send Utterance Button
                    IconButton(
                        onClick = onSend,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(RealityEngineAmber.copy(alpha = 0.15f))
                            .border(1.dp, RealityEngineAmber.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .testTag("strategy_send_btn_$tagSlug")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Send As Utterance",
                            tint = RealityEngineAmber,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
