package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.RealityEngineDatabase
import com.example.data.model.CallRecordEntity
import com.example.data.model.ClaimEntity
import com.example.data.model.MemoryEntity
import com.example.data.model.PersonEntity
import com.example.data.model.SignalEventEntity
import com.example.data.security.CryptoPreferencesManager
import com.example.engine.CallSummaryPayload
import com.example.engine.CopilotAnalysisResult
import com.example.engine.DeceptionSignalState
import com.example.engine.InconsistencyAlert
import com.example.engine.LiveCopilotEngine
import com.example.engine.LiveSignalMeters
import com.example.engine.MemoryCandidateAlert
import com.example.engine.Speaker
import com.example.engine.StrategyAlternative
import com.example.engine.StrategyType
import com.example.engine.ToneType
import com.example.engine.TranscriptSegment
import com.example.telecom.CallManager
import com.example.telecom.TelecomCallState
import com.example.telecom.TelecomRoleManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale

enum class AppNavTab {
    CALL, PEOPLE, MEMORY, SIGNALS
}

enum class CallScreenState {
    IDLE, DIALING, INCOMING, ACTIVE, SUMMARY
}

data class ActiveCallState(
    val caller: PersonEntity? = null,
    val phoneNumber: String = "",
    val elapsedSeconds: Int = 0,
    val objective: String = "Project X Milestone Review & Schedule",
    val transcript: List<TranscriptSegment> = emptyList(),
    val copilotResult: CopilotAnalysisResult? = null,
    val selectedAlternative: StrategyAlternative? = null,
    val isMuted: Boolean = false,
    val isSpeakerOn: Boolean = false,
    val isKeypadOpen: Boolean = false,
    val isHold: Boolean = false,
    val keypadDtmf: String = "",
    val activeInconsistency: InconsistencyAlert? = null,
    val activeMemoryAlert: MemoryCandidateAlert? = null,
    val simulationScriptIndex: Int = 0
)

data class ApiTestState(
    val twilioStatus: String? = null,
    val isTestingTwilio: Boolean = false,
    val deepgramStatus: String? = null,
    val isTestingDeepgram: Boolean = false,
    val groqStatus: String? = null,
    val isTestingGroq: Boolean = false,
    val supabaseStatus: String? = null,
    val isTestingSupabase: Boolean = false
)

class RealityEngineViewModel(application: Application) : AndroidViewModel(application) {

    private val db = RealityEngineDatabase.getDatabase(application, viewModelScope)
    val cryptoManager = CryptoPreferencesManager(application)
    private val copilotEngine = LiveCopilotEngine(cryptoManager)

    // DAOs
    val personDao = db.personDao()
    val callRecordDao = db.callRecordDao()
    val memoryDao = db.memoryDao()
    val claimDao = db.claimDao()
    val signalEventDao = db.signalEventDao()

    // Navigation & UI States
    private val _isDefaultPhoneApp = MutableStateFlow(false)
    val isDefaultPhoneApp: StateFlow<Boolean> = _isDefaultPhoneApp.asStateFlow()

    private val _currentTab = MutableStateFlow(AppNavTab.CALL)
    val currentTab: StateFlow<AppNavTab> = _currentTab.asStateFlow()

    private val _isSettingsOpen = MutableStateFlow(false)
    val isSettingsOpen: StateFlow<Boolean> = _isSettingsOpen.asStateFlow()

    private val _callScreenState = MutableStateFlow(CallScreenState.IDLE)
    val callScreenState: StateFlow<CallScreenState> = _callScreenState.asStateFlow()

    private val _activeCall = MutableStateFlow(ActiveCallState())
    val activeCall: StateFlow<ActiveCallState> = _activeCall.asStateFlow()

    private val _postCallSummary = MutableStateFlow<CallSummaryPayload?>(null)
    val postCallSummary: StateFlow<CallSummaryPayload?> = _postCallSummary.asStateFlow()

