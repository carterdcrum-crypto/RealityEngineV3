package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "people")
data class PersonEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phoneNumber: String,
    val email: String = "",
    val organization: String = "",
    val relationship: String = "Contact", // e.g., "Project Lead", "Client", "Partner", "Investor"
    val avatarColorHex: String = "#F59E0B",
    val lastContactTimestamp: Long = System.currentTimeMillis(),
    val currentTopics: String = "", // Comma-separated or short summary e.g. "Project X, Moving, Schedule"
    val openQuestions: String = "", // e.g. "Did Project X launch?"
    val recentCommitment: String = "", // e.g. "Send document Friday."
    val notes: String = ""
)

@Entity(tableName = "call_records")
data class CallRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val personId: Long?,
    val personName: String,
    val phoneNumber: String,
    val callType: String, // "INCOMING", "OUTGOING", "MISSED"
    val timestamp: Long = System.currentTimeMillis(),
    val durationSeconds: Int = 0,
    val topic: String = "",
    val summary: String = "",
    val importantStatements: String = "",
    val extractedClaims: String = "",
    val extractedCommitments: String = "",
    val questionsAnswered: String = "",
    val questionsUnresolved: String = "",
    val inconsistencies: String = "",
    val deceptionSummary: String = "",
    val newMemoriesCreated: String = "",
    val recommendedFollowUps: String = "",
    val strategiesUsed: String = "",
    val deceptionAvgScore: Int = 0,
    val transcriptJson: String = ""
)

@Entity(tableName = "memories")
data class MemoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val personId: Long?,
    val personName: String,
    val statement: String, // e.g. "Sarah says she is moving in October."
    val state: String = "OBSERVED", // "OBSERVED", "INFERRED", "UNVERIFIED", "CONFIRMED"
    val provenance: String = "", // e.g. "Call on Aug 15 at 00:04:12"
    val category: String = "General", // "Personal", "Professional", "Schedule", "Financial"
    val timestamp: Long = System.currentTimeMillis(),
    val isDismissed: Boolean = false
)

@Entity(tableName = "claims")
data class ClaimEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val personId: Long?,
    val personName: String,
    val currentStatement: String, // e.g. "I started the project in May."
    val previousStatement: String = "", // e.g. "I started the project in March."
    val context: String = "",
    val inconsistencyConfidence: Int = 0, // 0-100%
    val hasInconsistency: Boolean = false,
    val category: String = "Timeline",
    val timestamp: Long = System.currentTimeMillis(),
    val detectedInCallId: Long? = null
)

@Entity(tableName = "signal_events")
data class SignalEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val callRecordId: Long? = null,
    val personName: String,
    val timestamp: Long = System.currentTimeMillis(),
    val linguisticDistance: Float = 0.2f,
    val factualInconsistency: Float = 0.3f,
    val acousticStress: Float = 0.25f,
    val compositeDeceptionScore: Int = 0,
    val whyExplanation: String = "",
    val contributorsJson: String = ""
)
