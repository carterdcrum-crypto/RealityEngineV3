package com.example.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.contacts.DeviceContact
import com.example.data.contacts.DeviceContactsManager
import com.example.data.local.RealityEngineDatabase
import com.example.data.model.CallRecordEntity
import com.example.data.model.ClaimEntity
import com.example.data.model.MemoryEntity
import com.example.data.model.PersonEntity
import com.example.data.model.SignalEventEntity
import com.example.data.security.CryptoPreferencesManager
import com.example.engine.CallSummaryPayload
import com.example.engine.CopilotAnalysisResult
import com.example.engine.DeepgramLiveTranscriber
import com.example.engine.InconsistencyAlert
import com.example.engine.LiveCopilotEngine
import com.example.engine.MemoryCandidateAlert
import com.example.engine.Speaker
import com.example.engine.StrategyAlternative
import com.example.engine.StrategyRecommendation
import com.example.engine.StrategyType
import com.example.engine.TranscriberState
import com.example.engine.TranscriptSegment
import com.example.telecom.CallManager
import com.example.telecom.CallService
import com.example.telecom.CallState
import com.example.telecom.TelecomRoleManager
import com.example.telecom.TwilioTelephonyService
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
import java.util.UUID

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
    val objective: String = "Live Consultation & Fact Verification",
    val transcript: List<TranscriptSegment> = emptyList(),
    val copilotResult: CopilotAnalysisResult? = null,
    val activeInconsistency: InconsistencyAlert? = null,
    val selectedAlternative: StrategyAlternative? = null,
    val selectedStrategy: StrategyRecommendation? = null,
    val isMuted: Boolean = false,
    val isSpeakerOn: Boolean = false,
    val isKeypadOpen: Boolean = false,
    val isHold: Boolean = false,
    val keypadDtmf: String = "",
    val activeMemoryAlert: MemoryCandidateAlert? = null,
    val isTwilioCall: Boolean = false,
    val twilioCallSid: String? = null,
    val callState: CallState = CallState.IDLE,
    val rawTwilioStatus: String? = null,
    val transcriberState: TranscriberState = TranscriberState.IDLE,
    val audioWaveformAmp: Float = 0f
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
    companion object {
        private const val TAG = "RealityEngineViewModel"
    }

    private val db = RealityEngineDatabase.getDatabase(application, viewModelScope)
    val cryptoManager = CryptoPreferencesManager(application)
    private val copilotEngine = LiveCopilotEngine(cryptoManager)
    val twilioService = TwilioTelephonyService()
    val transcriber = DeepgramLiveTranscriber()
    val mediaStreamBridge = com.example.engine.TwilioMediaStreamBridge()

    // DAOs
    val personDao = db.personDao()
    val callRecordDao = db.callRecordDao()
    val memoryDao = db.memoryDao()
    val claimDao = db.claimDao()
    val signalEventDao = db.signalEventDao()

    val contactsManager = DeviceContactsManager(application, personDao)

    // Navigation & UI States
    private val _isDefaultPhoneApp = MutableStateFlow(false)
    val isDefaultPhoneApp: StateFlow<Boolean> = _isDefaultPhoneApp.asStateFlow()

    private val _currentTab = MutableStateFlow(AppNavTab.CALL)
    val currentTab: StateFlow<AppNavTab> = _currentTab.asStateFlow()

    private val _isSettingsOpen = MutableStateFlow(false)
    val isSettingsOpen: StateFlow<Boolean> = _isSettingsOpen.asStateFlow()

    // Authoritative Call Lifecycle State
    private val _callState = MutableStateFlow(CallState.IDLE)
    val callState: StateFlow<CallState> = _callState.asStateFlow()

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

    private val _syncStatusMessage = MutableStateFlow<String?>(null)
    val syncStatusMessage: StateFlow<String?> = _syncStatusMessage.asStateFlow()

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

    init {
        refreshDefaultPhoneStatus()
        observeTelecomCalls()
        observeTwilioCalls()
        observeTranscriber()
    }

    fun refreshDefaultPhoneStatus() {
        val isDefault = TelecomRoleManager.isDefaultPhoneApp(getApplication())
        _isDefaultPhoneApp.value = isDefault
    }

    private fun observeTelecomCalls() {
        viewModelScope.launch {
            CallManager.callInfo.collect { info ->
                if (info == null) return@collect
                val realState = info.state
                _callState.value = realState
                _activeCall.update { it.copy(callState = realState) }
                when (realState) {
                    CallState.RINGING -> {
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
                            phoneNumber = num.ifBlank { "Incoming Call" },
                            elapsedSeconds = 0,
                            objective = person?.currentTopics?.ifBlank { "Inbound Telecom Call" } ?: "Inbound Telecom Call",
                            transcript = emptyList(),
                            isTwilioCall = false,
                            callState = CallState.RINGING
                        )
                        _callScreenState.value = CallScreenState.INCOMING
                    }
                    CallState.CONNECTING -> {
                        if (_callScreenState.value != CallScreenState.DIALING && _callScreenState.value != CallScreenState.ACTIVE) {
                            val num = info.phoneNumber
                            val person = people.value.firstOrNull {
                                it.phoneNumber.replace("[^0-9]".toRegex(), "") == num.replace("[^0-9]".toRegex(), "")
                            }
                            _activeCall.value = ActiveCallState(
                                caller = person,
                                phoneNumber = num,
                                elapsedSeconds = 0,
                                objective = person?.currentTopics?.ifBlank { "Outbound Call" } ?: "Outbound Call",
                                isTwilioCall = false,
                                callState = CallState.CONNECTING
                            )
                            _callScreenState.value = CallScreenState.DIALING
                        }
                    }
                    CallState.ACTIVE -> {
                        if (_callScreenState.value != CallScreenState.ACTIVE) {
                            transitionToActiveCall()
                        }
                    }
                    CallState.HOLDING -> {
                        _activeCall.update { it.copy(isHold = true) }
                    }
                    CallState.DISCONNECTED -> {
                        if (_callScreenState.value == CallScreenState.ACTIVE) {
                            endActiveCall()
                        } else if (_callScreenState.value == CallScreenState.INCOMING || _callScreenState.value == CallScreenState.DIALING) {
                            _callScreenState.value = CallScreenState.IDLE
                            stopCallTimers()
                        }
                    }
                    CallState.IDLE -> {}
                }
            }
        }
    }

    private fun observeTwilioCalls() {
        viewModelScope.launch {
            twilioService.currentCall.collect { twilioCall ->
                if (twilioCall == null) return@collect
                val realState = twilioCall.state
                _callState.value = realState
                _activeCall.update {
                    it.copy(
                        callState = realState,
                        rawTwilioStatus = twilioCall.rawTwilioStatus,
                        twilioCallSid = twilioCall.callSid,
                        isTwilioCall = true
                    )
                }
                when (realState) {
                    CallState.CONNECTING, CallState.RINGING -> {
                        if (_callScreenState.value != CallScreenState.ACTIVE) {
                            _callScreenState.value = CallScreenState.DIALING
                        }
                    }
                    CallState.ACTIVE -> {
                        if (_callScreenState.value != CallScreenState.ACTIVE) {
                            _activeCall.update {
                                it.copy(
                                    isTwilioCall = true,
                                    twilioCallSid = twilioCall.callSid,
                                    callState = CallState.ACTIVE
                                )
                            }
                            transitionToActiveCall()
                        }
                    }
                    CallState.HOLDING -> {
                        _activeCall.update { it.copy(isHold = true) }
                    }
                    CallState.DISCONNECTED -> {
                        if (_callScreenState.value == CallScreenState.ACTIVE) {
                            endActiveCall()
                        } else if (_callScreenState.value == CallScreenState.DIALING || _callScreenState.value == CallScreenState.INCOMING) {
                            _callScreenState.value = CallScreenState.IDLE
                            stopCallTimers()
                        }
                    }
                    CallState.IDLE -> {}
                }
            }
        }
    }

    private fun observeTranscriber() {
        viewModelScope.launch {
            transcriber.transcriptFlow.collect { segment ->
                addTranscriptTurn(segment)
            }
        }
        viewModelScope.launch {
            mediaStreamBridge.transcriptFlow.collect { segment ->
                addTranscriptTurn(segment)
            }
        }
        viewModelScope.launch {
            transcriber.state.collect { tState ->
                _activeCall.update { it.copy(transcriberState = tState) }
            }
        }
        viewModelScope.launch {
            mediaStreamBridge.streamState.collect { sState ->
                val mapped = when (sState) {
                    com.example.engine.MediaStreamState.STREAMING -> TranscriberState.LISTENING
                    com.example.engine.MediaStreamState.CONNECTING -> TranscriberState.CONNECTING
                    com.example.engine.MediaStreamState.ERROR -> TranscriberState.ERROR
                    com.example.engine.MediaStreamState.STOPPED -> TranscriberState.STOPPED
                    com.example.engine.MediaStreamState.IDLE -> TranscriberState.IDLE
                }
                _activeCall.update { it.copy(transcriberState = mapped) }
            }
        }
        viewModelScope.launch {
            transcriber.audioAmplitude.collect { amp ->
                if (amp > 0f || _activeCall.value.audioWaveformAmp == 0f) {
                    _activeCall.update { it.copy(audioWaveformAmp = amp) }
                }
            }
        }
        viewModelScope.launch {
            mediaStreamBridge.audioAmplitude.collect { amp ->
                if (amp > 0f) {
                    _activeCall.update { it.copy(audioWaveformAmp = amp) }
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
        val numberToCall = targetNumber.ifBlank { knownPerson?.phoneNumber ?: "" }
        if (numberToCall.isBlank()) return

        val person = knownPerson ?: people.value.firstOrNull {
            val cleanA = it.phoneNumber.replace("[^0-9]".toRegex(), "")
            val cleanB = numberToCall.replace("[^0-9]".toRegex(), "")
            cleanA.isNotEmpty() && (cleanA == cleanB || cleanA.endsWith(cleanB) || cleanB.endsWith(cleanA))
        }

        _activeCall.value = ActiveCallState(
            caller = person,
            phoneNumber = numberToCall,
            elapsedSeconds = 0,
            objective = person?.currentTopics?.ifBlank { "Direct Consultation" } ?: "Direct Consultation",
            transcript = emptyList(),
            isTwilioCall = false
        )
        _callScreenState.value = CallScreenState.DIALING

        val twilioSid = cryptoManager.getTwilioSid()
        val twilioToken = cryptoManager.getTwilioToken()
        val twilioPhone = cryptoManager.getTwilioPhoneNumber()

        val isDefault = isDefaultPhoneApp.value

        if (isDefault) {
            // Native default phone dialer via Android Telecom
            val callPlaced = CallManager.placeOutgoingCall(getApplication(), numberToCall)
            if (!callPlaced && twilioSid.isNotBlank() && twilioPhone.isNotBlank()) {
                // Fallback to Twilio Voice
                placeTwilioCall(twilioSid, twilioToken, twilioPhone, numberToCall)
            }
        } else if (twilioSid.isNotBlank() && twilioPhone.isNotBlank()) {
            // Programmable Twilio Call
            placeTwilioCall(twilioSid, twilioToken, twilioPhone, numberToCall)
        } else {
            // Trigger Android Telecom call
            val callPlaced = CallManager.placeOutgoingCall(getApplication(), numberToCall)
            if (!callPlaced) {
                Log.w(TAG, "Neither Default Phone role nor Twilio credentials configured.")
            }
        }
    }

    private fun placeTwilioCall(sid: String, token: String, from: String, to: String) {
        val accessToken = cryptoManager.getTwilioAccessToken()
        if (accessToken.isNotBlank()) {
            val service = CallService.getInstance()
            if (service != null) {
                _activeCall.update { it.copy(isTwilioCall = true, callState = CallState.CONNECTING) }
                val params = mapOf("To" to to, "From" to from)
                val sdkCall = service.connectTwilioSdkCall(accessToken, params)
                if (sdkCall != null) {
                    _activeCall.update { it.copy(twilioCallSid = sdkCall.sid) }
                    return
                }
            }
        }

        viewModelScope.launch {
            _activeCall.update { it.copy(isTwilioCall = true) }
            val result = twilioService.placeCall(
                accountSid = sid,
                authToken = token,
                fromNumber = from,
                toNumber = to
            )
            result.onFailure { err ->
                Log.e(TAG, "Twilio call error: ${err.message}")
            }
        }
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

        // Start Deepgram live transcription if enabled and key is present
        val deepgramKey = cryptoManager.getDeepgramKey()
        val mediaStreamUrl = cryptoManager.getTwilioMediaStreamUrl()

        if (cryptoManager.isLiveTranscriptionEnabled && deepgramKey.isNotBlank()) {
            if (mediaStreamUrl.isNotBlank()) {
                mediaStreamBridge.startMediaStreamPipe(mediaStreamUrl, deepgramKey)
            } else {
                transcriber.startStreaming(deepgramKey)
            }
        }

        // Initialize initial active copilot assessment
        viewModelScope.launch {
            val initialResult = copilotEngine.analyzeConversationTurn(
                caller = _activeCall.value.caller,
                transcriptHistory = emptyList(),
                latestUtterance = null,
                knownClaims = emptyList(),
                knownMemories = emptyList(),
                objective = _activeCall.value.objective
            )
            _activeCall.update { it.copy(copilotResult = initialResult) }
        }
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
                    activeInconsistency = result.inconsistencyAlert ?: it.activeInconsistency,
                    selectedAlternative = null,
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
            id = UUID.randomUUID().toString(),
            speaker = if (isYou) Speaker.YOU else Speaker.OTHER,
            speakerName = name,
            text = text.trim(),
            timestamp = timeFormatted,
            isFinal = true,
            linguisticDistance = 0.2f,
            stressLevel = 0.2f
        )
        addTranscriptTurn(segment)
    }

    fun selectStrategy(strat: StrategyRecommendation) {
        _activeCall.update {
            it.copy(
                selectedStrategy = strat,
                selectedAlternative = StrategyAlternative(
                    strategy = strat.type,
                    suggestedResponse = strat.suggestedResponse,
                    tone = strat.tone
                )
            )
        }
    }

    fun selectAlternativeStrategy(alt: StrategyAlternative) {
        _activeCall.update { it.copy(selectedAlternative = alt) }
    }

    fun resetSelectedAlternative() {
        _activeCall.update { it.copy(selectedAlternative = null, selectedStrategy = null) }
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

    fun toggleMute() {
        val newMute = !_activeCall.value.isMuted
        CallManager.setMuted(newMute)
        CallService.toggleMute()
        _activeCall.update { it.copy(isMuted = newMute) }
    }

    fun toggleSpeaker() {
        val newSpeaker = !_activeCall.value.isSpeakerOn
        CallManager.setSpeakerphone(newSpeaker)
        CallService.toggleSpeaker(getApplication())
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
            val digit = char[0]
            CallManager.playDtmf(digit)
            CallService.sendDtmf(digit)

            val activeTwilio = _activeCall.value.twilioCallSid
            if (activeTwilio != null) {
                val sid = cryptoManager.getTwilioSid()
                val token = cryptoManager.getTwilioToken()
                viewModelScope.launch {
                    twilioService.sendDtmf(sid, token, activeTwilio, char)
                }
            }
        }
        _activeCall.update { it.copy(keypadDtmf = it.keypadDtmf + char) }
    }

    fun endActiveCall() {
        transcriber.stopStreaming()
        mediaStreamBridge.stopMediaStreamPipe()
        CallService.disconnectCall()

        val current = _activeCall.value
        if (current.isTwilioCall && current.twilioCallSid != null) {
            val sid = cryptoManager.getTwilioSid()
            val token = cryptoManager.getTwilioToken()
            viewModelScope.launch {
                twilioService.endCall(sid, token, current.twilioCallSid)
            }
        } else {
            CallManager.endCall()
        }

        stopCallTimers()

        val durationFormatted = formatCallDuration(current.elapsedSeconds)
        val participantName = current.caller?.name ?: "Unknown (${current.phoneNumber})"

        val summary = CallSummaryPayload(
            participants = "$participantName, You",
            durationFormatted = durationFormatted,
            durationSeconds = current.elapsedSeconds,
            topics = current.objective,
            importantStatements = if (current.transcript.isNotEmpty()) {
                current.transcript.joinToString("; ") { "${it.speaker}: \"${it.text}\"" }
            } else "Call completed.",
            claims = current.copilotResult?.inconsistencyAlert?.let {
                "Inconsistency flagged: ${it.currentStatement} (vs baseline: ${it.previousStatement})"
            } ?: "No verified conflicting claims flagged.",
            commitments = current.caller?.recentCommitment?.ifBlank { "Follow up as required" } ?: "Follow up as required",
            questionsAnswered = "Call consultation concluded.",
            questionsUnresolved = current.caller?.openQuestions ?: "None recorded",
            potentialInconsistencies = current.copilotResult?.inconsistencyAlert?.let {
                "Previous: ${it.previousStatement} vs Current: ${it.currentStatement} (${it.confidence}% confidence)"
            } ?: "None detected",
            deceptionSignalsSummary = "Composite signal average: ${current.copilotResult?.deceptionSignal?.score ?: 0}%",
            newMemoriesCreated = current.activeMemoryAlert?.statement ?: "None",
            recommendedFollowUps = "Review call intelligence notes.",
            strategiesUsed = current.copilotResult?.recommendedStrategy?.displayName ?: "CONVERSATION COPILOT"
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
                    callType = if (current.isTwilioCall) "TWILIO_VOICE" else "OUTGOING",
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
                    deceptionAvgScore = current.copilotResult?.deceptionSignal?.score ?: 0,
                    transcriptJson = ""
                )
            )

            // If an inconsistency claim was flagged, save it
            current.copilotResult?.inconsistencyAlert?.let { inc ->
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
    }

    // Contacts & Device Sync
    fun syncContactsFromDevice() {
        viewModelScope.launch {
            _syncStatusMessage.value = "Scanning device contacts..."
            try {
                val count = contactsManager.syncDeviceContactsToRoom()
                _syncStatusMessage.value = "Synced $count new contacts from device."
                delay(3000)
                _syncStatusMessage.value = null
            } catch (e: Exception) {
                _syncStatusMessage.value = "Sync failed: ${e.message}"
                delay(3000)
                _syncStatusMessage.value = null
            }
        }
    }

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