    // Dialpad State
    private val _dialerNumber = MutableStateFlow("")
    val dialerNumber: StateFlow<String> = _dialerNumber.asStateFlow()

    private val _selectedPerson = MutableStateFlow<PersonEntity?>(null)
    val selectedPerson: StateFlow<PersonEntity?> = _selectedPerson.asStateFlow()

    private val _memoryFilterState = MutableStateFlow<String?>("ALL")
    val memoryFilterState: StateFlow<String?> = _memoryFilterState.asStateFlow()

    private val _apiTestState = MutableStateFlow(ApiTestState())
    val apiTestState: StateFlow<ApiTestState> = _apiTestState.asStateFlow()

    // Data Streams from Room
    val people = personDao.getAllPeopleFlow().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val recentCalls = callRecordDao.getAllCallRecordsFlow().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val allMemories = memoryDao.getAllActiveMemoriesFlow().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val allClaims = claimDao.getAllClaimsFlow().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val inconsistentClaims = claimDao.getInconsistentClaimsFlow().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val allSignals = signalEventDao.getAllSignalsFlow().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    private var callTimerJob: Job? = null
    private var simulationScriptJob: Job? = null

    init {
        refreshDefaultPhoneStatus()
        observeTelecomCalls()
    }

    fun refreshDefaultPhoneStatus() {
        val isDefault = TelecomRoleManager.isDefaultPhoneApp(getApplication())
        _isDefaultPhoneApp.value = isDefault
    }

    private fun observeTelecomCalls() {
        viewModelScope.launch {
            CallManager.callInfo.collect { info ->
                if (info == null) return@collect
                when (info.state) {
                    TelecomCallState.RINGING -> {
                        val num = info.phoneNumber
                        val person = people.value.firstOrNull {
                            it.phoneNumber.replace("[^0-9]".toRegex(), "") == num.replace("[^0-9]".toRegex(), "") ||
                                    it.name.equals(info.displayName, ignoreCase = true)
                        } ?: if (info.displayName.isNotBlank() && info.displayName != num) {
                            PersonEntity(
                                name = info.displayName,
                                phoneNumber = num,
                                relationship = "Incoming Caller",
                                currentTopics = "Inbound Telecom Call"
                            )
                        } else null

                        _activeCall.value = ActiveCallState(
                            caller = person,
                            phoneNumber = num.ifBlank { "+1 (555) 019-2834" },
                            elapsedSeconds = 0,
                            objective = person?.currentTopics ?: "Inbound Telecom Call",
                            transcript = emptyList()
                        )
                        _callScreenState.value = CallScreenState.INCOMING
                    }
                    TelecomCallState.DIALING, TelecomCallState.CONNECTING -> {
                        if (_callScreenState.value != CallScreenState.DIALING && _callScreenState.value != CallScreenState.ACTIVE) {
                            val num = info.phoneNumber
                            val person = people.value.firstOrNull {
                                it.phoneNumber.replace("[^0-9]".toRegex(), "") == num.replace("[^0-9]".toRegex(), "")
                            }
                            _activeCall.value = ActiveCallState(
                                caller = person,
                                phoneNumber = num,
                                elapsedSeconds = 0,
                                objective = person?.currentTopics ?: "Outbound Call"
                            )
                            _callScreenState.value = CallScreenState.DIALING
                        }
                    }
                    TelecomCallState.ACTIVE -> {
                        if (_callScreenState.value != CallScreenState.ACTIVE) {
                            transitionToActiveCall()
                        }
                    }
                    TelecomCallState.DISCONNECTED -> {
                        if (_callScreenState.value == CallScreenState.ACTIVE) {
                            endActiveCall()
                        } else if (_callScreenState.value == CallScreenState.INCOMING || _callScreenState.value == CallScreenState.DIALING) {
                            _callScreenState.value = CallScreenState.IDLE
                            stopCallTimers()
                        }
                    }
                    else -> {}
                }
            }
        }
    }

