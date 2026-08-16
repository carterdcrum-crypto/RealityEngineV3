package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.MemoryEntity
import com.example.ui.RealityEngineViewModel
import com.example.ui.components.PrecisionCard
import com.example.ui.components.PrecisionSectionHeader
import com.example.ui.components.TacticalHeader
import com.example.ui.theme.RealityEngineAmber
import com.example.ui.theme.RealityEngineBorder
import com.example.ui.theme.RealityEngineCyan
import com.example.ui.theme.RealityEngineDarkBg
import com.example.ui.theme.RealityEngineEmerald
import com.example.ui.theme.RealityEngineSurface
import com.example.ui.theme.RealityEngineSurfaceElevated
import com.example.ui.theme.RealityEngineTextMuted
import com.example.ui.theme.RealityEngineTextPrimary
import com.example.ui.theme.RealityEngineTextSecondary

@Composable
fun MemoryScreen(
    viewModel: RealityEngineViewModel,
    modifier: Modifier = Modifier
) {
    val memories by viewModel.allMemories.collectAsState()
    val filterState by viewModel.memoryFilterState.collectAsState()
    var isAddDialogOpen by remember { mutableStateOf(false) }

    val filterTabs = listOf("ALL", "OBSERVED", "INFERRED", "UNVERIFIED", "CONFIRMED")
    val currentTabIndex = filterTabs.indexOf(filterState ?: "ALL").coerceAtLeast(0)

    val filteredMemories = remember(memories, filterState) {
        if (filterState == null || filterState == "ALL") memories
        else memories.filter { it.state.equals(filterState, ignoreCase = true) }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(RealityEngineDarkBg)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            TacticalHeader(
                title = "REALITY ENGINE",
                subtitle = "STRUCTURED MEMORY REPOSITORY",
                onSettingsClick = { viewModel.openSettings(true) }
            )

            // State Filter Scrollable Tabs
            ScrollableTabRow(
                selectedTabIndex = currentTabIndex,
                containerColor = RealityEngineSurface,
                contentColor = RealityEngineAmber,
                edgePadding = 16.dp,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[currentTabIndex]),
                        color = RealityEngineAmber,
                        height = 2.dp
                    )
                },
                divider = { Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(RealityEngineBorder)) }
            ) {
                filterTabs.forEachIndexed { index, tabName ->
                    Tab(
                        selected = currentTabIndex == index,
                        onClick = { viewModel.setMemoryFilter(tabName) },
                        text = {
                            Text(
                                text = tabName,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = if (currentTabIndex == index) FontWeight.Bold else FontWeight.Normal,
                                color = if (currentTabIndex == index) RealityEngineAmber else RealityEngineTextMuted
                            )
                        }
                    )
                }
            }

            // Memory Items List
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    PrecisionSectionHeader(
                        title = "MEMORY ARTIFACTS",
                        tag = "${filteredMemories.size} ITEMS",
                        tagColor = RealityEngineCyan
                    )
                }

                if (filteredMemories.isEmpty()) {
                    item {
                        PrecisionCard {
                            Text(
                                text = "NO MEMORIES IN THIS CATEGORY",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    color = RealityEngineTextMuted
                                )
                            )
                        }
                    }
                } else {
                    items(filteredMemories, key = { it.id }) { memory ->
                        MemoryItemCard(
                            memory = memory,
                            onConfirm = { viewModel.updateMemoryState(memory.id, "CONFIRMED") },
                            onUnverify = { viewModel.updateMemoryState(memory.id, "UNVERIFIED") },
                            onInfer = { viewModel.updateMemoryState(memory.id, "INFERRED") },
                            onDismiss = { viewModel.dismissMemory(memory.id) },
                            onDelete = { viewModel.deleteMemory(memory) }
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }

        // Add Memory FAB
        FloatingActionButton(
            onClick = { isAddDialogOpen = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("add_memory_fab"),
            containerColor = RealityEngineAmber,
            contentColor = RealityEngineDarkBg
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "Add Memory")
        }

        if (isAddDialogOpen) {
            AddMemoryDialog(
                onDismiss = { isAddDialogOpen = false },
                onSave = { personName, stmt, state ->
                    viewModel.addManualMemory(personName, stmt, state)
                    isAddDialogOpen = false
                }
            )
        }
    }
}

@Composable
private fun MemoryItemCard(
    memory: MemoryEntity,
    onConfirm: () -> Unit,
    onUnverify: () -> Unit,
    onInfer: () -> Unit,
    onDismiss: () -> Unit,
    onDelete: () -> Unit
) {
    val stateColor = when (memory.state.uppercase()) {
        "CONFIRMED" -> RealityEngineEmerald
        "OBSERVED" -> RealityEngineCyan
        "INFERRED" -> RealityEngineAmber
        else -> RealityEngineTextSecondary
    }

    PrecisionCard {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = memory.personName.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = RealityEngineAmber,
                        letterSpacing = 1.sp
                    )
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(stateColor.copy(alpha = 0.15f))
                        .border(1.dp, stateColor.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = memory.state.uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp,
                            color = stateColor
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "\"${memory.statement}\"",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = RealityEngineTextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 20.sp
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Provenance: ${memory.provenance}",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp,
                    color = RealityEngineTextMuted
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            // State Actions Row (CONFIRM, OBSERVE, DISMISS, DELETE)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (memory.state != "CONFIRMED") {
                        StateActionButton(
                            label = "CONFIRM",
                            color = RealityEngineEmerald,
                            onClick = onConfirm
                        )
                    }
                    if (memory.state != "INFERRED") {
                        StateActionButton(
                            label = "INFER",
                            color = RealityEngineAmber,
                            onClick = onInfer
                        )
                    }
                    if (memory.state != "UNVERIFIED") {
                        StateActionButton(
                            label = "UNVERIFY",
                            color = RealityEngineCyan,
                            onClick = onUnverify
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Dismiss", tint = RealityEngineTextMuted, modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = RealityEngineTextMuted, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun StateActionButton(
    label: String,
    color: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(RealityEngineSurfaceElevated)
            .border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        )
    }
}

@Composable
private fun AddMemoryDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit
) {
    var personName by remember { mutableStateOf("Sarah") }
    var statement by remember { mutableStateOf("") }
    var state by remember { mutableStateOf("OBSERVED") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = RealityEngineSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, RealityEngineCyan),
            modifier = Modifier.fillMaxWidth().padding(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "CREATE MEMORY ARTIFACT",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = RealityEngineCyan
                    )
                )

                OutlinedTextField(
                    value = personName,
                    onValueChange = { personName = it },
                    label = { Text("Person Name") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = statement,
                    onValueChange = { statement = it },
                    label = { Text("Memory Statement") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = RealityEngineSurfaceElevated)) {
                        Text("CANCEL", color = RealityEngineTextSecondary)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (statement.isNotBlank()) {
                                onSave(personName.trim(), statement.trim(), state)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = RealityEngineCyan, contentColor = RealityEngineDarkBg)
                    ) {
                        Text("SAVE")
                    }
                }
            }
        }
    }
}
