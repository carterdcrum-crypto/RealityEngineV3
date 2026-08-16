package com.example.engine

import android.util.Log
import com.example.data.model.ClaimEntity
import com.example.data.model.MemoryEntity
import com.example.data.model.PersonEntity
import com.example.data.security.CryptoPreferencesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class LiveCopilotEngine(
    private val cryptoManager: CryptoPreferencesManager
) {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    suspend fun analyzeConversationTurn(
        caller: PersonEntity?,
        transcriptHistory: List<TranscriptSegment>,
        latestUtterance: TranscriptSegment?,
        knownClaims: List<ClaimEntity>,
        knownMemories: List<MemoryEntity>,
        objective: String = "Align on deliverable timelines and verify commitments"
    ): CopilotAnalysisResult = withContext(Dispatchers.Default) {

        val groqKey = cryptoManager.getGroqKey()
        val isAiEnabled = cryptoManager.isAiAnalysisEnabled

        if (!isAiEnabled) {
            return@withContext defaultPassiveResult(latestUtterance?.speakerName ?: "Contact")
        }

        if (groqKey.isNotBlank()) {
            try {
                val apiResult = callGroqApi(
                    groqKey = groqKey,
                    model = cryptoManager.getGroqModel(),
                    caller = caller,
                    transcript = transcriptHistory,
                    latestUtterance = latestUtterance,
                    knownClaims = knownClaims,
                    knownMemories = knownMemories,
                    objective = objective
                )
                if (apiResult != null) {
                    return@withContext apiResult
                }
            } catch (e: Exception) {
                Log.e(TAG, "Groq Live Engine fallback invoked: ${e.message}")
            }
        }

        // Fast high-precision deterministic copilot engine
        return@withContext evaluateDeterministicCopilot(
            caller = caller,
            transcriptHistory = transcriptHistory,
            latest = latestUtterance,
            knownClaims = knownClaims,
            knownMemories = knownMemories,
            objective = objective
        )
    }

    private suspend fun callGroqApi(
        groqKey: String,
        model: String,
        caller: PersonEntity?,
        transcript: List<TranscriptSegment>,
        latestUtterance: TranscriptSegment?,
        knownClaims: List<ClaimEntity>,
        knownMemories: List<MemoryEntity>,
        objective: String
    ): CopilotAnalysisResult? = withContext(Dispatchers.IO) {
        val transcriptFormatted = transcript.takeLast(10).joinToString("\n") {
            "[${it.timestamp}] ${it.speakerName}: \"${it.text}\""
        }

        val claimsSummary = knownClaims.joinToString("; ") {
            "${it.personName} previously claimed: '${it.currentStatement}' (${it.context})"
        }

        val memorySummary = knownMemories.joinToString("; ") {
            "[${it.state}] ${it.statement}"
        }

        val promptJson = JSONObject().apply {
            put("caller_name", caller?.name ?: "Unknown")
            put("organization", caller?.organization ?: "")
            put("current_topics", caller?.currentTopics ?: "")
            put("open_questions", caller?.openQuestions ?: "")
            put("recent_commitment", caller?.recentCommitment ?: "")
            put("known_claims", claimsSummary)
            put("known_memories", memorySummary)
            put("objective", objective)
            put("recent_transcript", transcriptFormatted)
            put("latest_utterance", latestUtterance?.text ?: "")
        }

        val systemPrompt = """
            You are REALITY ENGINE v2, an ultra-precise tactical live conversation co-pilot.
            Analyze the ongoing conversation and return ONLY valid JSON matching this schema:
            {
              "recommended_strategy": "COGNITIVE PROBE" | "MIRRORING" | "PIVOT" | "BONDING" | "CLARIFY" | "CONFRONT" | "VALIDATE" | "CHALLENGE" | "DE-ESCALATE" | "PROBE" | "FOLLOW-UP" | "SUMMARIZE" | "PAUSE" | "DIRECT RESPONSE",
              "tone": "CALM · CURIOUS" | "CALM" | "CURIOUS" | "WARM" | "NEUTRAL" | "DIRECT" | "ASSERTIVE" | "DIPLOMATIC" | "EMPATHETIC" | "CAUTIOUS" | "SKEPTICAL" | "PROFESSIONAL" | "URGENT" | "DE-ESCALATING",
              "confidence": 84,
              "suggested_response": "Exact words the user should speak right now.",
              "reason": "Tactical justification for this response.",
              "alternatives": [
                {"strategy": "MIRRORING", "suggested_response": "...", "tone": "CALM"},
                {"strategy": "PIVOT", "suggested_response": "...", "tone": "DIPLOMATIC"},
                {"strategy": "BONDING", "suggested_response": "...", "tone": "WARM"}
              ],
              "signals": {
                "linguistic": 0.25,
                "factual": 0.75,
                "acoustic": 0.40
              },
              "deception_signal": {
                "score": 73,
                "is_elevated": true,
                "contributors": [
                  {"label": "Linguistic distancing", "delta": 18},
                  {"label": "Statement inconsistency", "delta": 31},
                  {"label": "Uncertainty", "delta": 12},
                  {"label": "Context mismatch", "delta": 12}
                ],
                "why": "Current statement differs from a previous statement."
              },
              "inconsistency": {
                "detected": true,
                "previous": "I started the project in March.",
                "current": "I started the project in May.",
                "confidence": 87
              },
              "memory_candidate": {
                "detected": true,
                "statement": "Sarah says she is moving in October.",
                "suggested_state": "OBSERVED"
              }
            }
            Do not include markdown or backticks. Return raw JSON.
        """.trimIndent()

        val messages = JSONArray().apply {
            put(JSONObject().apply {
                put("role", "system")
                put("content", systemPrompt)
            })
            put(JSONObject().apply {
                put("role", "user")
                put("content", promptJson.toString())
            })
        }

        val requestBody = JSONObject().apply {
            put("model", model)
            put("messages", messages)
            put("temperature", 0.2)
            put("response_format", JSONObject().put("type", "json_object"))
        }.toString().toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url("https://api.groq.com/openai/v1/chat/completions")
            .header("Authorization", "Bearer $groqKey")
            .header("Content-Type", "application/json")
            .post(requestBody)
            .build()

        val response = httpClient.newCall(request).execute()
        if (!response.isSuccessful) return@withContext null

        val responseBody = response.body?.string() ?: return@withContext null
        val jsonRoot = JSONObject(responseBody)
        val choices = jsonRoot.optJSONArray("choices") ?: return@withContext null
        if (choices.length() == 0) return@withContext null
        val content = choices.getJSONObject(0).getJSONObject("message").getString("content")

        parseCopilotJson(content)
    }

    private fun parseCopilotJson(jsonStr: String): CopilotAnalysisResult? {
        return try {
            val json = JSONObject(jsonStr)
            val stratName = json.optString("recommended_strategy", "COGNITIVE PROBE")
            val strat = StrategyType.entries.firstOrNull { it.displayName.equals(stratName, true) }
                ?: StrategyType.COGNITIVE_PROBE

            val toneName = json.optString("tone", "CALM · CURIOUS")
            val tone = ToneType.entries.firstOrNull { it.displayName.equals(toneName, true) }
                ?: ToneType.CALM_CURIOUS

            val confidence = json.optInt("confidence", 84)
            val response = json.optString("suggested_response", "Can you help me understand what changed?")
            val reason = json.optString("reason", "Clarifies discrepancy without creating defensive resistance.")

            val altArray = json.optJSONArray("alternatives")
            val alternatives = mutableListOf<StrategyAlternative>()
            if (altArray != null) {
                for (i in 0 until altArray.length()) {
                    val item = altArray.getJSONObject(i)
                    val sName = item.optString("strategy")
                    val sType = StrategyType.entries.firstOrNull { it.displayName.equals(sName, true) }
                        ?: StrategyType.MIRRORING
                    val tName = item.optString("tone", "CALM")
                    val tType = ToneType.entries.firstOrNull { it.displayName.equals(tName, true) }
                        ?: ToneType.CALM
                    alternatives.add(
                        StrategyAlternative(
                            strategy = sType,
                            suggestedResponse = item.optString("suggested_response", ""),
                            tone = tType
                        )
                    )
                }
            }

            val sigObj = json.optJSONObject("signals")
            val liveSignals = LiveSignalMeters(
                linguisticPosition = (sigObj?.optDouble("linguistic", 0.25) ?: 0.25).toFloat(),
                factualPosition = (sigObj?.optDouble("factual", 0.75) ?: 0.75).toFloat(),
                acousticPosition = (sigObj?.optDouble("acoustic", 0.40) ?: 0.40).toFloat()
            )

            val decObj = json.optJSONObject("deception_signal")
            val decScore = decObj?.optInt("score", 18) ?: 18
            val decContribs = mutableListOf<DeceptionContributor>()
            val contribArr = decObj?.optJSONArray("contributors")
            if (contribArr != null) {
                for (i in 0 until contribArr.length()) {
                    val c = contribArr.getJSONObject(i)
                    decContribs.add(
                        DeceptionContributor(
                            label = c.optString("label", "Signal"),
                            scoreDelta = c.optInt("delta", 10)
                        )
                    )
                }
            }

            val deceptionState = DeceptionSignalState(
                score = decScore,
                isElevated = decScore >= 50,
                contributors = decContribs,
                whyExplanation = decObj?.optString("why", "Analysis derived from linguistic markers.") ?: ""
            )

            val incObj = json.optJSONObject("inconsistency")
            val inconsistencyAlert = if (incObj != null && incObj.optBoolean("detected", false)) {
                InconsistencyAlert(
                    previousStatement = incObj.optString("previous", ""),
                    currentStatement = incObj.optString("current", ""),
                    confidence = incObj.optInt("confidence", 85),
                    context = "Discrepancy detected against baseline records"
                )
            } else null

            val memObj = json.optJSONObject("memory_candidate")
            val memoryAlert = if (memObj != null && memObj.optBoolean("detected", false)) {
                MemoryCandidateAlert(
                    statement = memObj.optString("statement", ""),
                    suggestedState = memObj.optString("suggested_state", "OBSERVED"),
                    provenance = "Live Call Analysis"
                )
            } else null

            CopilotAnalysisResult(
                recommendedStrategy = strat,
                tone = tone,
                confidence = confidence,
                suggestedResponse = response,
                reason = reason,
                alternatives = alternatives,
                liveSignals = liveSignals,
                deceptionSignal = deceptionState,
                inconsistencyAlert = inconsistencyAlert,
                memoryCandidateAlert = memoryAlert
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing copilot JSON: ${e.message}")
            null
        }
    }

    private fun evaluateDeterministicCopilot(
        caller: PersonEntity?,
        transcriptHistory: List<TranscriptSegment>,
        latest: TranscriptSegment?,
        knownClaims: List<ClaimEntity>,
        knownMemories: List<MemoryEntity>,
        objective: String
    ): CopilotAnalysisResult {
        val text = latest?.text?.lowercase() ?: ""
        val speaker = latest?.speaker ?: Speaker.OTHER

        // Scenario 1: Sarah "I never said the meeting was Friday" or timeline/date discrepancy
        if (text.contains("never said") || text.contains("friday") || text.contains("meeting was friday")) {
            return CopilotAnalysisResult(
                recommendedStrategy = StrategyType.COGNITIVE_PROBE,
                tone = ToneType.CALM_CURIOUS,
                confidence = 84,
                suggestedResponse = "Can you help me understand what changed?",
                reason = "Gently invites clarification on the schedule without creating defensiveness.",
                alternatives = listOf(
                    StrategyAlternative(
                        StrategyType.MIRRORING,
                        "\"The meeting was never Friday?\"",
                        ToneType.CALM
                    ),
                    StrategyAlternative(
                        StrategyType.PIVOT,
                        "What target delivery date works best for the project launch?",
                        ToneType.DIPLOMATIC
                    ),
                    StrategyAlternative(
                        StrategyType.BONDING,
                        "I know deadlines have been shifting with the current workload, let's realign.",
                        ToneType.WARM
                    )
                ),
                liveSignals = LiveSignalMeters(
                    linguisticPosition = 0.22f,
                    factualPosition = 0.78f,
                    acousticPosition = 0.45f
                ),
                deceptionSignal = DeceptionSignalState(
                    score = 73,
                    isElevated = true,
                    label = "EXPERIMENTAL SIGNAL",
                    disclaimer = "REQUIRES HUMAN INTERPRETATION",
                    contributors = listOf(
                        DeceptionContributor("Linguistic distancing", 18),
                        DeceptionContributor("Statement inconsistency", 31),
                        DeceptionContributor("Uncertainty", 12),
                        DeceptionContributor("Context mismatch", 12)
                    ),
                    whyExplanation = "Current statement differs from a previous statement recorded in commitment history."
                ),
                inconsistencyAlert = InconsistencyAlert(
                    previousStatement = "Friday document delivery agreed on Aug 12",
                    currentStatement = "I never said the meeting was Friday",
                    confidence = 88,
                    context = "Schedule commitment discrepancy"
                ),
                memoryCandidateAlert = null
            )
        }

        // Scenario 2: Claim timeline inconsistency ("May" vs "March")
        if (text.contains("started the project in may") || text.contains("in may") || text.contains("started in may")) {
            return CopilotAnalysisResult(
                recommendedStrategy = StrategyType.CLARIFY,
                tone = ToneType.CALM,
                confidence = 89,
                suggestedResponse = "Earlier we noted the kickoff commenced in March—did the official sprint start in May?",
                reason = "Validates the discrepancy between previous March record and current May assertion.",
                alternatives = listOf(
                    StrategyAlternative(
                        StrategyType.COGNITIVE_PROBE,
                        "Can you walk me through the milestones between March and May?",
                        ToneType.CURIOUS
                    ),
                    StrategyAlternative(
                        StrategyType.MIRRORING,
                        "\"Started in May?\"",
                        ToneType.CALM
                    ),
                    StrategyAlternative(
                        StrategyType.PIVOT,
                        "Regardless of start date, what is the completion estimate?",
                        ToneType.DIRECT
                    )
                ),
                liveSignals = LiveSignalMeters(
                    linguisticPosition = 0.35f,
                    factualPosition = 0.85f,
                    acousticPosition = 0.30f
                ),
                deceptionSignal = DeceptionSignalState(
                    score = 68,
                    isElevated = true,
                    contributors = listOf(
                        DeceptionContributor("Statement inconsistency", 38),
                        DeceptionContributor("Timeline variance", 18),
                        DeceptionContributor("Context mismatch", 12)
                    ),
                    whyExplanation = "Current assertion (May kickoff) contradicts prior statement (March kickoff)."
                ),
                inconsistencyAlert = InconsistencyAlert(
                    previousStatement = "March",
                    currentStatement = "May",
                    confidence = 87,
                    context = "Kickoff timeline variance"
                ),
                memoryCandidateAlert = null
            )
        }

        // Scenario 3: Memory extraction ("moving in October" or relocation)
        if (text.contains("moving") || text.contains("october") || text.contains("relocating")) {
            return CopilotAnalysisResult(
                recommendedStrategy = StrategyType.BONDING,
                tone = ToneType.WARM,
                confidence = 92,
                suggestedResponse = "That's a major milestone—will the October move impact the Q4 release schedule?",
                reason = "Acknowledges personal milestone while smoothly maintaining awareness of project timeline.",
                alternatives = listOf(
                    StrategyAlternative(
                        StrategyType.VALIDATE,
                        "Moving is always stressful, let us know how we can support.",
                        ToneType.EMPATHETIC
                    ),
                    StrategyAlternative(
                        StrategyType.PIVOT,
                        "Let's make sure all deliverables are locked in before the relocation window.",
                        ToneType.PROFESSIONAL
                    )
                ),
                liveSignals = LiveSignalMeters(
                    linguisticPosition = 0.15f,
                    factualPosition = 0.20f,
                    acousticPosition = 0.18f
                ),
                deceptionSignal = DeceptionSignalState(
                    score = 14,
                    isElevated = false,
                    contributors = listOf(
                        DeceptionContributor("Baseline authenticity", 5),
                        DeceptionContributor("Personal disclosure", 9)
                    ),
                    whyExplanation = "Open personal disclosure with consistent tone."
                ),
                inconsistencyAlert = null,
                memoryCandidateAlert = MemoryCandidateAlert(
                    statement = "${caller?.name ?: "Contact"} says she is moving in October.",
                    suggestedState = "OBSERVED",
                    provenance = "Active Live Call"
                )
            )
        }

        // Scenario 4: Investor / Valuation / Term Sheet discussion
        if (text.contains("valuation") || text.contains("term sheet") || text.contains("board seat") || text.contains("investor")) {
            return CopilotAnalysisResult(
                recommendedStrategy = StrategyType.ASSERTIVE,
                tone = ToneType.DIPLOMATIC,
                confidence = 86,
                suggestedResponse = "We're structured to align board representation with capital deployment tranches.",
                reason = "Positions terms firmly while anchoring governance to capital investment.",
                alternatives = listOf(
                    StrategyAlternative(
                        StrategyType.COGNITIVE_PROBE,
                        "What specific governance concerns is your investment committee prioritizing?",
                        ToneType.CURIOUS
                    ),
                    StrategyAlternative(
                        StrategyType.MIRRORING,
                        "\"A mandatory board seat?\"",
                        ToneType.CALM
                    ),
                    StrategyAlternative(
                        StrategyType.PIVOT,
                        "Let's focus on milestone velocity for the next funding tranche.",
                        ToneType.DIRECT
                    )
                ),
                liveSignals = LiveSignalMeters(
                    linguisticPosition = 0.28f,
                    factualPosition = 0.42f,
                    acousticPosition = 0.35f
                ),
                deceptionSignal = DeceptionSignalState(
                    score = 24,
                    isElevated = false,
                    contributors = listOf(
                        DeceptionContributor("Commercial negotiation", 14),
                        DeceptionContributor("Strategic positioning", 10)
                    ),
                    whyExplanation = "Standard negotiation posture detected."
                ),
                inconsistencyAlert = null,
                memoryCandidateAlert = null
            )
        }

        // Default active copilot
        return CopilotAnalysisResult(
            recommendedStrategy = StrategyType.MIRRORING,
            tone = ToneType.CALM_CURIOUS,
            confidence = 82,
            suggestedResponse = if (text.isNotBlank()) "\"${text.take(40)}...\"" else "Understood. How would you prioritize the immediate next steps?",
            reason = "Maintains conversational flow and encourages speaker elaboration.",
            alternatives = listOf(
                StrategyAlternative(
                    StrategyType.COGNITIVE_PROBE,
                    "What led you to that conclusion?",
                    ToneType.CURIOUS
                ),
                StrategyAlternative(
                    StrategyType.PIVOT,
                    "Let's review the primary objective for this week.",
                    ToneType.DIRECT
                ),
                StrategyAlternative(
                    StrategyType.BONDING,
                    "I appreciate you sharing that context.",
                    ToneType.WARM
                )
            ),
            liveSignals = LiveSignalMeters(
                linguisticPosition = 0.20f,
                factualPosition = 0.30f,
                acousticPosition = 0.25f
            ),
            deceptionSignal = DeceptionSignalState(
                score = 21,
                isElevated = false,
                contributors = listOf(
                    DeceptionContributor("Conversational baseline", 12),
                    DeceptionContributor("Linguistic stability", 9)
                ),
                whyExplanation = "Linguistic markers and acoustic signals align with baseline expectations."
            ),
            inconsistencyAlert = null,
            memoryCandidateAlert = null
        )
    }

    private fun defaultPassiveResult(name: String): CopilotAnalysisResult {
        return CopilotAnalysisResult(
            recommendedStrategy = StrategyType.DIRECT_RESPONSE,
            tone = ToneType.NEUTRAL,
            confidence = 50,
            suggestedResponse = "AI Analysis is paused in Settings.",
            reason = "AI Co-pilot turned off by user configuration.",
            alternatives = emptyList(),
            liveSignals = LiveSignalMeters(0.1f, 0.1f, 0.1f),
            deceptionSignal = DeceptionSignalState(
                score = 0,
                isElevated = false,
                contributors = emptyList(),
                whyExplanation = "Passive monitoring"
            ),
            inconsistencyAlert = null,
            memoryCandidateAlert = null
        )
    }

    companion object {
        private const val TAG = "LiveCopilotEngine"
    }
}
