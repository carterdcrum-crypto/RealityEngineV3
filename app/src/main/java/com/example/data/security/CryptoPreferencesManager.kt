package com.example.data.security

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.security.KeyStore
import java.util.concurrent.TimeUnit
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class CryptoPreferencesManager(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("reality_engine_secure_prefs", Context.MODE_PRIVATE)

    private val keyStore: KeyStore = KeyStore.getInstance("AndroidKeyStore").apply {
        load(null)
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    init {
        ensureKeyExists()
    }

    private fun ensureKeyExists() {
        if (!keyStore.containsAlias(KEY_ALIAS)) {
            val keyGenerator =
                KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
            val keyGenSpec = KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
            keyGenerator.init(keyGenSpec)
            keyGenerator.generateKey()
        }
    }

    private fun encrypt(plainText: String): String {
        if (plainText.isEmpty()) return ""
        try {
            val secretKey = keyStore.getKey(KEY_ALIAS, null) as SecretKey
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            val iv = cipher.iv
            val cipherText = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
            val combined = ByteArray(iv.size + cipherText.size)
            System.arraycopy(iv, 0, combined, 0, iv.size)
            System.arraycopy(cipherText, 0, combined, iv.size, cipherText.size)
            return Base64.encodeToString(combined, Base64.NO_WRAP)
        } catch (e: Exception) {
            Log.e(TAG, "Encryption error: ${e.message}", e)
            return ""
        }
    }

    private fun decrypt(encryptedBase64: String): String {
        if (encryptedBase64.isEmpty()) return ""
        try {
            val combined = Base64.decode(encryptedBase64, Base64.NO_WRAP)
            if (combined.size < 12) return ""
            val iv = ByteArray(12)
            val cipherText = ByteArray(combined.size - 12)
            System.arraycopy(combined, 0, iv, 0, 12)
            System.arraycopy(combined, 12, cipherText, 0, cipherText.size)

            val secretKey = keyStore.getKey(KEY_ALIAS, null) as SecretKey
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val spec = GCMParameterSpec(128, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
            val plainTextBytes = cipher.doFinal(cipherText)
            return String(plainTextBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            Log.e(TAG, "Decryption error: ${e.message}", e)
            return ""
        }
    }

    // Encrypted API Credentials Getters & Setters
    fun saveTwilioSid(sid: String) {
        prefs.edit().putString(PREF_TWILIO_SID, encrypt(sid.trim())).apply()
    }
    fun getTwilioSid(): String = decrypt(prefs.getString(PREF_TWILIO_SID, "") ?: "")

    fun saveTwilioToken(token: String) {
        prefs.edit().putString(PREF_TWILIO_TOKEN, encrypt(token.trim())).apply()
    }
    fun getTwilioToken(): String = decrypt(prefs.getString(PREF_TWILIO_TOKEN, "") ?: "")

    fun saveTwilioPhoneNumber(number: String) {
        prefs.edit().putString(PREF_TWILIO_PHONE, encrypt(number.trim())).apply()
    }
    fun getTwilioPhoneNumber(): String = decrypt(prefs.getString(PREF_TWILIO_PHONE, "") ?: "")

    fun saveDeepgramKey(apiKey: String) {
        prefs.edit().putString(PREF_DEEPGRAM_KEY, encrypt(apiKey.trim())).apply()
    }
    fun getDeepgramKey(): String = decrypt(prefs.getString(PREF_DEEPGRAM_KEY, "") ?: "")

    fun saveGroqKey(apiKey: String) {
        prefs.edit().putString(PREF_GROQ_KEY, encrypt(apiKey.trim())).apply()
    }
    fun getGroqKey(): String = decrypt(prefs.getString(PREF_GROQ_KEY, "") ?: "")

    fun saveGroqModel(model: String) {
        prefs.edit().putString(PREF_GROQ_MODEL, model.trim()).apply()
    }
    fun getGroqModel(): String =
        prefs.getString(PREF_GROQ_MODEL, "meta/llama-3.1-8b-instant") ?: "meta/llama-3.1-8b-instant"

    fun saveSupabaseUrl(url: String) {
        prefs.edit().putString(PREF_SUPABASE_URL, encrypt(url.trim())).apply()
    }
    fun getSupabaseUrl(): String = decrypt(prefs.getString(PREF_SUPABASE_URL, "") ?: "")

    fun saveSupabaseAnonKey(anonKey: String) {
        prefs.edit().putString(PREF_SUPABASE_KEY, encrypt(anonKey.trim())).apply()
    }
    fun getSupabaseAnonKey(): String = decrypt(prefs.getString(PREF_SUPABASE_KEY, "") ?: "")

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
            if (sid.isBlank() || token.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("Account SID and Token are required"))
            }
            val credential = okhttp3.Credentials.basic(sid, token)
            val request = Request.Builder()
                .url("https://api.twilio.com/2010-04-01/Accounts/$sid.json")
                .header("Authorization", credential)
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Result.success("Twilio Connected: Account active (${response.code})")
                } else {
                    Result.failure(Exception("Twilio Auth Failed: HTTP ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(Exception("Twilio Connection Error: ${e.localizedMessage}"))
        }
    }

    suspend fun testDeepgram(apiKey: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (apiKey.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("Deepgram API Key is required"))
            }
            val request = Request.Builder()
                .url("https://api.deepgram.com/v1/projects")
                .header("Authorization", "Token $apiKey")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Result.success("Deepgram Connected: API Key valid (${response.code})")
                } else {
                    Result.failure(Exception("Deepgram Auth Failed: HTTP ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(Exception("Deepgram Connection Error: ${e.localizedMessage}"))
        }
    }

    suspend fun testGroq(apiKey: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (apiKey.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("Groq API Key is required"))
            }
            val request = Request.Builder()
                .url("https://api.groq.com/openai/v1/models")
                .header("Authorization", "Bearer $apiKey")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Result.success("Groq Connected: Llama-3.1 API ready (${response.code})")
                } else {
                    Result.failure(Exception("Groq Auth Failed: HTTP ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(Exception("Groq Connection Error: ${e.localizedMessage}"))
        }
    }

    suspend fun testSupabase(url: String, anonKey: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (url.isBlank() || anonKey.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("Supabase URL and Anon Key are required"))
            }
            val normalizedUrl = if (url.endsWith("/")) url else "$url/"
            val request = Request.Builder()
                .url("${normalizedUrl}rest/v1/")
                .header("apikey", anonKey)
                .header("Authorization", "Bearer $anonKey")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful || response.code == 404 || response.code == 200) {
                    Result.success("Supabase Connected: Endpoint reached (${response.code})")
                } else {
                    Result.failure(Exception("Supabase Auth Failed: HTTP ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(Exception("Supabase Connection Error: ${e.localizedMessage}"))
        }
    }

    companion object {
        private const val TAG = "CryptoPreferences"
        private const val KEY_ALIAS = "RealityEngineKeyStoreKeyV2"

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
