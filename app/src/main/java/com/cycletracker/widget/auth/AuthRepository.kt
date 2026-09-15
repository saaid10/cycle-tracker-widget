package com.cycletracker.widget.auth

import android.content.Context
import com.cycletracker.widget.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

sealed class AuthResult {
    data class Success(val accessToken: String) : AuthResult()
    data class InvalidCredentials(val message: String) : AuthResult()
    object NetworkError : AuthResult()
}

/**
 * Talks to Supabase's GoTrue auth endpoint directly (this app has no backend
 * of its own beyond the existing cycle-tracker Next.js API) and keeps the
 * session valid for [com.cycletracker.widget.network.WidgetApiClient] calls.
 */
class AuthRepository(context: Context) {

    private val appContext = context.applicationContext
    private val tokenStore = TokenStore(appContext)
    private val json = Json { ignoreUnknownKeys = true }
    private val httpClient = OkHttpClient()

    @Serializable
    private data class TokenResponse(
        val access_token: String,
        val refresh_token: String,
        val expires_in: Long
    )

    @Serializable
    private data class PasswordGrantBody(val email: String, val password: String)

    @Serializable
    private data class RefreshGrantBody(val refresh_token: String)

    private val jsonMediaType = "application/json".toMediaType()

    suspend fun signIn(email: String, password: String): AuthResult = withContext(Dispatchers.IO) {
        val body = json.encodeToString(PasswordGrantBody.serializer(), PasswordGrantBody(email, password))
        performGrant("password", body)
    }

    /**
     * Returns a still-valid access token, refreshing first if there's less
     * than 60s left on the current one. Returns null when there is no
     * session, or the refresh token itself is invalid/revoked (caller should
     * treat that as "needs login") -- as opposed to a transient network
     * failure, which leaves the stored session untouched.
     */
    suspend fun getValidAccessToken(): String? = withContext(Dispatchers.IO) {
        val session = tokenStore.load() ?: return@withContext null
        val hasSixtySecondsLeft = session.expiresAtEpochMillis - System.currentTimeMillis() > 60_000
        if (hasSixtySecondsLeft) return@withContext session.accessToken

        when (val result = refresh(session.refreshToken)) {
            is AuthResult.Success -> result.accessToken
            is AuthResult.InvalidCredentials -> {
                tokenStore.clear()
                null
            }
            AuthResult.NetworkError -> null
        }
    }

    /** Forces a refresh regardless of the cached expiry -- used after a 401 from the API. */
    suspend fun forceRefresh(): AuthResult = withContext(Dispatchers.IO) {
        val session = tokenStore.load() ?: return@withContext AuthResult.InvalidCredentials("No session")
        val result = refresh(session.refreshToken)
        if (result is AuthResult.InvalidCredentials) tokenStore.clear()
        result
    }

    fun isSignedIn(): Boolean = tokenStore.load() != null

    fun signOut() {
        tokenStore.clear()
    }

    private suspend fun refresh(refreshToken: String): AuthResult {
        val body = json.encodeToString(RefreshGrantBody.serializer(), RefreshGrantBody(refreshToken))
        return performGrant("refresh_token", body)
    }

    private fun performGrant(grantType: String, jsonBody: String): AuthResult {
        val request = Request.Builder()
            .url("${BuildConfig.SUPABASE_URL}/auth/v1/token?grant_type=$grantType")
            .addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY)
            .post(jsonBody.toRequestBody(jsonMediaType))
            .build()

        return try {
            httpClient.newCall(request).execute().use { response ->
                val responseBody = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return if (response.code == 400 || response.code == 401) {
                        AuthResult.InvalidCredentials(responseBody)
                    } else {
                        AuthResult.NetworkError
                    }
                }
                val token = json.decodeFromString(TokenResponse.serializer(), responseBody)
                tokenStore.save(
                    TokenStore.Session(
                        accessToken = token.access_token,
                        refreshToken = token.refresh_token,
                        expiresAtEpochMillis = System.currentTimeMillis() + token.expires_in * 1000
                    )
                )
                AuthResult.Success(token.access_token)
            }
        } catch (e: IOException) {
            AuthResult.NetworkError
        }
    }
}
