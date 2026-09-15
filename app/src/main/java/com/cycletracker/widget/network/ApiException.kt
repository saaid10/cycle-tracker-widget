package com.cycletracker.widget.network

sealed class ApiException(message: String) : Exception(message) {
    object Unauthorized : ApiException("Unauthorized")
    object NetworkFailure : ApiException("Network failure")
    data class ServerError(val code: Int, val detail: String) : ApiException("Server error $code: $detail")
}
