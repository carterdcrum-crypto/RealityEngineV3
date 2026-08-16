package com.example.engine

import com.example.data.model.ClaimEntity
import com.example.data.model.MemoryEntity
import com.example.data.model.PersonEntity
import java.util.Locale

/**
 * Strategy Registry providing extensible strategy definitions and capabilities.
 */
object StrategyRegistry {
    private val registeredStrategies = mutableMapOf<StrategyType, StrategyMetadata>()

    data class StrategyMetadata(
        val type: StrategyType,
        val defaultTone: ToneType,
        val baseConfidence: Int,
        val examplePrompt: String
    )

    init {
        // Register primary strategies
        register(StrategyType.BONDING, ToneType.WARM, 85, "Build rapport, establish common ground, reduce defensiveness.")
        register(StrategyType.COGNITIVE_PROBE, ToneType.CURIOUS, 88, "Ask targeted clarifying questions regarding timeline, facts, or specifics.")
        register(StrategyType.MIRROR, ToneType.CALM, 82, "Reflect the speaker's key words with questioning intonation.")
        register(StrategyType.MIRRORING, ToneType.CALM, 82, "Reflect the speaker's key words with questioning intonation.")
        register(StrategyType.PIVOT, ToneType.DIPLOMATIC, 80, "Gracefully redirect toward the strategic objective.")

        // Register extensible future strategies
        register(StrategyType.CLARIFY, ToneType.CALM_CURIOUS, 84, "Request unambiguous definitions and timeline confirmations.")
        register(StrategyType.VERIFY, ToneType.DIRECT, 78, "Cross-check facts against baseline records.")
        register(StrategyType.TIMELINE, ToneType.CURIOUS, 82, "Establish chronological sequence of events.")
        register(StrategyType.SPECIFY, ToneType.DIRECT, 80, "Request concrete quantifiable metrics and figures.")
        register(StrategyType.CONTRAST, ToneType.ASSERTIVE, 76, "Highlight differences between current and previous assertions.")
        register(StrategyType.SUMMARIZE, ToneType.PROFESSIONAL, 85, "Anchor agreements and recap consensus.")
        register(StrategyType.OPEN_QUESTION, ToneType.CURIOUS, 82, "Invite broad unconstrained narrative.")
        register(StrategyType.CHALLENGE, ToneType.ASSERTIVE, 70, "Test assertion with known counter-evidence.")
        register(StrategyType.DE_ESCALATE, ToneType.DE_ESCALATING, 88, "Lower emotional intensity and reset constructive dialogue.")
        register(StrategyType.CLOSE, ToneType.PROFESSIONAL, 85, "Finalize commitments and conclude the call.")
        register(StrategyType.DIRECT_RESPONSE, ToneType.DIRECT, 80, "Clear factual answer.")
    }

    fun register(type: StrategyType, defaultTone: ToneType, baseConfidence: Int, examplePrompt: String) {
        registeredStrategies[type] = StrategyMetadata(type, defaultTone, baseConfidence, examplePrompt)
    }

    fun getMetadata(type: StrategyType): StrategyMetadata? = registeredStrategies[type]

    fun getAllPrimaryTypes(): List<StrategyType> = listOf(
        StrategyType.BONDING,
        StrategyType.COGNITIVE_PROBE,
        StrategyType.MIRROR,
        StrategyType.PIVOT
    )

    fun getAllAvailableTypes(): List<StrategyType> = registeredStrategies.keys.toList()
}

/**
 * Dynamic Strategy Engine that generates context-driven recommendations
 * for all primary strategies and context-triggered extensible strategies.
 */
class StrategyEngine {