    // Navigation setters
    fun setTab(tab: AppNavTab) {
        _currentTab.value = tab
    }

    fun openSettings(open: Boolean) {
        _isSettingsOpen.value = open
    }

    fun selectPerson(person: PersonEntity?) {
        _selectedPerson.value = person
    }

    fun setMemoryFilter(filter: String?) {
        _memoryFilterState.value = filter
    }

    // Dialpad functions
    fun appendDialDigit(digit: String) {
        _dialerNumber.value += digit
    }

    fun deleteDialDigit() {
        if (_dialerNumber.value.isNotEmpty()) {
            _dialerNumber.value = _dialerNumber.value.dropLast(1)
        }
    }

    fun clearDialer() {
        _dialerNumber.value = ""
    }

    // Telephony & Call Flow
    fun startOutgoingCall(targetNumber: String, knownPerson: PersonEntity? = null) {
        val numberToCall = targetNumber.ifBlank { knownPerson?.phoneNumber ?: "+1 (415) 890-2134" }
        val person = knownPerson ?: people.value.firstOrNull {
            it.phoneNumber.replace("[^0-9]".toRegex(), "") == numberToCall.replace("[^0-9]".toRegex(), "")
        }

        _activeCall.value = ActiveCallState(
            caller = person,
            phoneNumber = numberToCall,
            elapsedSeconds = 0,
            objective = person?.currentTopics?.ifBlank { "Direct Consultation" } ?: "Direct Consultation",
            transcript = emptyList(),
            simulationScriptIndex = 0
        )
        _callScreenState.value = CallScreenState.DIALING

        // If default dialer, trigger real outbound phone call via TelecomManager
        val isRealDialer = isDefaultPhoneApp.value
        val callPlaced = if (isRealDialer) {
            CallManager.placeOutgoingCall(getApplication(), numberToCall)
        } else false

        if (!callPlaced) {
            // Local simulation fallback for testing / emulator
            viewModelScope.launch {
                delay(2200) // Connecting delay
                if (_callScreenState.value == CallScreenState.DIALING) {
                    transitionToActiveCall()
                }
            }
        }
    }

    fun triggerIncomingCall(caller: PersonEntity? = null) {
        val person = caller ?: people.value.firstOrNull { it.name == "Sarah" } ?: people.value.firstOrNull()
        val phoneNumber = person?.phoneNumber ?: "+1 (415) 890-2134"

        _activeCall.value = ActiveCallState(
            caller = person,
            phoneNumber = phoneNumber,
            elapsedSeconds = 0,
            objective = person?.currentTopics ?: "Project X Milestone Review & Schedule",
            transcript = emptyList(),
            simulationScriptIndex = 0
        )
        _callScreenState.value = CallScreenState.INCOMING
    }

    fun answerIncomingCall() {
        CallManager.answerCall()
        if (_callScreenState.value == CallScreenState.INCOMING) {
            transitionToActiveCall()
        }
    }

    fun declineIncomingCall() {
        CallManager.declineCall()
        val current = _activeCall.value
        viewModelScope.launch {
            callRecordDao.insertCallRecord(
                CallRecordEntity(
                    personId = current.caller?.id,
                    personName = current.caller?.name ?: "Unknown",
                    phoneNumber = current.phoneNumber,
                    callType = "MISSED",
                    timestamp = System.currentTimeMillis(),
                    durationSeconds = 0,
                    topic = "Declined Inbound Call",
                    summary = "Call was declined or missed."
                )
            )
        }
        _callScreenState.value = CallScreenState.IDLE
        stopCallTimers()
    }

    private fun transitionToActiveCall() {
        _callScreenState.value = CallScreenState.ACTIVE
        startCallTimer()
        startLiveDialogueStream()
    }

