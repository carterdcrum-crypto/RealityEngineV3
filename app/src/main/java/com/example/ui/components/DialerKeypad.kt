package com.example.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.RealityEngineAmber
import com.example.ui.theme.RealityEngineAmberGlow
import com.example.ui.theme.RealityEngineBorder
import com.example.ui.theme.RealityEngineBorderSubtle
import com.example.ui.theme.RealityEngineDarkBg
import com.example.ui.theme.RealityEngineSurface
import com.example.ui.theme.RealityEngineSurfaceElevated
import com.example.ui.theme.RealityEngineTextMuted
import com.example.ui.theme.RealityEngineTextPrimary
import com.example.ui.theme.RealityEngineTextSecondary

/**
 * Data structure representing each key in the 3x4 dialer grid.
 */
data class KeypadKeyItem(
    val digit: String,
    val letters: String = "",
    val longPressDigit: String? = null
)

/**
 * Standard 3x4 Grid Layout for Reality Engine Telephony.
 * Row 1: 1 (Voicemail/empty), 2 (ABC), 3 (DEF)
 * Row 2: 4 (GHI), 5 (JKL), 6 (MNO)
 * Row 3: 7 (PQRS), 8 (TUV), 9 (WXYZ)
 * Row 4: * (Star), 0 (+ with long-press), # (Pound/Hash)
 */
val STANDARD_DIALER_GRID: List<List<KeypadKeyItem>> = listOf(
    listOf(
        KeypadKeyItem("1", ""),
        KeypadKeyItem("2", "ABC"),
        KeypadKeyItem("3", "DEF")
    ),
    listOf(
        KeypadKeyItem("4", "GHI"),
        KeypadKeyItem("5", "JKL"),
        KeypadKeyItem("6", "MNO")
    ),
    listOf(
        KeypadKeyItem("7", "PQRS"),
        KeypadKeyItem("8", "TUV"),
        KeypadKeyItem("9", "WXYZ")
    ),
    listOf(
        KeypadKeyItem("*", ""),
        KeypadKeyItem("0", "+", longPressDigit = "+"),
        KeypadKeyItem("#", "")
    )
)

/**
 * Premium 3x4 Numerical Keypad Grid for Reality Engine.
 */
@Composable
fun RealityEngineDialerGrid(
    onDigitClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    keySize: Dp = 74.dp,
    spacing: Dp = 14.dp,
    gridData: List<List<KeypadKeyItem>> = STANDARD_DIALER_GRID
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(spacing),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        gridData.forEach { rowKeys ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                rowKeys.forEach { keyItem ->
                    RealityEngineDialerKey(
                        keyItem = keyItem,
                        onClick = { onDigitClick(keyItem.digit) },
                        onLongClick = {
                            keyItem.longPressDigit?.let { onDigitClick(it) } ?: onDigitClick(keyItem.digit)
                        },
                        buttonSize = keySize
                    )
                }
            }
        }
    }
}

/**
 * Individual Custom-Styled Keypad Key matching the Reality Engine Dark Palette.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RealityEngineDialerKey(
    keyItem: KeypadKeyItem,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    buttonSize: Dp = 74.dp
) {
    val view = LocalView.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Tactile press animation
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.93f else 1.0f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
        label = "keypad_press_scale"
    )

    // Border and surface gradient styling
    val borderBrush = if (isPressed) {
        Brush.linearGradient(
            colors = listOf(RealityEngineAmber, RealityEngineAmberGlow)
        )
    } else {
        Brush.linearGradient(
            colors = listOf(RealityEngineBorderSubtle, RealityEngineBorder)
        )
    }

    val backgroundBrush = if (isPressed) {
        Brush.verticalGradient(
            colors = listOf(RealityEngineSurfaceElevated, RealityEngineDarkBg)
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(RealityEngineSurfaceElevated, RealityEngineSurface)
        )
    }

    Box(
        modifier = modifier
            .size(buttonSize)
            .scale(scale)
            .clip(CircleShape)
            .background(backgroundBrush)
            .border(
                width = if (isPressed) 1.5.dp else 1.dp,
                brush = borderBrush,
                shape = CircleShape
            )
            .combinedClickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true, color = RealityEngineAmber),
                onClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    onClick()
                },
                onLongClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                    onLongClick()
                }
            )
            .testTag("keypad_digit_${keyItem.digit}"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Main Digit
            Text(
                text = keyItem.digit,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Medium,
                    fontSize = if (keyItem.digit == "*" || keyItem.digit == "#") 26.sp else 28.sp,
                    color = if (isPressed) RealityEngineAmber else RealityEngineTextPrimary,
                    lineHeight = 28.sp
                )
            )

            // Letters / Symbols Sub-label
            if (keyItem.letters.isNotEmpty()) {
                Spacer(modifier = Modifier.height(1.dp))
                Text(
                    text = keyItem.letters,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = if (keyItem.letters == "+") 11.sp else 9.sp,
                        fontWeight = if (keyItem.letters == "+") FontWeight.Bold else FontWeight.SemiBold,
                        letterSpacing = if (keyItem.letters == "+") 0.sp else 1.5.sp,
                        color = if (keyItem.letters == "+") RealityEngineAmber else RealityEngineTextMuted
                    )
                )
            }
        }
    }
}
