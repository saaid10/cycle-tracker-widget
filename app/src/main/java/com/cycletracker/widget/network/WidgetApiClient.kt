package com.cycletracker.widget.network

import android.content.Context
import com.cycletracker.widget.BuildConfig
import com.cycletracker.widget.auth.AuthRepository
import com.cycletracker.widget.auth.AuthResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

// Calls the existing cycle-tracker Next.js app's /api/widget/* routes.
class WidgetApiClient(context: Context) {

    private val authRepository = AuthRepository(context)
    private val httpClient = OkHttpClient()
    private val json = Json { ignoreUnknownKeys = true }
    private val jsonMediaType = "application/json".toMediaType()

    suspend fun getStatus(): WidgetStatus = callWithAuth { token ->
        Request.Builder()
            .url("${BuildConfig.API_BASE_URL}/api/widget/status")
            .addHeader("Authorization", "Bearer $token")
            .get()
            .build()
    }

    suspend fun logFlow(flowIntensity: String, symptoms: List<String>?): WidgetStatus {
        val bodyJson = json.encodeToString(
            LogRequest.serializer(),
            LogRequest(flowIntensity = flowIntensity, symptoms = symptoms)
        )
        return callWithAuth { token ->
            Request.Builder()
                .url("${BuildConfig.API_BASE_URL}/api/widget/log")
                .addHeader("Authorization", "Bearer $token")
                .post(bodyJson.toRequestBody(jsonMediaType))
                .build()
        }
    }

    /** Attaches the current access token; on a 401, forces exactly one refresh + retry. */
    private suspend fun callWithAuth(buildRequest: (accessToken: String) -> Request): WidgetStatus =
        withContext(Dispatchers.IO) {
            val token = authRepository.getValidAccessToken() ?: throw ApiException.Unauthorized

            execute(buildRequest(token))?.let { return@withContext it }

            val refreshResult = authRepository.forceRefresh()
            val refreshedToken = (refreshResult as? AuthResult.Success)?.accessToken
                ?: throw ApiException.Unauthorized

            execute(buildRequest(refreshedToken)) ?: throw ApiException.Unauthorized
        }

    /** Returns null on a 401 (caller retries once), throws for anything else. */
    private fun execute(request: Request): WidgetStatus? {
        try {
            httpClient.newCall(request).execute().use { response ->
                val bodyString = response.body?.string().orEmpty()
                return when {
                    response.code == 401 -> null
                    response.isSuccessful -> json.decodeFromString(WidgetStatus.serializer(), bodyString)
                    else -> throw ApiException.ServerError(response.code, bodyString)
                }
            }
        } catch (e: IOException) {
            throw ApiException.NetworkFailure
        }
    }
}
