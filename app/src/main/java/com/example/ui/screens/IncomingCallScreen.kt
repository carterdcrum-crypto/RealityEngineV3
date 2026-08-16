package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PersonEntity
import com.example.ui.components.LiveWaveformBar
import com.example.ui.components.PrecisionCard
import com.example.ui.components.PrecisionSectionHeader
import com.example.ui.theme.RealityEngineAmber
import com.example.ui.theme.RealityEngineBorder
import com.example.ui.theme.RealityEngineCallGreen
import com.example.ui.theme.RealityEngineCallRed
import com.example.ui.theme.RealityEngineCyan
import com.example.ui.theme.RealityEngineDarkBg
import com.example.ui.theme.RealityEngineSurface
import com.example.ui.theme.RealityEngineSurfaceElevated
import com.example.ui.theme.RealityEngineTextMuted
import com.example.ui.theme.RealityEngineTextPrimary
import com.example.ui.theme.RealityEngineTextSecondary

@Composable
fun IncomingCallScreen(
    caller: PersonEntity?,
    phoneNumber: String,
    onAnswer: () -> Unit,
    onDecline: () -> Unit,
    modifier: Modifier = Modifier
) {
    val callerName = caller?.name ?: "Unknown Caller"
    val isKnown = caller != null

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(RealityEngineDarkBg)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top Tactical Identifier
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(RealityEngineAmber, CircleShape)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "REALITY ENGINE",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 2.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = RealityEngineTextPrimary
                    )
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "INCOMING CALL",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 2.sp,
                    color = RealityEngineCyan
                )
            )
            Spacer(modifier = Modifier.height(12.dp))
            LiveWaveformBar(isActive = true, color = RealityEngineCyan)
        }

        // Caller Identity & Pre-loaded Context
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Avatar / Monogram
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(RealityEngineSurfaceElevated)
                    .border(2.dp, RealityEngineAmber, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = callerName.take(2).uppercase(),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = RealityEngineAmber
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = callerName,
                style = MaterialTheme.typography.displayMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = RealityEngineTextPrimary
                ),
                textAlign = TextAlign.Center
            )

            Text(
                text = if (isKnown) "Known contact" else phoneNumber,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    color = if (isKnown) RealityEngineCyan else RealityEngineTextSecondary,
                    fontSize = 12.sp,
                    letterSpacing = 1.sp
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Relevant Context Card (Mandated in prompt)
            PrecisionCard(
                borderColor = RealityEngineBorder,
                backgroundColor = RealityEngineSurface
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    PrecisionSectionHeader(
                        title = "RELEVANT CONTEXT",
                        tag = if (isKnown) "VERIFIED" else "NEW",
                        tagColor = if (isKnown) RealityEngineCyan else RealityEngineAmber
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    ContextRow(label = "PRIMARY TOPIC", value = caller?.currentTopics?.ifBlank { "Project X" } ?: "Project X")
                    ContextRow(label = "LAST CONTACT", value = "3 days ago")
                    if (caller?.openQuestions?.isNotBlank() == true) {
                        ContextRow(label = "OPEN QUESTION", value = caller.openQuestions)
                    } else {
                        ContextRow(label = "OPEN QUESTION", value = "Did Project X launch?")
                    }
                    if (caller?.recentCommitment?.isNotBlank() == true) {
                        ContextRow(label = "RECENT COMMITMENT", value = caller.recentCommitment)
                    }
                }
            }
        }

        // Action Buttons (ANSWER / DECLINE)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Decline Button
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Button(
                    onClick = onDecline,
                    modifier = Modifier
                        .size(72.dp)
                        .testTag("decline_call_button"),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RealityEngineCallRed,
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CallEnd,
                        contentDescription = "Decline Call",
                        modifier = Modifier.size(32.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "DECLINE",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = RealityEngineTextSecondary
                    )
                )
            }

            // Answer Button
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Button(
                    onClick = onAnswer,
                    modifier = Modifier
                        .size(72.dp)
                        .testTag("answer_call_button"),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RealityEngineCallGreen,
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = "Answer Call",
                        modifier = Modifier.size(32.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "ANSWER",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = RealityEngineCallGreen
                    )
                )
            }
        }
    }
}

@Composable
private fun ContextRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                color = RealityEngineTextMuted,
                letterSpacing = 1.sp
            ),
            modifier = Modifier.width(110.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = RealityEngineTextPrimary
            ),
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.End
        )
    }
}
