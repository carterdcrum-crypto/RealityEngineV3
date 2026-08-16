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
    companion object {
        private const val TAG = "LiveCopilotEngine"
    }

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
        objective: String = "Verify commitments and maintain alignment"
    ): CopilotAnalysisResult = withContext(Dispatchers.Default) {
        val groqKey = cryptoManager.getGroqKey()
        val isAiEnabled = cryptoManager.isAiAnalysisEnabled

        if (!isAiEnabled) {
            return@withContext defaultPassiveResult()
        }

        if (latestUtterance == null && transcriptHistory.isEmpty()) {
            return@withContext CopilotAnalysisResult(
                recommendedStrategy = StrategyType.DIRECT_RESPONSE,
                tone = ToneType.CALM,
                confidence = 80,
                suggestedResponse = "Awaiting speech to generate live tactical response...",
                reason = "Call connected. Listening for conversational context.",
                alternatives = emptyList(),
                liveSignals = LiveSignalMeters(0.1f, 0.1f, 0.1f),
                deceptionSignal = DeceptionSignalState(score = 0, isElevated = false, contributors = emptyList(), whyExplanation = "Awaiting speech"),
                inconsistencyAlert = null,
                memoryCandidateAlert = null
            )
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
                Log.e(TAG, "Groq Live Engine error: ${e.message}")
            }
        }

        // Real dynamic heuristic linguistic analysis based on actual speech input
        return@withContext evaluateDynamicLinguisticCopilot(
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
            "[${it.speakerName}]: \"${it.text}\""
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
            You are REALITY ENGINE, an ultra-precise tactical live conversation co-pilot.
            Analyze the real-time ongoing conversation and return ONLY valid JSON matching this schema:
            {
              "recommended_strategy": "COGNITIVE PROBE" | "MIRRORING" | "PIVOT" | "BONDING" | "CLARIFY" | "CONFRONT" | "VALIDATE" | "CHALLENGE" | "DE-ESCALATE" | "ASSERTIVE" | "DIRECT RESPONSE",
              "tone": "CALM · CURIOUS" | "CALM" | "CURIOUS" | "WARM" | "NEUTRAL" | "DIRECT" | "ASSERTIVE" | "DIPLOMATIC" | "EMPATHETIC" | "PROFESSIONAL",
              "confidence": 85,
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
                "score": 25,
                "is_elevated": false,
                "contributors": [
                  {"label": "Linguistic markers", "delta": 10}
                ],
                "why": "Linguistic flow assessment."
              },
              "inconsistency": {
                "detected": false,
                "previous": "",
                "current": "",
                "confidence": 0
              },
              "memory_candidate": {
                "detected": false,
                "statement": "",
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
            val response = json.optString("suggested_response", "Can you tell me more about that?")
            val reason = json.optString("reason", "Maintains conversational flow.")

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
                whyExplanation = decObj?.optString("why", "Analysis derived from speech markers.") ?: ""
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

    private fun evaluateDynamicLinguisticCopilot(
        caller: PersonEntity?,
        transcriptHistory: List<TranscriptSegment>,
        latest: TranscriptSegment?,
        knownClaims: List<ClaimEntity>,
        knownMemories: List<MemoryEntity>,
        objective: String
    ): CopilotAnalysisResult {
        val rawText = latest?.text ?: ""
        val textLower = rawText.lowercase()

        // 1. Linguistic markers calculation
        val hedgeWords = listOf("maybe", "i think", "probably", "sort of", "kind of", "honestly", "to be honest", "as far as i know")
        val hedgeCount = hedgeWords.count { textLower.contains(it) }

        val negationWords = listOf("never", "didn't", "did not", "wasn't", "was not", "cannot", "can't", "won't", "no way")
        val negationCount = negationWords.count { textLower.contains(it) }

        val isQuestion = rawText.endsWith("?") || textLower.startsWith("what") || textLower.startsWith("how") || textLower.startsWith("why") || textLower.startsWith("when") || textLower.startsWith("who")

        // 2. Check for real Inconsistency against known claims in DB
        var detectedInconsistency: InconsistencyAlert? = null
        for (claim in knownClaims) {
            if (claim.currentStatement.isNotBlank()) {
                val claimKeywords = claim.currentStatement.lowercase().split(" ").filter { it.length > 3 }
                val matchCount = claimKeywords.count { textLower.contains(it) }
                if (matchCount >= 2 && negationCount > 0) {
                    detectedInconsistency = InconsistencyAlert(
                        previousStatement = claim.currentStatement,
                        currentStatement = rawText,
                        confidence = 82,
                        context = "Discrepancy with previously recorded claim"
                    )
                    break
                }
            }
        }

        // 3. Check for potential Memory candidate
        var memoryAlert: MemoryCandidateAlert? = null
        val memoryTriggers = listOf("i will", "i promise", "moving to", "relocating", "budget is", "my target is", "we agreed", "deadline is")
        if (memoryTriggers.any { textLower.contains(it) }) {
            memoryAlert = MemoryCandidateAlert(
                statement = "${caller?.name ?: "Speaker"}: \"$rawText\"",
                suggestedState = "OBSERVED",
                provenance = "Live Audio Stream"
            )
        }

        // 4. Calculate Deception / Stress score
        val deceptionScore = ((hedgeCount * 14) + (negationCount * 18) + (if (detectedInconsistency != null) 35 else 0)).coerceIn(10, 88)
        val isElevated = deceptionScore >= 50

        val contributors = mutableListOf<DeceptionContributor>()
        if (hedgeCount > 0) contributors.add(DeceptionContributor("Hedging / Uncertainty", hedgeCount * 14))
        if (negationCount > 0) contributors.add(DeceptionContributor("Strong Negation", negationCount * 18))
        if (detectedInconsistency != null) contributors.add(DeceptionContributor("Claim Variance", 35))
        if (contributors.isEmpty()) contributors.add(DeceptionContributor("Conversational baseline", 10))

        val deceptionState = DeceptionSignalState(
            score = deceptionScore,
            isElevated = isElevated,
            contributors = contributors,
            whyExplanation = if (isElevated) "Elevated linguistic qualifiers and negation detected." else "Speech metrics match normal conversation baseline."
        )

        // 5. Formulate tactical response
        val strategy: StrategyType
        val tone: ToneType
        val suggestedResponse: String
        val reason: String

        if (detectedInconsistency != null) {
            strategy = StrategyType.CLARIFY
            tone = ToneType.CALM_CURIOUS
            suggestedResponse = "Can you help me understand how that aligns with our earlier baseline?"
            reason = "Addresses statement variance neutrally without creating defensiveness."
        } else if (isQuestion) {
            strategy = StrategyType.DIRECT_RESPONSE
            tone = ToneType.DIRECT
            suggestedResponse = "From our perspective, the key priority is achieving the objective cleanly."
            reason = "Directly satisfies the inquiry while anchoring to core priorities."
        } else if (rawText.length > 25) {
            strategy = StrategyType.MIRRORING
            tone = ToneType.CALM
            val mirrorPhrase = rawText.split(" ").takeLast(4).joinToString(" ")
            suggestedResponse = "\"$mirrorPhrase?\""
            reason = "Mirrors recent phrasing to encourage speaker elaboration."
        } else {
            strategy = StrategyType.COGNITIVE_PROBE
            tone = ToneType.CURIOUS
            suggestedResponse = "What would be the most effective next milestone from your perspective?"
            reason = "Prompts strategic input and keeps dialogue moving forward."
        }

        val alternatives = listOf(
            StrategyAlternative(StrategyType.COGNITIVE_PROBE, "What specific factor is driving that priority?", ToneType.CURIOUS),
            StrategyAlternative(StrategyType.PIVOT, "Let's focus on the concrete deliverables for this sprint.", ToneType.DIPLOMATIC),
            StrategyAlternative(StrategyType.BONDING, "I completely understand where you're coming from.", ToneType.WARM)
        )

        return CopilotAnalysisResult(
            recommendedStrategy = strategy,
            tone = tone,
            confidence = 82,
            suggestedResponse = suggestedResponse,
            reason = reason,
            alternatives = alternatives,
            liveSignals = LiveSignalMeters(
                linguisticPosition = (hedgeCount * 0.2f).coerceIn(0.1f, 0.9f),
                factualPosition = if (detectedInconsistency != null) 0.85f else 0.25f,
                acousticPosition = 0.35f
            ),
            deceptionSignal = deceptionState,
            inconsistencyAlert = detectedInconsistency,
            memoryCandidateAlert = memoryAlert
        )
    }

    private fun defaultPassiveResult(): CopilotAnalysisResult {
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
}
