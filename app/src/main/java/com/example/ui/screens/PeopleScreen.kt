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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
import com.example.data.model.PersonEntity
import com.example.ui.RealityEngineViewModel
import com.example.ui.components.PrecisionCard
import com.example.ui.components.PrecisionSectionHeader
import com.example.ui.components.TacticalHeader
import com.example.ui.theme.RealityEngineAmber
import com.example.ui.theme.RealityEngineBorder
import com.example.ui.theme.RealityEngineCallGreen
import com.example.ui.theme.RealityEngineCyan
import com.example.ui.theme.RealityEngineDarkBg
import com.example.ui.theme.RealityEngineSurface
import com.example.ui.theme.RealityEngineSurfaceElevated
import com.example.ui.theme.RealityEngineTextMuted
import com.example.ui.theme.RealityEngineTextPrimary
import com.example.ui.theme.RealityEngineTextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PeopleScreen(
    viewModel: RealityEngineViewModel,
    modifier: Modifier = Modifier
) {
    val people by viewModel.people.collectAsState()
    val selectedPerson by viewModel.selectedPerson.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var isAddDialogOpen by remember { mutableStateOf(false) }

    val filteredPeople = remember(people, searchQuery) {
        if (searchQuery.isBlank()) people
        else people.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
                    it.currentTopics.contains(searchQuery, ignoreCase = true) ||
                    it.organization.contains(searchQuery, ignoreCase = true)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(RealityEngineDarkBg)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            TacticalHeader(
                title = "REALITY ENGINE",
                subtitle = "PEOPLE DATABASE & INTELLIGENCE",
                onSettingsClick = { viewModel.openSettings(true) }
            )

            // Search Bar
            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            "Search contacts, topics, commitments...",
                            color = RealityEngineTextMuted,
                            fontSize = 12.sp
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = RealityEngineTextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    modifier = Modifier.fillMaxWidth().testTag("people_search_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = RealityEngineAmber,
                        unfocusedBorderColor = RealityEngineBorder,
                        focusedContainerColor = RealityEngineSurface,
                        unfocusedContainerColor = RealityEngineSurface,
                        focusedTextColor = RealityEngineTextPrimary,
                        unfocusedTextColor = RealityEngineTextPrimary
                    ),
                    singleLine = true
                )
            }

            // People List
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    PrecisionSectionHeader(
                        title = "KNOWN PERSONS",
                        tag = "${filteredPeople.size} PROFILES",
                        tagColor = RealityEngineCyan
                    )
                }

                items(filteredPeople, key = { it.id }) { person ->
                    PersonCard(
                        person = person,
                        onClick = { viewModel.selectPerson(person) },
                        onCall = {
                            viewModel.startOutgoingCall(person.phoneNumber, person)
                        }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }

        // Add Person FAB
        FloatingActionButton(
            onClick = { isAddDialogOpen = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("add_person_fab"),
            containerColor = RealityEngineAmber,
            contentColor = RealityEngineDarkBg
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "Add Contact")
        }

        // Selected Person Detail Sheet
        if (selectedPerson != null) {
            PersonDetailDialog(
                person = selectedPerson!!,
                onDismiss = { viewModel.selectPerson(null) },
                onCall = {
                    val p = selectedPerson!!
                    viewModel.selectPerson(null)
                    viewModel.startOutgoingCall(p.phoneNumber, p)
                },
                onDelete = {
                    viewModel.deletePerson(selectedPerson!!)
                }
            )
        }

        // Add Person Dialog
        if (isAddDialogOpen) {
            AddPersonDialog(
                onDismiss = { isAddDialogOpen = false },
                onSave = { newPerson ->
                    viewModel.savePerson(newPerson)
                    isAddDialogOpen = false
                }
            )
        }
    }
}

