package com.example.engine

enum class StrategyType(val displayName: String, val description: String) {
    COGNITIVE_PROBE("COGNITIVE PROBE", "Neutral question uncovering hidden assumptions or inconsistencies"),
    MIRRORING("MIRRORING", "Reflect back the exact last keywords to build rapport and prompt detail"),
    PIVOT("PIVOT", "Shift the conversation focus toward the primary strategic objective"),
    BONDING("BONDING", "Establish emotional alignment and shared context"),
    CLARIFY("CLARIFY", "Request unambiguous definitions and timeline confirmations"),
    CONFRONT("CONFRONT", "Firmly and objectively highlight factual discrepancies"),
    VALIDATE("VALIDATE", "Acknowledge feelings and legitimacy to lower resistance"),
    CHALLENGE("CHALLENGE", "Test strength of assertion with counter-evidence"),
    DE_ESCALATE("DE-ESCALATE", "Lower emotional intensity and reset constructive dialogue"),
    PROBE("PROBE", "Inquire further into specific rationale"),
    FOLLOW_UP("FOLLOW-UP", "Verify previously agreed action items and commitments"),
    SUMMARIZE("SUMMARIZE", "Recap key agreements to anchor commitments"),
    PAUSE("PAUSE", "Deliberate strategic silence to prompt other party to elaborate"),
    DIRECT_RESPONSE("DIRECT RESPONSE", "Provide clear, concise factual answer"),
    ASSERTIVE("ASSERTIVE", "Direct and firm strategic positioning")
}

enum class ToneType(val displayName: String) {
    CALM_CURIOUS("CALM · CURIOUS"),
    CALM("CALM"),
    CURIOUS("CURIOUS"),
    WARM("WARM"),
    NEUTRAL("NEUTRAL"),
    DIRECT("DIRECT"),
    ASSERTIVE("ASSERTIVE"),
    DIPLOMATIC("DIPLOMATIC"),
    EMPATHETIC("EMPATHETIC"),
    CAUTIOUS("CAUTIOUS"),
    SKEPTICAL("SKEPTICAL"),
    PROFESSIONAL("PROFESSIONAL"),
    URGENT("URGENT"),
    DE_ESCALATING("DE-ESCALATING")
}

data class TranscriptSegment(
    val id: String = java.util.UUID.randomUUID().toString(),
    val speaker: Speaker,
    val speakerName: String,
    val text: String,
    val timestamp: String, // e.g. "00:12:35"
    val isFinal: Boolean = true,
    val linguisticDistance: Float = 0.2f,
    val stressLevel: Float = 0.2f
)

enum class Speaker {
    YOU, OTHER
}

data class StrategyAlternative(
    val strategy: StrategyType,
    val suggestedResponse: String,
    val tone: ToneType = ToneType.CALM
)

data class DeceptionContributor(
    val label: String, // e.g. "Linguistic distancing", "Statement inconsistency", "Uncertainty", "Context mismatch"
    val scoreDelta: Int // e.g. +18, +31, +12, +12
)

data class DeceptionSignalState(
    val score: Int = 18, // 0 - 100%
    val isElevated: Boolean = false,
    val label: String = "EXPERIMENTAL SIGNAL",
    val disclaimer: String = "REQUIRES HUMAN INTERPRETATION",
    val contributors: List<DeceptionContributor> = emptyList(),
    val whyExplanation: String = "Linguistic markers and timeline statements are consistent with baseline."
)

data class LiveSignalMeters(
    val linguisticPosition: Float = 0.2f, // 0.0 to 1.0 (●────────)
    val factualPosition: Float = 0.7f,    // 0.0 to 1.0 (─────●───)
    val acousticPosition: Float = 0.4f    // 0.0 to 1.0 (───●─────)
)

data class MemoryCandidateAlert(
    val id: String = java.util.UUID.randomUUID().toString(),
    val statement: String, // e.g. "Sarah says she is moving in October."
    val suggestedState: String = "OBSERVED",
    val provenance: String = ""
)

data class InconsistencyAlert(
    val previousStatement: String, // e.g. "March"
    val currentStatement: String,  // e.g. "May"
    val confidence: Int = 87,
    val context: String = "Project kickoff timeline mismatch"
)

data class CopilotAnalysisResult(
    val recommendedStrategy: StrategyType,
    val tone: ToneType,
    val confidence: Int, // e.g. 84
    val suggestedResponse: String,
    val reason: String,
    val alternatives: List<StrategyAlternative>,
    val liveSignals: LiveSignalMeters,
    val deceptionSignal: DeceptionSignalState,
    val inconsistencyAlert: InconsistencyAlert? = null,
    val memoryCandidateAlert: MemoryCandidateAlert? = null
)

data class CallSummaryPayload(
    val participants: String,
    val durationFormatted: String,
    val durationSeconds: Int,
    val topics: String,
    val importantStatements: String,
    val claims: String,
    val commitments: String,
    val questionsAnswered: String,
    val questionsUnresolved: String,
    val potentialInconsistencies: String,
    val deceptionSignalsSummary: String,
    val newMemoriesCreated: String,
    val recommendedFollowUps: String,
    val strategiesUsed: String
)
