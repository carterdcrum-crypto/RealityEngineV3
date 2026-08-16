package com.example.engine

import java.util.UUID

enum class StrategyType(
    val id: String,
    val displayName: String,
    val description: String,
    val purpose: String,
    val isPrimary: Boolean = false
) {
    BONDING(
        id = "bonding",
        displayName = "BONDING",
        description = "Build rapport before probing",
        purpose = "Build rapport, increase conversational openness, establish common ground, reduce unnecessary defensiveness.",
        isPrimary = true
    ),
    COGNITIVE_PROBE(
        id = "cognitive_probe",
        displayName = "COGNITIVE PROBE",
        description = "Clarify claims & timeline",
        purpose = "Ask a targeted follow-up question designed to clarify a claim, expose ambiguity, establish chronology, or request concrete details.",
        isPrimary = true
    ),
    MIRROR(
        id = "mirror",
        displayName = "MIRROR",
        description = "Encourage elaboration",
        purpose = "Reflect relevant language, framing, or conversational style to encourage the person to elaborate.",
        isPrimary = true
    ),
    MIRRORING(
        id = "mirroring",
        displayName = "MIRROR",
        description = "Encourage elaboration",
        purpose = "Reflect relevant language, framing, or conversational style to encourage the person to elaborate.",
        isPrimary = true
    ),
    PIVOT(
        id = "pivot",
        displayName = "PIVOT",
        description = "Redirect the conversation",
        purpose = "Redirect the conversation when the current conversational path is unproductive or when another topic/question is strategically more useful.",
        isPrimary = true
    ),
    CLARIFY(
        id = "clarify",
        displayName = "CLARIFY",
        description = "Request unambiguous definitions",
        purpose = "Request unambiguous definitions and timeline confirmations."
    ),
    VERIFY(
        id = "verify",
        displayName = "VERIFY",
        description = "Cross-check facts against baseline",
        purpose = "Confirm specific dates, metrics, or factual statements with verifiable evidence."
    ),
    TIMELINE(
        id = "timeline",
        displayName = "TIMELINE",
        description = "Establish chronological sequence",
        purpose = "Walk through events in strict chronological order to uncover gaps or inconsistencies."
    ),
    SPECIFY(
        id = "specify",
        displayName = "SPECIFY",
        description = "Request concrete quantifiable metrics",
        purpose = "Drill down from general claims into specific numbers, deliverables, and dates."
    ),
    CONTRAST(
        id = "contrast",
        displayName = "CONTRAST",
        description = "Highlight differences between statements",
        purpose = "Compare current assertions directly with previous commitments or baselines."
    ),
    SUMMARIZE(
        id = "summarize",
        displayName = "SUMMARIZE",
        description = "Recap key agreements to anchor commitments",
        purpose = "Recap key agreements to anchor commitments and verify mutual consensus."
    ),
    OPEN_QUESTION(
        id = "open_question",
        displayName = "OPEN QUESTION",
        description = "Invite broad unconstrained narrative",
        purpose = "Provide wide conversational space to observe spontaneous details and unstructured narrative."
    ),
    CHALLENGE(
        id = "challenge",
        displayName = "CHALLENGE",
        description = "Test assertion with counter-evidence",
        purpose = "Firmly and objectively test the strength of an assertion with known counter-evidence."
    ),
    DE_ESCALATE(
        id = "de_escalate",
        displayName = "DE-ESCALATE",
        description = "Lower emotional intensity",
        purpose = "Lower emotional intensity, acknowledge feelings, and reset constructive dialogue."
    ),
    CLOSE(
        id = "close",
        displayName = "CLOSE",
        description = "Conclude and secure next steps",
        purpose = "Finalize mutual commitments, secure deadlines, and conclude the interaction cleanly."
    ),
    DIRECT_RESPONSE(
        id = "direct_response",
        displayName = "DIRECT RESPONSE",
        description = "Clear factual answer",
        purpose = "Provide clear, concise factual answer while anchoring to strategic priorities."
    )
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

data class StrategyRecommendation(
    val id: String = UUID.randomUUID().toString(),
    val type: StrategyType,
    val name: String = type.displayName,
    val description: String = type.description,
    val purpose: String = type.purpose,
    val recommendationReason: String,
    val suggestedResponse: String,
    val confidence: Int = 80, // 0 to 100
    val tone: ToneType = ToneType.CALM,
    val isPrimaryRecommended: Boolean = false,
    val isSelected: Boolean = false,
    val enabled: Boolean = true
)

data class TranscriptSegment(
    val id: String = UUID.randomUUID().toString(),
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
    val id: String = UUID.randomUUID().toString(),
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
    val strategies: List<StrategyRecommendation> = emptyList(),
    val alternatives: List<StrategyAlternative> = emptyList(),
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