@Composable
private fun PersonCard(
    person: PersonEntity,
    onClick: () -> Unit,
    onCall: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM d", Locale.US) }
    val lastContactStr = dateFormat.format(Date(person.lastContactTimestamp))

    PrecisionCard(
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(RealityEngineSurfaceElevated)
                            .border(1.dp, RealityEngineAmber, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = person.name.take(2).uppercase(),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = RealityEngineAmber
                            )
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = person.name.uppercase(),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = RealityEngineTextPrimary,
                                letterSpacing = 0.5.sp
                            )
                        )
                        Text(
                            text = "${person.relationship} · ${person.organization}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = RealityEngineTextSecondary,
                                fontSize = 11.sp
                            )
                        )
                    }
                }

                IconButton(
                    onClick = onCall,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(RealityEngineCallGreen.copy(alpha = 0.15f))
                        .border(1.dp, RealityEngineCallGreen, CircleShape)
                        .testTag("call_person_${person.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = "Call ${person.name}",
                        tint = RealityEngineCallGreen,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Topics & Commitments
            if (person.currentTopics.isNotBlank()) {
                Row(modifier = Modifier.padding(vertical = 2.dp)) {
                    Text(
                        text = "TOPICS: ",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            color = RealityEngineTextMuted,
                            fontSize = 10.sp
                        )
                    )
                    Text(
                        text = person.currentTopics,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = RealityEngineCyan,
                            fontSize = 11.sp
                        )
                    )
                }
            }

            if (person.recentCommitment.isNotBlank()) {
                Row(modifier = Modifier.padding(vertical = 2.dp)) {
                    Text(
                        text = "COMMITMENT: ",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            color = RealityEngineTextMuted,
                            fontSize = 10.sp
                        )
                    )
                    Text(
                        text = person.recentCommitment,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = RealityEngineAmber,
                            fontSize = 11.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Last contact: $lastContactStr",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    color = RealityEngineTextMuted,
                    fontSize = 9.sp
                )
            )
        }
    }
}

@Composable
private fun PersonDetailDialog(
    person: PersonEntity,
    onDismiss: () -> Unit,
    onCall: () -> Unit,
    onDelete: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = RealityEngineSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, RealityEngineBorder),
            modifier = Modifier.fillMaxWidth().padding(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = person.name.uppercase(),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = RealityEngineAmber
                        )
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = RealityEngineTextSecondary)
                    }
                }

                Text(
                    text = "${person.relationship} · ${person.organization}",
                    style = MaterialTheme.typography.bodySmall.copy(color = RealityEngineTextSecondary)
                )
                Text(
                    text = person.phoneNumber,
                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace, color = RealityEngineCyan)
                )

                Spacer(modifier = Modifier.height(12.dp))
                PrecisionSectionHeader(title = "CURRENT CONTEXT")

                DetailItem(label = "CURRENT TOPICS", value = person.currentTopics)
                DetailItem(label = "OPEN QUESTION", value = person.openQuestions.ifBlank { "None pending" })
                DetailItem(label = "RECENT COMMITMENT", value = person.recentCommitment.ifBlank { "None recorded" })
                DetailItem(label = "NOTES", value = person.notes.ifBlank { "No notes attached" })

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onCall,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = RealityEngineCallGreen)
                    ) {
                        Icon(imageVector = Icons.Default.Call, contentDescription = "Call", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("CALL")
                    }

                    Button(
                        onClick = {
                            onDelete()
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = RealityEngineSurfaceElevated)
                    ) {
                        Text("DELETE", color = RealityEngineTextMuted)
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailItem(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = FontFamily.Monospace,
                color = RealityEngineTextMuted,
                fontSize = 9.sp
            )
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(
                color = RealityEngineTextPrimary,
                fontSize = 12.sp
            )
        )
    }
}

@Composable
private fun AddPersonDialog(
    onDismiss: () -> Unit,
    onSave: (PersonEntity) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var org by remember { mutableStateOf("") }
    var rel by remember { mutableStateOf("Client") }
    var topics by remember { mutableStateOf("") }
    var questions by remember { mutableStateOf("") }
    var commitments by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = RealityEngineSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, RealityEngineAmber),
            modifier = Modifier.fillMaxWidth().padding(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "NEW PERSON PROFILE",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = RealityEngineAmber
                    )
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name (e.g. Sarah)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone (+1 415...)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = topics,
                    onValueChange = { topics = it },
                    label = { Text("Topics (e.g. Project X, Schedule)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = commitments,
                    onValueChange = { commitments = it },
                    label = { Text("Recent Commitment") },
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
                            if (name.isNotBlank()) {
                                onSave(
                                    PersonEntity(
                                        name = name.trim(),
                                        phoneNumber = phone.trim(),
                                        organization = org.trim(),
                                        relationship = rel.trim(),
                                        currentTopics = topics.trim(),
                                        openQuestions = questions.trim(),
                                        recentCommitment = commitments.trim()
                                    )
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = RealityEngineAmber, contentColor = RealityEngineDarkBg)
                    ) {
                        Text("SAVE PROFILE")
                    }
                }
            }
        }
    }
}
