package com.example.engine

import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max

/** Keeps Groq event-driven instead of continuously querying the model. */
class GroqGovernor(
    private val minimumEventIntervalMs: Long = 4_000L,
    private val maxRequestsPerCall: Int = 30
) {
    private var requestCount = 0
    private var lastRequestAt = 0L
    private val responseCache = ConcurrentHashMap<String, CopilotAnalysisResult>()

    @Synchronized
    fun shouldRequest(event: AnalysisEvent, nowMs: Long = System.currentTimeMillis()): Boolean {
        if (requestCount >= maxRequestsPerCall) return false
        if (nowMs - lastRequestAt < minimumEventIntervalMs) return false
        return when (event) {
            AnalysisEvent.USER_REQUEST,
            AnalysisEvent.HIGH_PRIORITY_SIGNAL,
            AnalysisEvent.INCONSISTENCY,
            AnalysisEvent.MAJOR_TOPIC_CHANGE -> true
            AnalysisEvent.ROUTINE_TRANSCRIPT -> false
        }
    }

    @Synchronized
    fun markRequest(nowMs: Long = System.currentTimeMillis()) {
        requestCount++
        lastRequestAt = nowMs
    }

    fun getCached(contextKey: String): CopilotAnalysisResult? = responseCache[contextKey]

    fun cache(contextKey: String, result: CopilotAnalysisResult) {
        responseCache[contextKey] = result
    }

    @Synchronized
    fun remainingRequests(): Int = max(0, maxRequestsPerCall - requestCount)

    @Synchronized
    fun resetCallBudget() {
        requestCount = 0
        lastRequestAt = 0L
        responseCache.clear()
    }
}

enum class AnalysisEvent {
    ROUTINE_TRANSCRIPT,
    MAJOR_TOPIC_CHANGE,
    INCONSISTENCY,
    HIGH_PRIORITY_SIGNAL,
    USER_REQUEST
}
