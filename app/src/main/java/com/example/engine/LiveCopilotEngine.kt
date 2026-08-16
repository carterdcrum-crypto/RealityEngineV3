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
    private val cryptoManager: CryptoPreferencesManager,
    private val strategyEngine: StrategyEngine = StrategyEngine()
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
            return@withContext defaultPassiveResult(caller, transcriptHistory, latestUtterance, knownClaims, knownMemories, objective)
        }

        if (latestUtterance == null && transcriptHistory.isEmpty()) {
            val initialStrategies = strategyEngine.evaluateStrategies(
                caller = caller,
                transcriptHistory = emptyList(),
                latestUtterance = null,
                knownClaims = knownClaims,
                knownMemories = knownMemories,
                objective = objective
            )
            return@withContext CopilotAnalysisResult(
                recommendedStrategy = StrategyType.COGNITIVE_PROBE,
                tone = ToneType.CALM,
                confidence = 80,
                suggestedResponse = "Awaiting speech to generate live tactical response...",
                reason = "Call connected. Listening for conversational context.",
                strategies = initialStrategies,
                alternatives = initialStrategies.map { StrategyAlternative(it.type, it.suggestedResponse, it.tone) },
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
            You are REALITY ENGINE, an ultra-precise tactical live conversation co-pilot and strategy engine.
            Analyze the real-time ongoing conversation and return ONLY valid JSON matching this schema:
            {
              "recommended_strategy": "COGNITIVE PROBE" | "MIRROR" | "PIVOT" | "BONDING" | "CLARIFY" | "TIMELINE" | "SUMMARIZE",
              "tone": "CALM · CURIOUS" | "CALM" | "CURIOUS" | "WARM" | "NEUTRAL" | "DIRECT" | "ASSERTIVE" | "DIPLOMATIC" | "EMPATHETIC" | "PROFESSIONAL",
              "confidence": 85,
              "suggested_response": "Exact words the user should speak right now.",
              "reason": "Tactical justification for this response.",
              "strategies": [
                {
                  "type": "BONDING",
                  "suggested_response": "...",
                  "reason": "...",
                  "confidence": 84,
                  "tone": "WARM"
                },
                {
                  "type": "COGNITIVE PROBE",
                  "suggested_response": "...",
                  "reason": "...",
                  "confidence": 90,
                  "tone": "CURIOUS"
                },
                {
                  "type": "MIRROR",
                  "suggested_response": "...",
                  "reason": "...",
                  "confidence": 82,
                  "tone": "CALM"
                },
                {
                  "type": "PIVOT",
                  "suggested_response": "...",
                  "reason": "...",
                  "confidence": 86,
                  "tone": "DIPLOMATIC"
                }
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
            Ensure ALL FOUR primary strategies (BONDING, COGNITIVE PROBE, MIRROR, PIVOT) are included in the strategies array with actionable contextual phrases.
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

        parseCopilotJson(content, caller, transcript, latestUtterance, knownClaims, knownMemories, objective)
    }

    private fun parseCopilotJson(
        jsonStr: String,
        caller: PersonEntity?,
        transcript: List<TranscriptSegment>,
        latestUtterance: TranscriptSegment?,
        knownClaims: List<ClaimEntity>,
        knownMemories: List<MemoryEntity>,
        objective: String
    ): CopilotAnalysisResult? {
        return try {
            val json = JSONObject(jsonStr)
            val stratName = json.optString("recommended_strategy", "COGNITIVE PROBE")
            val strat = StrategyType.entries.firstOrNull {
                it.displayName.equals(stratName, true) || it.name.equals(stratName, true)
            } ?: StrategyType.COGNITIVE_PROBE

            val toneName = json.optString("tone", "CALM · CURIOUS")
            val tone = ToneType.entries.firstOrNull {
                it.displayName.equals(toneName, true) || it.name.equals(toneName, true)
            } ?: ToneType.CALM_CURIOUS

            val confidence = json.optInt("confidence", 84)
            val response = json.optString("suggested_response", "Can you tell me more about that?")
            val reason = json.optString("reason", "Maintains conversational flow.")

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

            // Parse strategies array
            val parsedStrategies = mutableListOf<StrategyRecommendation>()
            val stratArray = json.optJSONArray("strategies")
            if (stratArray != null) {
                for (i in 0 until stratArray.length()) {
                    val item = stratArray.getJSONObject(i)
                    val sName = item.optString("type", item.optString("strategy"))
                    val sType = StrategyType.entries.firstOrNull {
                        it.name.equals(sName, true) || it.displayName.equals(sName, true)
                    } ?: StrategyType.COGNITIVE_PROBE
                    val tName = item.optString("tone", "CALM")
                    val tType = ToneType.entries.firstOrNull {
                        it.name.equals(tName, true) || it.displayName.equals(tName, true)
                    } ?: ToneType.CALM

                    parsedStrategies.add(
                        StrategyRecommendation(
                            type = sType,
                            name = sType.displayName,
                            description = sType.description,
                            purpose = sType.purpose,
                            recommendationReason = item.optString("reason", sType.description),
                            suggestedResponse = item.optString("suggested_response", ""),
                            confidence = item.optInt("confidence", 80),
                            tone = tType,
                            isPrimaryRecommended = sType == strat,
                            enabled = true
                        )
                    )
                }
            }

            // Guarantee all 4 primary strategies are populated
            val fallbackStrategies = strategyEngine.evaluateStrategies(
                caller = caller,
                transcriptHistory = transcript,
                latestUtterance = latestUtterance,
                knownClaims = knownClaims,
                knownMemories = knownMemories,
                objective = objective,
                detectedInconsistency = inconsistencyAlert,
                deceptionScore = decScore,
                previousStrategy = strat
            )

            val finalStrategies = mutableListOf<StrategyRecommendation>()
            val primaryEnums = StrategyRegistry.getAllPrimaryTypes().distinctBy { it.displayName }

            for (primType in primaryEnums) {
                val existing = parsedStrategies.firstOrNull {
                    it.type.displayName.equals(primType.displayName, true)
                }
                if (existing != null && existing.suggestedResponse.isNotBlank()) {
                    finalStrategies.add(existing.copy(isPrimaryRecommended = existing.type == strat))
                } else {
                    val fallback = fallbackStrategies.firstOrNull {
                        it.type.displayName.equals(primType.displayName, true)
                    }
                    if (fallback != null) {
                        finalStrategies.add(fallback.copy(isPrimaryRecommended = fallback.type == strat))
                    }
                }
            }

            // Include any additional contextual strategies (e.g. TIMELINE, CLARIFY)
            fallbackStrategies.filter { it.type !in primaryEnums }.forEach { extra ->
                finalStrategies.add(extra)
            }

            val alternatives = finalStrategies.map {
                StrategyAlternative(
                    strategy = it.type,
                    suggestedResponse = it.suggestedResponse,
                    tone = it.tone
                )
            }

            CopilotAnalysisResult(
                recommendedStrategy = strat,
                tone = tone,
                confidence = confidence,
                suggestedResponse = response,
                reason = reason,
                strategies = finalStrategies,
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

        // 5. Evaluate dynamic strategy suite
        val allStrategies = strategyEngine.evaluateStrategies(
            caller = caller,
            transcriptHistory = transcriptHistory,
            latestUtterance = latest,
            knownClaims = knownClaims,
            knownMemories = knownMemories,
            objective = objective,
            detectedInconsistency = detectedInconsistency,
            deceptionScore = deceptionScore
        )

        val primaryStrategy = allStrategies.firstOrNull { it.isPrimaryRecommended } ?: allStrategies.first()

        val alternatives = allStrategies.map {
            StrategyAlternative(
                strategy = it.type,
                suggestedResponse = it.suggestedResponse,
                tone = it.tone
            )
        }

        return CopilotAnalysisResult(
            recommendedStrategy = primaryStrategy.type,
            tone = primaryStrategy.tone,
            confidence = primaryStrategy.confidence,
            suggestedResponse = primaryStrategy.suggestedResponse,
            reason = primaryStrategy.recommendationReason,
            strategies = allStrategies,
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

    private fun defaultPassiveResult(
        caller: PersonEntity?,
        transcriptHistory: List<TranscriptSegment>,
        latestUtterance: TranscriptSegment?,
        knownClaims: List<ClaimEntity>,
        knownMemories: List<MemoryEntity>,
        objective: String
    ): CopilotAnalysisResult {
        val passiveStrategies = strategyEngine.evaluateStrategies(
            caller = caller,
            transcriptHistory = transcriptHistory,
            latestUtterance = latestUtterance,
            knownClaims = knownClaims,
            knownMemories = knownMemories,
            objective = objective
        )
        return CopilotAnalysisResult(
            recommendedStrategy = StrategyType.DIRECT_RESPONSE,
            tone = ToneType.NEUTRAL,
            confidence = 50,
            suggestedResponse = "AI Analysis is paused in Settings.",
            reason = "AI Co-pilot turned off by user configuration.",
            strategies = passiveStrategies,
            alternatives = passiveStrategies.map { StrategyAlternative(it.type, it.suggestedResponse, it.tone) },
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