    private fun startCallTimer() {
        callTimerJob?.cancel()
        callTimerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                _activeCall.update { it.copy(elapsedSeconds = it.elapsedSeconds + 1) }
            }
        }
    }

    private fun startLiveDialogueStream() {
        simulationScriptJob?.cancel()
        simulationScriptJob = viewModelScope.launch {
            val isSarah = _activeCall.value.caller?.name?.contains("Sarah", true) == true
            val callerName = _activeCall.value.caller?.name ?: "Sarah"

            // Seed initial transcript turns matching user request scenario
            val script = if (isSarah) {
                listOf(
                    TranscriptSegment(
                        speaker = Speaker.OTHER,
                        speakerName = callerName,
                        text = "Hey, thanks for jumping on. I wanted to sync regarding our milestone schedule.",
                        timestamp = "00:12:30",
                        linguisticDistance = 0.15f,
                        stressLevel = 0.20f
                    ),
                    TranscriptSegment(
                        speaker = Speaker.YOU,
                        speakerName = "You",
                        text = "Good to connect. Are we still on track to review the deliverables this week?",
                        timestamp = "00:12:35",
                        linguisticDistance = 0.10f,
                        stressLevel = 0.15f
                    ),
                    TranscriptSegment(
                        speaker = Speaker.OTHER,
                        speakerName = callerName,
                        text = "I never said the meeting was Friday.",
                        timestamp = "00:12:43",
                        linguisticDistance = 0.65f,
                        stressLevel = 0.72f
                    ),
                    TranscriptSegment(
                        speaker = Speaker.YOU,
                        speakerName = "You",
                        text = "What date did you have in mind?",
                        timestamp = "00:12:51",
                        linguisticDistance = 0.12f,
                        stressLevel = 0.18f
                    ),
                    TranscriptSegment(
                        speaker = Speaker.OTHER,
                        speakerName = callerName,
                        text = "Also just a quick heads up—I am moving to Seattle in October.",
                        timestamp = "00:13:05",
                        linguisticDistance = 0.18f,
                        stressLevel = 0.25f
                    ),
                    TranscriptSegment(
                        speaker = Speaker.OTHER,
                        speakerName = callerName,
                        text = "And for the kickoff record, I started the project in May.",
                        timestamp = "00:13:18",
                        linguisticDistance = 0.70f,
                        stressLevel = 0.68f
                    )
                )
            } else {
                listOf(
                    TranscriptSegment(
                        speaker = Speaker.OTHER,
                        speakerName = callerName,
                        text = "Hello, thanks for taking the call. Let's discuss our target allocation.",
                        timestamp = "00:01:10",
                        linguisticDistance = 0.20f,
                        stressLevel = 0.25f
                    ),
                    TranscriptSegment(
                        speaker = Speaker.YOU,
                        speakerName = "You",
                        text = "Happy to review the term sheet details.",
                        timestamp = "00:01:18",
                        linguisticDistance = 0.10f,
                        stressLevel = 0.15f
                    ),
                    TranscriptSegment(
                        speaker = Speaker.OTHER,
                        speakerName = callerName,
                        text = "We require a formal board observer seat for the next financing round.",
                        timestamp = "00:01:28",
                        linguisticDistance = 0.45f,
                        stressLevel = 0.50f
                    )
                )
            }

            // Stream dialogue turns progressively
            for (item in script) {
                delay(2400)
                addTranscriptTurn(item)
            }
        }
    }

    fun addTranscriptTurn(segment: TranscriptSegment) {
        val currentList = _activeCall.value.transcript + segment
        _activeCall.update { it.copy(transcript = currentList) }

        // Trigger Live Co-Pilot Analysis
        viewModelScope.launch {
            val knownClaims = _activeCall.value.caller?.let { claimDao.getClaimsForPerson(it.name) } ?: emptyList()
            val knownMemories = _activeCall.value.caller?.let { memoryDao.getMemoriesForPerson(it.name) } ?: emptyList()

            val result = copilotEngine.analyzeConversationTurn(
                caller = _activeCall.value.caller,
                transcriptHistory = currentList,
                latestUtterance = segment,
                knownClaims = knownClaims,
                knownMemories = knownMemories,
                objective = _activeCall.value.objective
            )

            _activeCall.update {
                it.copy(
                    copilotResult = result,
                    selectedAlternative = null,
                    activeInconsistency = result.inconsistencyAlert ?: it.activeInconsistency,
                    activeMemoryAlert = result.memoryCandidateAlert ?: it.activeMemoryAlert
                )
            }

            // Record Signal Event in background
            signalEventDao.insertSignalEvent(
                SignalEventEntity(
                    personName = _activeCall.value.caller?.name ?: "Unknown",
                    linguisticDistance = result.liveSignals.linguisticPosition,
                    factualInconsistency = result.liveSignals.factualPosition,
                    acousticStress = result.liveSignals.acousticPosition,
                    compositeDeceptionScore = result.deceptionSignal.score,
                    whyExplanation = result.deceptionSignal.whyExplanation
                )
            )
        }
    }

    fun sendManualUtterance(text: String, isYou: Boolean) {
        if (text.isBlank()) return
        val timeFormatted = formatCallDuration(_activeCall.value.elapsedSeconds)
        val name = if (isYou) "You" else (_activeCall.value.caller?.name ?: "Contact")
        val segment = TranscriptSegment(
            speaker = if (isYou) Speaker.YOU else Speaker.OTHER,
            speakerName = name,
            text = text.trim(),
            timestamp = timeFormatted
        )
        addTranscriptTurn(segment)
    }

    fun selectAlternativeStrategy(alt: StrategyAlternative) {
        _activeCall.update { it.copy(selectedAlternative = alt) }
    }

    fun resetSelectedAlternative() {
        _activeCall.update { it.copy(selectedAlternative = null) }
    }

    fun saveMemoryCandidate(alert: MemoryCandidateAlert, state: String = "OBSERVED") {
        val caller = _activeCall.value.caller
        viewModelScope.launch {
            memoryDao.insertMemory(
                MemoryEntity(
                    personId = caller?.id,
                    personName = caller?.name ?: "Contact",
                    statement = alert.statement,
                    state = state,
                    provenance = "Live Call (${formatCallDuration(_activeCall.value.elapsedSeconds)})"
                )
            )
            _activeCall.update { it.copy(activeMemoryAlert = null) }
        }
    }

    fun dismissMemoryCandidate() {
        _activeCall.update { it.copy(activeMemoryAlert = null) }
    }

    fun dismissInconsistencyAlert() {
        _activeCall.update { it.copy(activeInconsistency = null) }
    }

    fun toggleMute() {
        val newMute = !_activeCall.value.isMuted
        CallManager.setMuted(newMute)
        _activeCall.update { it.copy(isMuted = newMute) }
    }

    fun toggleSpeaker() {
        val newSpeaker = !_activeCall.value.isSpeakerOn
        CallManager.setSpeakerphone(newSpeaker)
        _activeCall.update { it.copy(isSpeakerOn = newSpeaker) }
    }

    fun toggleKeypad() {
        _activeCall.update { it.copy(isKeypadOpen = !it.isKeypadOpen) }
    }

    fun toggleHold() {
        val newHold = !_activeCall.value.isHold
        if (newHold) {
            CallManager.holdCall()
        } else {
            CallManager.unholdCall()
        }
        _activeCall.update { it.copy(isHold = newHold) }
    }

    fun appendDtmf(char: String) {
        if (char.isNotEmpty()) {
            CallManager.playDtmf(char[0])
        }
        _activeCall.update { it.copy(keypadDtmf = it.keypadDtmf + char) }
    }

    fun endActiveCall() {
        CallManager.endCall()
        val current = _activeCall.value
        stopCallTimers()

        val durationFormatted = formatCallDuration(current.elapsedSeconds)
        val participantName = current.caller?.name ?: "Unknown (${current.phoneNumber})"

        val summary = CallSummaryPayload(
            participants = "$participantName, You",
            durationFormatted = durationFormatted,
            durationSeconds = current.elapsedSeconds,
            topics = current.objective,
            importantStatements = if (current.transcript.isNotEmpty()) {
                current.transcript.joinToString("; ") { "${it.speakerName}: \"${it.text}\"" }
            } else "Brief discussion on project deliverable timeline and schedule alignment.",
            claims = if (current.activeInconsistency != null) {
                "Stated project start was in ${current.activeInconsistency.currentStatement} (Previous baseline was ${current.activeInconsistency.previousStatement})"
            } else "Confirmed deliverable review and milestone velocity.",
            commitments = current.caller?.recentCommitment?.ifBlank { "Follow up regarding confirmed timeline" }
                ?: "Follow up regarding confirmed timeline",
            questionsAnswered = "Timeline discrepancies identified and clarified.",
            questionsUnresolved = current.caller?.openQuestions ?: "Did Project X launch?",
            potentialInconsistencies = current.activeInconsistency?.let {
                "Previous: ${it.previousStatement} vs Current: ${it.currentStatement} (${it.confidence}% confidence)"
            } ?: "No critical inconsistencies recorded.",
            deceptionSignalsSummary = "Average signal score: ${current.copilotResult?.deceptionSignal?.score ?: 22}% (${current.copilotResult?.deceptionSignal?.label ?: "EXPERIMENTAL SIGNAL"})",
            newMemoriesCreated = current.activeMemoryAlert?.statement ?: "No new memory candidates logged.",
            recommendedFollowUps = "Schedule follow-up check-in to confirm deliverable submission.",
            strategiesUsed = "COGNITIVE PROBE, MIRRORING, BONDING, CLARIFY"
        )

        _postCallSummary.value = summary
        _callScreenState.value = CallScreenState.SUMMARY
    }

    fun saveCallSummaryAndFinish(summary: CallSummaryPayload) {
        val current = _activeCall.value
        viewModelScope.launch {
            val recordId = callRecordDao.insertCallRecord(
                CallRecordEntity(
                    personId = current.caller?.id,
                    personName = current.caller?.name ?: "Unknown",
                    phoneNumber = current.phoneNumber,
                    callType = "OUTGOING",
                    timestamp = System.currentTimeMillis(),
                    durationSeconds = summary.durationSeconds,
                    topic = summary.topics,
                    summary = summary.importantStatements,
                    importantStatements = summary.importantStatements,
                    extractedClaims = summary.claims,
                    extractedCommitments = summary.commitments,
                    questionsAnswered = summary.questionsAnswered,
                    questionsUnresolved = summary.questionsUnresolved,
                    inconsistencies = summary.potentialInconsistencies,
                    deceptionSummary = summary.deceptionSignalsSummary,
                    newMemoriesCreated = summary.newMemoriesCreated,
                    recommendedFollowUps = summary.recommendedFollowUps,
                    strategiesUsed = summary.strategiesUsed,
                    deceptionAvgScore = current.copilotResult?.deceptionSignal?.score ?: 20,
                    transcriptJson = ""
                )
            )

            // If an inconsistency claim was flagged, save it
            current.activeInconsistency?.let { inc ->
                claimDao.insertClaim(
                    ClaimEntity(
                        personId = current.caller?.id,
                        personName = current.caller?.name ?: "Unknown",
                        currentStatement = inc.currentStatement,
                        previousStatement = inc.previousStatement,
                        context = inc.context,
                        inconsistencyConfidence = inc.confidence,
                        hasInconsistency = true,
                        detectedInCallId = recordId
                    )
                )
            }

            // Update contact last contact timestamp
            current.caller?.let { person ->
                personDao.updatePerson(
                    person.copy(lastContactTimestamp = System.currentTimeMillis())
                )
            }

            _callScreenState.value = CallScreenState.IDLE
            _postCallSummary.value = null
            _activeCall.value = ActiveCallState()
        }
    }

    fun discardSummaryAndFinish() {
        _callScreenState.value = CallScreenState.IDLE
        _postCallSummary.value = null
        _activeCall.value = ActiveCallState()
    }

    private fun stopCallTimers() {
        callTimerJob?.cancel()
        simulationScriptJob?.cancel()
    }

    // Person & Contact management
    fun savePerson(person: PersonEntity) {
        viewModelScope.launch {
            if (person.id == 0L) {
                personDao.insertPerson(person)
            } else {
                personDao.updatePerson(person)
            }
        }
    }

    fun deletePerson(person: PersonEntity) {
        viewModelScope.launch {
            personDao.deletePerson(person)
            if (_selectedPerson.value?.id == person.id) {
                _selectedPerson.value = null
            }
        }
    }

    // Memory management
    fun updateMemoryState(id: Long, state: String) {
        viewModelScope.launch {
            memoryDao.updateMemoryState(id, state)
        }
    }

    fun dismissMemory(id: Long) {
        viewModelScope.launch {
            memoryDao.dismissMemory(id)
        }
    }

    fun deleteMemory(memory: MemoryEntity) {
        viewModelScope.launch {
            memoryDao.deleteMemory(memory)
        }
    }

    fun addManualMemory(personName: String, statement: String, state: String = "OBSERVED") {
        viewModelScope.launch {
            val person = people.value.firstOrNull { it.name.equals(personName, true) }
            memoryDao.insertMemory(
                MemoryEntity(
                    personId = person?.id,
                    personName = personName,
                    statement = statement,
                    state = state,
                    provenance = "Manual Entry"
                )
            )
        }
    }

    // API Tests
    fun testTwilioApi(sid: String, token: String) {
        viewModelScope.launch {
            _apiTestState.update { it.copy(isTestingTwilio = true, twilioStatus = "Connecting...") }
            val result = cryptoManager.testTwilio(sid, token)
            _apiTestState.update {
                it.copy(
                    isTestingTwilio = false,
                    twilioStatus = result.fold({ msg -> "✓ $msg" }, { err -> "✗ ${err.message}" })
                )
            }
        }
    }

    fun testDeepgramApi(apiKey: String) {
        viewModelScope.launch {
            _apiTestState.update { it.copy(isTestingDeepgram = true, deepgramStatus = "Verifying...") }
            val result = cryptoManager.testDeepgram(apiKey)
            _apiTestState.update {
                it.copy(
                    isTestingDeepgram = false,
                    deepgramStatus = result.fold({ msg -> "✓ $msg" }, { err -> "✗ ${err.message}" })
                )
            }
        }
    }

    fun testGroqApi(apiKey: String) {
        viewModelScope.launch {
            _apiTestState.update { it.copy(isTestingGroq = true, groqStatus = "Testing Llama-3.1...") }
            val result = cryptoManager.testGroq(apiKey)
            _apiTestState.update {
                it.copy(
                    isTestingGroq = false,
                    groqStatus = result.fold({ msg -> "✓ $msg" }, { err -> "✗ ${err.message}" })
                )
            }
        }
    }

    fun testSupabaseApi(url: String, anonKey: String) {
        viewModelScope.launch {
            _apiTestState.update { it.copy(isTestingSupabase = true, supabaseStatus = "Probing endpoint...") }
            val result = cryptoManager.testSupabase(url, anonKey)
            _apiTestState.update {
                it.copy(
                    isTestingSupabase = false,
                    supabaseStatus = result.fold({ msg -> "✓ $msg" }, { err -> "✗ ${err.message}" })
                )
            }
        }
    }

    fun formatCallDuration(seconds: Int): String {
        val hours = seconds / 3600
        val mins = (seconds % 3600) / 60
        val secs = seconds % 60
        return if (hours > 0) {
            String.format(Locale.US, "%02d:%02d:%02d", hours, mins, secs)
        } else {
            String.format(Locale.US, "%02d:%02d:%02d", 0, mins, secs)
        }
    }
}
