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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallEnd
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
import com.example.ui.theme.RealityEngineAmber
import com.example.ui.theme.RealityEngineCallRed
import com.example.ui.theme.RealityEngineCyan
import com.example.ui.theme.RealityEngineDarkBg
import com.example.ui.theme.RealityEngineSurfaceElevated
import com.example.ui.theme.RealityEngineTextMuted
import com.example.ui.theme.RealityEngineTextPrimary
import com.example.ui.theme.RealityEngineTextSecondary

import com.example.telecom.CallState

@Composable
fun OutgoingCallScreen(
    caller: PersonEntity?,
    phoneNumber: String,
    callState: CallState = CallState.CONNECTING,
    rawStatus: String? = null,
    onEndCall: () -> Unit,
    modifier: Modifier = Modifier
) {
    val callerName = caller?.name ?: phoneNumber
    val statusText = when {
        !rawStatus.isNullOrBlank() -> rawStatus.replace("-", " ").uppercase()
        callState == CallState.RINGING -> "RINGING..."
        callState == CallState.CONNECTING -> "CONNECTING..."
        callState == CallState.ACTIVE -> "ACTIVE"
        else -> "DIALING..."
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(RealityEngineDarkBg)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top Header
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
                text = "OUTGOING CALL",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 2.sp,
                    color = RealityEngineAmber
                )
            )
            Spacer(modifier = Modifier.height(12.dp))
            LiveWaveformBar(isActive = true, color = RealityEngineAmber)
        }

        // Center Identity
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
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

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = statusText,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    color = RealityEngineCyan,
                    fontSize = 13.sp,
                    letterSpacing = 1.2.sp
                )
            )

            if (caller != null) {
                Spacer(modifier = Modifier.height(20.dp))
                PrecisionCard {
                    Column {
                        Text(
                            text = "OBJECTIVE",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                color = RealityEngineTextMuted,
                                letterSpacing = 1.2.sp
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = caller.currentTopics.ifBlank { "Direct Consultation" },
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = RealityEngineTextPrimary,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                }
            }
        }

        // End Call Button
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(bottom = 24.dp)
        ) {
            Button(
                onClick = onEndCall,
                modifier = Modifier
                    .size(72.dp)
                    .testTag("end_outgoing_call_button"),
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
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "END CALL",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = RealityEngineTextSecondary
                )
            )
        }
    }
}
