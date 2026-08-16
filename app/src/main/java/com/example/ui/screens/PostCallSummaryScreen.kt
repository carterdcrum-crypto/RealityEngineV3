package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.CallSummaryPayload
import com.example.ui.components.PrecisionCard
import com.example.ui.components.PrecisionSectionHeader
import com.example.ui.theme.RealityEngineAmber
import com.example.ui.theme.RealityEngineBorder
import com.example.ui.theme.RealityEngineCyan
import com.example.ui.theme.RealityEngineDarkBg
import com.example.ui.theme.RealityEngineSurface
import com.example.ui.theme.RealityEngineTextMuted
import com.example.ui.theme.RealityEngineTextPrimary
import com.example.ui.theme.RealityEngineTextSecondary

@Composable
fun PostCallSummaryScreen(
    initialSummary: CallSummaryPayload,
    onSave: (CallSummaryPayload) -> Unit,
    onDiscard: () -> Unit,
    modifier: Modifier = Modifier
) {
    var topics by remember { mutableStateOf(initialSummary.topics) }
    var importantStatements by remember { mutableStateOf(initialSummary.importantStatements) }
    var claims by remember { mutableStateOf(initialSummary.claims) }
    var commitments by remember { mutableStateOf(initialSummary.commitments) }
    var questionsAnswered by remember { mutableStateOf(initialSummary.questionsAnswered) }
    var questionsUnresolved by remember { mutableStateOf(initialSummary.questionsUnresolved) }
    var inconsistencies by remember { mutableStateOf(initialSummary.potentialInconsistencies) }
    var deceptionSummary by remember { mutableStateOf(initialSummary.deceptionSignalsSummary) }
    var newMemories by remember { mutableStateOf(initialSummary.newMemoriesCreated) }
    var followUps by remember { mutableStateOf(initialSummary.recommendedFollowUps) }
    var strategiesUsed by remember { mutableStateOf(initialSummary.strategiesUsed) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(RealityEngineDarkBg)
            .padding(16.dp)
    ) {
        // Tactical Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
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
                        letterSpacing = 2.sp,
                        fontWeight = FontWeight.Bold,
                        color = RealityEngineTextPrimary
                    )
                )
            }
            Text(
                text = "CALL SUMMARY",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    color = RealityEngineCyan,
                    fontWeight = FontWeight.Bold
                )
            )
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                PrecisionCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "PARTICIPANTS",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    color = RealityEngineTextMuted
                                )
                            )
                            Text(
                                text = initialSummary.participants,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = RealityEngineTextPrimary
                                )
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "DURATION",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    color = RealityEngineTextMuted
                                )
                            )
                            Text(
                                text = initialSummary.durationFormatted,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = RealityEngineAmber
                                )
                            )
                        }
                    }
                }
            }

            item {
                EditableSummaryField(label = "TOPICS", value = topics, onValueChange = { topics = it })
            }

            item {
                EditableSummaryField(label = "IMPORTANT STATEMENTS", value = importantStatements, onValueChange = { importantStatements = it })
            }

            item {
                EditableSummaryField(label = "EXTRACTED CLAIMS", value = claims, onValueChange = { claims = it })
            }

            item {
                EditableSummaryField(label = "COMMITMENTS", value = commitments, onValueChange = { commitments = it })
            }

            item {
                EditableSummaryField(label = "QUESTIONS ANSWERED", value = questionsAnswered, onValueChange = { questionsAnswered = it })
            }

            item {
                EditableSummaryField(label = "QUESTIONS UNRESOLVED", value = questionsUnresolved, onValueChange = { questionsUnresolved = it })
            }

            item {
                EditableSummaryField(label = "POTENTIAL INCONSISTENCIES", value = inconsistencies, onValueChange = { inconsistencies = it })
            }

            item {
                EditableSummaryField(label = "DECEPTION-RELATED SIGNALS", value = deceptionSummary, onValueChange = { deceptionSummary = it })
            }

            item {
                EditableSummaryField(label = "NEW MEMORIES", value = newMemories, onValueChange = { newMemories = it })
            }

            item {
                EditableSummaryField(label = "RECOMMENDED FOLLOW-UPS", value = followUps, onValueChange = { followUps = it })
            }

            item {
                EditableSummaryField(label = "STRATEGIES USED", value = strategiesUsed, onValueChange = { strategiesUsed = it })
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // Action Buttons: SAVE SUMMARY / DISCARD
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onDiscard,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("discard_summary_button"),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = RealityEngineTextSecondary
                )
            ) {
                Icon(imageVector = Icons.Default.Close, contentDescription = "Discard", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "DISCARD",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            }

            Button(
                onClick = {
                    onSave(
                        initialSummary.copy(
                            topics = topics,
                            importantStatements = importantStatements,
                            claims = claims,
                            commitments = commitments,
                            questionsAnswered = questionsAnswered,
                            questionsUnresolved = questionsUnresolved,
                            potentialInconsistencies = inconsistencies,
                            deceptionSignalsSummary = deceptionSummary,
                            newMemoriesCreated = newMemories,
                            recommendedFollowUps = followUps,
                            strategiesUsed = strategiesUsed
                        )
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("save_summary_button"),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = RealityEngineAmber,
                    contentColor = RealityEngineDarkBg
                )
            ) {
                Icon(imageVector = Icons.Default.Check, contentDescription = "Save", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "SAVE SUMMARY",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun EditableSummaryField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit
) {
    PrecisionCard {
        Column(modifier = Modifier.fillMaxWidth()) {
            PrecisionSectionHeader(title = label)
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = RealityEngineTextPrimary,
                    fontSize = 13.sp
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = RealityEngineAmber,
                    unfocusedBorderColor = RealityEngineBorder,
                    focusedContainerColor = RealityEngineSurface,
                    unfocusedContainerColor = RealityEngineSurface
                )
            )
        }
    }
}
