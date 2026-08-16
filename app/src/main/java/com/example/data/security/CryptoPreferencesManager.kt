package com.example.data.security

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Credentials
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class CryptoPreferencesManager(private val context: Context) {

    private val masterKey: MasterKey by lazy {
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }

    private val prefs: SharedPreferences by lazy {
        try {
            EncryptedSharedPreferences.create(
                context,
                SECURE_PREFS_FILE,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error opening EncryptedSharedPreferences, resetting secure vault: ${e.message}")
            context.getSharedPreferences(SECURE_PREFS_FILE, Context.MODE_PRIVATE)
                .edit().clear().apply()
            EncryptedSharedPreferences.create(
                context,
                SECURE_PREFS_FILE,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        }
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    // Encrypted API Credentials Getters & Setters
    fun saveTwilioSid(sid: String) {
        prefs.edit().putString(PREF_TWILIO_SID, sid.trim()).apply()
    }
    fun getTwilioSid(): String = prefs.getString(PREF_TWILIO_SID, "") ?: ""

    fun saveTwilioToken(token: String) {
        prefs.edit().putString(PREF_TWILIO_TOKEN, token.trim()).apply()
    }
    fun getTwilioToken(): String = prefs.getString(PREF_TWILIO_TOKEN, "") ?: ""

    fun saveTwilioPhoneNumber(number: String) {
        prefs.edit().putString(PREF_TWILIO_PHONE, number.trim()).apply()
    }
    fun getTwilioPhoneNumber(): String = prefs.getString(PREF_TWILIO_PHONE, "") ?: ""

    fun saveDeepgramKey(apiKey: String) {
        prefs.edit().putString(PREF_DEEPGRAM_KEY, apiKey.trim()).apply()
    }
    fun getDeepgramKey(): String = prefs.getString(PREF_DEEPGRAM_KEY, "") ?: ""

    fun saveGroqKey(apiKey: String) {
        prefs.edit().putString(PREF_GROQ_KEY, apiKey.trim()).apply()
    }
    fun getGroqKey(): String = prefs.getString(PREF_GROQ_KEY, "") ?: ""

    fun saveGroqModel(model: String) {
        prefs.edit().putString(PREF_GROQ_MODEL, model.trim()).apply()
    }
    fun getGroqModel(): String =
        prefs.getString(PREF_GROQ_MODEL, "llama-3.1-8b-instant") ?: "llama-3.1-8b-instant"

    fun saveSupabaseUrl(url: String) {
        prefs.edit().putString(PREF_SUPABASE_URL, url.trim()).apply()
    }
    fun getSupabaseUrl(): String = prefs.getString(PREF_SUPABASE_URL, "") ?: ""

    fun saveSupabaseAnonKey(anonKey: String) {
        prefs.edit().putString(PREF_SUPABASE_KEY, anonKey.trim()).apply()
    }
    fun getSupabaseAnonKey(): String = prefs.getString(PREF_SUPABASE_KEY, "") ?: ""

    fun clearAllCredentials() {
        prefs.edit()
            .remove(PREF_TWILIO_SID)
            .remove(PREF_TWILIO_TOKEN)
            .remove(PREF_TWILIO_PHONE)
            .remove(PREF_DEEPGRAM_KEY)
            .remove(PREF_GROQ_KEY)
            .remove(PREF_GROQ_MODEL)
            .remove(PREF_SUPABASE_URL)
            .remove(PREF_SUPABASE_KEY)
            .apply()
    }

    fun hasTwilioConfig(): Boolean = getTwilioSid().isNotBlank() && getTwilioToken().isNotBlank()
    fun hasDeepgramConfig(): Boolean = getDeepgramKey().isNotBlank()
    fun hasGroqConfig(): Boolean = getGroqKey().isNotBlank()
    fun hasSupabaseConfig(): Boolean = getSupabaseUrl().isNotBlank() && getSupabaseAnonKey().isNotBlank()

    // Mask helper
    fun maskSecret(secret: String): String {
        if (secret.isBlank()) return ""
        if (secret.length <= 6) return "••••••"
        val prefix = secret.take(3)
        val suffix = secret.takeLast(3)
        return "$prefix••••••••$suffix"
    }

    // Engine feature toggles
    var isAiAnalysisEnabled: Boolean
        get() = prefs.getBoolean(PREF_AI_ANALYSIS, true)
        set(value) = prefs.edit().putBoolean(PREF_AI_ANALYSIS, value).apply()

    var isDeceptionSignalEnabled: Boolean
        get() = prefs.getBoolean(PREF_DECEPTION_SIGNAL, true)
        set(value) = prefs.edit().putBoolean(PREF_DECEPTION_SIGNAL, value).apply()

    var isLiveTranscriptionEnabled: Boolean
        get() = prefs.getBoolean(PREF_LIVE_TRANSCRIPTION, true)
        set(value) = prefs.edit().putBoolean(PREF_LIVE_TRANSCRIPTION, value).apply()

    var isMemorySystemEnabled: Boolean
        get() = prefs.getBoolean(PREF_MEMORY, true)
        set(value) = prefs.edit().putBoolean(PREF_MEMORY, value).apply()

    var isAutomaticStrategyEnabled: Boolean
        get() = prefs.getBoolean(PREF_AUTO_STRATEGY, true)
        set(value) = prefs.edit().putBoolean(PREF_AUTO_STRATEGY, value).apply()

    var isAutoPostCallSummaryEnabled: Boolean
        get() = prefs.getBoolean(PREF_AUTO_SUMMARY, true)
        set(value) = prefs.edit().putBoolean(PREF_AUTO_SUMMARY, value).apply()

    // Real API Test Probes
    suspend fun testTwilio(sid: String, token: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val cleanSid = sid.trim()
            val cleanToken = token.trim()
            if (cleanSid.isBlank() || cleanToken.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("Account SID and Auth Token are required"))
            }
            if (!cleanSid.startsWith("AC")) {
                return@withContext Result.failure(IllegalArgumentException("Account SID must start with 'AC'"))
            }

            val credential = Credentials.basic(cleanSid, cleanToken)
            val request = Request.Builder()
                .url("https://api.twilio.com/2010-04-01/Accounts/$cleanSid.json")
                .header("Authorization", credential)
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Result.success("Twilio Connected: Account verified active (HTTP ${response.code})")
                } else {
                    Result.failure(Exception("Twilio Auth Failed: HTTP ${response.code} (${response.message})"))
                }
            }
        } catch (e: Exception) {
            Result.failure(Exception("Twilio Connection Error: ${e.localizedMessage ?: e.message}"))
        }
    }

    suspend fun testDeepgram(apiKey: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val cleanKey = apiKey.trim()
            if (cleanKey.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("Deepgram API Key is required"))
            }
            val request = Request.Builder()
                .url("https://api.deepgram.com/v1/projects")
                .header("Authorization", "Token $cleanKey")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Result.success("Deepgram Connected: Nova-2 API verified (HTTP ${response.code})")
                } else {
                    Result.failure(Exception("Deepgram Auth Failed: HTTP ${response.code} (${response.message})"))
                }
            }
        } catch (e: Exception) {
            Result.failure(Exception("Deepgram Connection Error: ${e.localizedMessage ?: e.message}"))
        }
    }

    suspend fun testGroq(apiKey: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val cleanKey = apiKey.trim()
            if (cleanKey.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("Groq API Key is required"))
            }
            val request = Request.Builder()
                .url("https://api.groq.com/openai/v1/models")
                .header("Authorization", "Bearer $cleanKey")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Result.success("Groq Connected: Llama-3.1 inference engine ready (HTTP ${response.code})")
                } else {
                    Result.failure(Exception("Groq Auth Failed: HTTP ${response.code} (${response.message})"))
                }
            }
        } catch (e: Exception) {
            Result.failure(Exception("Groq Connection Error: ${e.localizedMessage ?: e.message}"))
        }
    }

    suspend fun testSupabase(url: String, anonKey: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val cleanUrl = url.trim()
            val cleanKey = anonKey.trim()
            if (cleanUrl.isBlank() || cleanKey.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("Supabase URL and Anon Key are required"))
            }
            val normalizedUrl = if (cleanUrl.endsWith("/")) cleanUrl else "$cleanUrl/"
            val request = Request.Builder()
                .url("${normalizedUrl}rest/v1/")
                .header("apikey", cleanKey)
                .header("Authorization", "Bearer $cleanKey")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful || response.code == 404 || response.code == 200) {
                    Result.success("Supabase Connected: REST endpoint accessible (HTTP ${response.code})")
                } else {
                    Result.failure(Exception("Supabase Auth Failed: HTTP ${response.code} (${response.message})"))
                }
            }
        } catch (e: Exception) {
            Result.failure(Exception("Supabase Connection Error: ${e.localizedMessage ?: e.message}"))
        }
    }

    companion object {
        private const val TAG = "CryptoPreferences"
        private const val SECURE_PREFS_FILE = "reality_engine_secure_vault"

        private const val PREF_TWILIO_SID = "enc_twilio_sid"
        private const val PREF_TWILIO_TOKEN = "enc_twilio_token"
        private const val PREF_TWILIO_PHONE = "enc_twilio_phone"

        private const val PREF_DEEPGRAM_KEY = "enc_deepgram_key"
        private const val PREF_GROQ_KEY = "enc_groq_key"
        private const val PREF_GROQ_MODEL = "groq_model"

        private const val PREF_SUPABASE_URL = "enc_supabase_url"
        private const val PREF_SUPABASE_KEY = "enc_supabase_key"

        private const val PREF_AI_ANALYSIS = "toggle_ai_analysis"
        private const val PREF_DECEPTION_SIGNAL = "toggle_deception_signal"
        private const val PREF_LIVE_TRANSCRIPTION = "toggle_live_transcription"
        private const val PREF_MEMORY = "toggle_memory"
        private const val PREF_AUTO_STRATEGY = "toggle_auto_strategy"
        private const val PREF_AUTO_SUMMARY = "toggle_auto_summary"
    }
}