    /**
     * Dynamically generates the complete recommended strategy set based on
     * the real conversation history, recent signals, detected inconsistencies, and caller profile.
     */
    fun evaluateStrategies(
        caller: PersonEntity?,
        transcriptHistory: List<TranscriptSegment>,
        latestUtterance: TranscriptSegment?,
        knownClaims: List<ClaimEntity>,
        knownMemories: List<MemoryEntity>,
        objective: String,
        detectedInconsistency: InconsistencyAlert? = null,
        deceptionScore: Int = 18,
        previousStrategy: StrategyType? = null
    ): List<StrategyRecommendation> {
        val lastSpeakerIsOther = latestUtterance?.speaker == Speaker.OTHER || latestUtterance == null
        val rawText = latestUtterance?.text?.trim() ?: ""
        val textLower = rawText.lowercase(Locale.US)

        val callerName = caller?.name ?: "the other party"
        val cleanObjective = objective.ifBlank { "main strategic goal" }

        // Context cues extraction
        val words = rawText.split("\\s+".toRegex()).filter { it.isNotBlank() }
        val lastWords = if (words.size >= 4) words.takeLast(4).joinToString(" ") else if (words.isNotEmpty()) words.joinToString(" ") else "that timeline"
        val cleanedMirrorPhrase = lastWords.trimEnd('.', ',', '!', '?', ';', ':')

        val isQuestion = rawText.endsWith("?") ||
                textLower.startsWith("what") ||
                textLower.startsWith("how") ||
                textLower.startsWith("why") ||
                textLower.startsWith("when") ||
                textLower.startsWith("who") ||
                textLower.startsWith("could you") ||
                textLower.startsWith("can you")

        val containsHedge = listOf("maybe", "i think", "probably", "sort of", "kind of", "not sure", "honestly", "to be honest", "as far as i know")
            .any { textLower.contains(it) }

        val containsNegation = listOf("never", "didn't", "did not", "wasn't", "was not", "cannot", "can't", "won't", "no way", "impossible")
            .any { textLower.contains(it) }

        val containsTimeRef = listOf("yesterday", "today", "tomorrow", "next week", "last month", "friday", "monday", "october", "march", "quarter", "deadline", "schedule", "timeline")
            .any { textLower.contains(it) }

        val containsEmotionStress = listOf("frustrated", "stressed", "rushed", "tough", "difficult", "overwhelmed", "crazy", "busy", "hectic")
            .any { textLower.contains(it) }

        val containsCommitment = listOf("will do", "promise", "guarantee", "send you", "follow up", "finish", "deliver", "ready by")
            .any { textLower.contains(it) }

        // Determine top primary strategy based on real conversational dynamics
        val primaryType: StrategyType = when {
            detectedInconsistency != null -> StrategyType.COGNITIVE_PROBE
            containsEmotionStress || deceptionScore > 50 -> StrategyType.BONDING
            isQuestion -> StrategyType.PIVOT
            containsTimeRef || containsHedge -> StrategyType.COGNITIVE_PROBE
            rawText.length > 20 && !containsHedge -> StrategyType.MIRROR
            transcriptHistory.size > 8 && previousStrategy == StrategyType.MIRROR -> StrategyType.PIVOT
            else -> StrategyType.COGNITIVE_PROBE
        }

        val recommendations = mutableListOf<StrategyRecommendation>()

        // 1. BONDING Strategy
        val bondingSuggested = when {
            containsEmotionStress -> "I completely understand how demanding that has been on your end. We're on the same team here."
            rawText.isNotBlank() -> "I appreciate you walking through that openly. Finding a seamless way forward for both of us is the top priority."
            caller?.organization?.isNotBlank() == true -> "Great to connect today. Really appreciate the collaborative work ${caller.organization} has been putting into this."
            else -> "I appreciate your perspective on this. Let's make sure we find an outcome that works smoothly on your end."
        }
        val bondingReason = when {
            containsEmotionStress -> "Speaker indicated stress or friction; building rapport disarms defensiveness and restores trust."
            deceptionScore > 40 -> "Elevated tension detected; establishing common ground prevents speaker from withdrawing."
            else -> "Establishes conversational safety and goodwill before diving into analytical scrutiny."
        }
        val bondingConfidence = if (containsEmotionStress || deceptionScore > 40) 92 else 82

        recommendations.add(
            StrategyRecommendation(
                type = StrategyType.BONDING,
                name = StrategyType.BONDING.displayName,
                description = StrategyType.BONDING.description,
                purpose = StrategyType.BONDING.purpose,
                recommendationReason = bondingReason,
                suggestedResponse = bondingSuggested,
                confidence = bondingConfidence,
                tone = ToneType.WARM,
                isPrimaryRecommended = primaryType == StrategyType.BONDING,
                enabled = true
            )
        )

        // 2. COGNITIVE PROBE Strategy
        val probeSuggested = when {
            detectedInconsistency != null -> "Can you walk me through the steps between ${detectedInconsistency.previousStatement} and ${detectedInconsistency.currentStatement}?"
            containsTimeRef -> "To make sure our timeline aligns, what specific sequence of events leads up to $cleanedMirrorPhrase?"
            containsHedge -> "When you mentioned \"$cleanedMirrorPhrase\", what specific constraints or variables are you considering?"
            rawText.isNotBlank() -> "Could you break down the concrete details behind $cleanedMirrorPhrase so we have exact clarity?"
            else -> "Could you walk me through the key chronological milestones for $cleanObjective?"
        }
        val probeReason = when {
            detectedInconsistency != null -> "Factual variance identified; a neutral chronological probe tests the discrepancy without confrontation."
            containsHedge -> "Speaker used qualifiers/hedges; probing uncovers underlying assumptions."
            containsTimeRef -> "Timeline referenced; mapping the chronology prevents future ambiguity."
            else -> "Targets concrete details and establishes unambiguous baseline facts."
        }
        val probeConfidence = if (detectedInconsistency != null || containsTimeRef || containsHedge) 94 else 86

        recommendations.add(
            StrategyRecommendation(
                type = StrategyType.COGNITIVE_PROBE,
                name = StrategyType.COGNITIVE_PROBE.displayName,
                description = StrategyType.COGNITIVE_PROBE.description,
                purpose = StrategyType.COGNITIVE_PROBE.purpose,
                recommendationReason = probeReason,
                suggestedResponse = probeSuggested,
                confidence = probeConfidence,
                tone = ToneType.CALM_CURIOUS,
                isPrimaryRecommended = primaryType == StrategyType.COGNITIVE_PROBE,
                enabled = true
            )
        )

        // 3. MIRROR Strategy
        val mirrorSuggested = if (rawText.isNotBlank()) {
            "\"$cleanedMirrorPhrase?\""
        } else {
            "\"The primary schedule?\""
        }
        val mirrorReason = "Reflecting the speaker's exact phrase encourages automatic elaboration and psychological flow without putting them on the defensive."
        val mirrorConfidence = if (rawText.length > 20) 88 else 78

        recommendations.add(
            StrategyRecommendation(
                type = StrategyType.MIRROR,
                name = StrategyType.MIRROR.displayName,
                description = StrategyType.MIRROR.description,
                purpose = StrategyType.MIRROR.purpose,
                recommendationReason = mirrorReason,
                suggestedResponse = mirrorSuggested,
                confidence = mirrorConfidence,
                tone = ToneType.CALM,
                isPrimaryRecommended = primaryType == StrategyType.MIRROR,
                enabled = true
            )
        )

        // 4. PIVOT Strategy
        val pivotSuggested = when {
            isQuestion -> "That's a key factor. From our end, focusing on $cleanObjective will unlock the best outcome—let's align on next steps."
            containsNegation -> "Understood on what won't work. Let's pivot to what options are currently actionable for $cleanObjective."
            rawText.isNotBlank() -> "That provides valuable context. Turning to our primary focus on $cleanObjective, where should we direct efforts next?"
            else -> "Let's pivot to our core objective: ensuring alignment on $cleanObjective."
        }
        val pivotReason = when {
            isQuestion -> "Answers inquiry while immediately steering focus back to the core strategic initiative."
            containsNegation -> "Redirects unproductive negative framing toward actionable solutions."
            else -> "Keeps conversation bound to strategic agenda and prevents unhelpful rabbit holes."
        }
        val pivotConfidence = if (isQuestion || containsNegation || transcriptHistory.size > 6) 90 else 80

        recommendations.add(
            StrategyRecommendation(
                type = StrategyType.PIVOT,
                name = StrategyType.PIVOT.displayName,
                description = StrategyType.PIVOT.description,
                purpose = StrategyType.PIVOT.purpose,
                recommendationReason = pivotReason,
                suggestedResponse = pivotSuggested,
                confidence = pivotConfidence,
                tone = ToneType.DIPLOMATIC,
                isPrimaryRecommended = primaryType == StrategyType.PIVOT,
                enabled = true
            )
        )

        // 5. Context-Triggered Extensible Strategies (e.g. TIMELINE, CLARIFY, SUMMARIZE, DE_ESCALATE)
        if (detectedInconsistency != null) {
            recommendations.add(
                StrategyRecommendation(
                    type = StrategyType.CLARIFY,
                    name = StrategyType.CLARIFY.displayName,
                    description = StrategyType.CLARIFY.description,
                    purpose = StrategyType.CLARIFY.purpose,
                    recommendationReason = "Discrepancy detected with earlier baseline statement (${detectedInconsistency.previousStatement}).",
                    suggestedResponse = "Earlier we noted ${detectedInconsistency.previousStatement}, but you just mentioned ${detectedInconsistency.currentStatement}. How do those two connect?",
                    confidence = 90,
                    tone = ToneType.CALM_CURIOUS,
                    isPrimaryRecommended = false,
                    enabled = true
                )
            )
        }

        if (containsTimeRef) {
            recommendations.add(
                StrategyRecommendation(
                    type = StrategyType.TIMELINE,
                    name = StrategyType.TIMELINE.displayName,
                    description = StrategyType.TIMELINE.description,
                    purpose = StrategyType.TIMELINE.purpose,
                    recommendationReason = "Multiple time references detected; reconstruct chronological order to isolate bottlenecks.",
                    suggestedResponse = "Could we step through this sequentially starting from the initial trigger up until today?",
                    confidence = 85,
                    tone = ToneType.CURIOUS,
                    isPrimaryRecommended = false,
                    enabled = true
                )
            )
        }

        if (transcriptHistory.size >= 8 || containsCommitment) {
            recommendations.add(
                StrategyRecommendation(
                    type = StrategyType.SUMMARIZE,
                    name = StrategyType.SUMMARIZE.displayName,
                    description = StrategyType.SUMMARIZE.description,
                    purpose = StrategyType.SUMMARIZE.purpose,
                    recommendationReason = "Sufficient points discussed to anchor mutual understanding and confirm commitments.",
                    suggestedResponse = "To summarize what we've aligned on so far: we're targeting key milestones and will review progress next.",
                    confidence = 88,
                    tone = ToneType.PROFESSIONAL,
                    isPrimaryRecommended = false,
                    enabled = true
                )
            )
        }

        return recommendations
    }
}
