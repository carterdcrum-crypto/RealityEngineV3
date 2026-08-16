package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.RealityEngineAmber
import com.example.ui.theme.RealityEngineBgWhite05
import com.example.ui.theme.RealityEngineBorder
import com.example.ui.theme.RealityEngineBorderSubtle
import com.example.ui.theme.RealityEngineBorderWhite10
import com.example.ui.theme.RealityEngineCyan
import com.example.ui.theme.RealityEngineDarkBg
import com.example.ui.theme.RealityEngineSurface
import com.example.ui.theme.RealityEngineSurfaceElevated
import com.example.ui.theme.RealityEngineTextMuted
import com.example.ui.theme.RealityEngineTextPrimary
import com.example.ui.theme.RealityEngineTextSecondary

@Composable
fun TacticalHeader(
    title: String = "REALITY ENGINE",
    subtitle: String? = null,
    onSettingsClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(RealityEngineDarkBg)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .background(RealityEngineAmber, CircleShape)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title.uppercase(),
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontFamily = FontFamily.SansSerif,
                        letterSpacing = 2.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = RealityEngineAmber
                    )
                )
            }
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = RealityEngineTextSecondary,
                        fontSize = 12.sp
                    ),
                    modifier = Modifier.padding(start = 15.dp, top = 2.dp)
                )
            }
        }

        if (onSettingsClick != null) {
            IconButton(
                onClick = onSettingsClick,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(RealityEngineBgWhite05)
                    .border(1.dp, RealityEngineBorderWhite10, CircleShape)
                    .testTag("settings_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = RealityEngineTextSecondary,
                    modifier = Modifier.size(17.dp)
                )
            }
        }
    }
}

@Composable
fun PrecisionSectionHeader(
    title: String,
    tag: String? = null,
    tagColor: Color = RealityEngineAmber,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelMedium.copy(
                fontFamily = FontFamily.SansSerif,
                letterSpacing = 2.sp,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                color = RealityEngineTextMuted
            )
        )
        if (!tag.isNullOrBlank()) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(tagColor.copy(alpha = 0.08f))
                    .border(1.dp, tagColor.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = tag.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = tagColor
                    )
                )
            }
        }
    }
}

@Composable
fun PrecisionCard(
    modifier: Modifier = Modifier,
    borderColor: Color = RealityEngineBorder,
    backgroundColor: Color = RealityEngineSurface,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        content()
    }
}

@Composable
fun SignalSliderRow(
    label: String,
    position: Float, // 0.0 to 1.0
    accentColor: Color = RealityEngineAmber,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = FontFamily.SansSerif,
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium,
                color = RealityEngineTextSecondary.copy(alpha = 0.7f),
                letterSpacing = 1.2.sp
            ),
            modifier = Modifier.width(76.dp)
        )

        // Clean Modern Track & Progress Fill (Matches Elegant Dark layout)
        Box(
            modifier = Modifier
                .weight(1f)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(RealityEngineBgWhite05)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(position.coerceIn(0.02f, 1.0f))
                    .background(accentColor, RoundedCornerShape(2.dp))
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Text(
            text = "${(position * 100).toInt()}%",
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                color = RealityEngineTextSecondary
            ),
            modifier = Modifier.width(32.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.End
        )
    }
}

@Composable
fun LiveWaveformBar(
    isActive: Boolean = true,
    color: Color = RealityEngineCyan,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "wave")
    val anim1 by transition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar1"
    )
    val anim2 by transition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(550, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar2"
    )
    val anim3 by transition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(350, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar3"
    )

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val heights = if (isActive) listOf(anim1, anim2, anim3, anim1 * 0.7f) else listOf(0.2f, 0.2f, 0.2f, 0.2f)
        heights.forEach { h ->
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height((16 * h).dp.coerceAtLeast(4.dp))
                    .background(color, RoundedCornerShape(1.5.dp))
            )
        }
    }
}

