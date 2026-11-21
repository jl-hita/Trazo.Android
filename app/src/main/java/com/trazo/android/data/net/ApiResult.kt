package com.trazo.android.data.net

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import retrofit2.Response
import java.io.IOException

enum class ErrorKind { Network, Unauthorized, SubscriptionRequired, NotFound, Conflict, Validation, Server, Unknown }

/**
 * An API or transport failure. [code] is the stable code the backend puts in the problem `title`
 * (`no_active_plan`, `session_already_logged`...) or in `error`; the UI turns it into Spanish text.
 */
data class ApiError(
    val kind: ErrorKind,
    val code: String? = null,
    val serverMessage: String? = null,
    val fields: Map<String, List<String>> = emptyMap(),
    val httpStatus: Int? = null
) {
    /** True when sending the same request again later may succeed. */
    val retryable: Boolean get() = kind == ErrorKind.Network || kind == ErrorKind.Server
}

sealed interface ApiResult<out T> {
    data class Success<T>(val value: T) : ApiResult<T>
    data class Failure(val error: ApiError) : ApiResult<Nothing>
}

inline fun <T, R> ApiResult<T>.map(transform: (T) -> R): ApiResult<R> = when (this) {
    is ApiResult.Success -> ApiResult.Success(transform(value))
    is ApiResult.Failure -> this
}

fun <T> ApiResult<T>.getOrNull(): T? = (this as? ApiResult.Success)?.value

/** Runs a Retrofit call and turns every outcome into an [ApiResult]; it never throws for network or HTTP errors. */
suspend fun <T> apiCall(block: suspend () -> Response<T>): ApiResult<T> {
    val response = try {
        block()
    } catch (e: kotlinx.coroutines.CancellationException) {
        throw e
    } catch (_: IOException) {
        return ApiResult.Failure(ApiError(ErrorKind.Network))
    } catch (e: RuntimeException) {
        com.trazo.android.diagnostics.Diagnostics.report(e)
        // Malformed body (gson) or similar: a retry will not fix it, but it must not crash the app either.
        return ApiResult.Failure(ApiError(ErrorKind.Unknown))
    }
    if (response.isSuccessful) {
        @Suppress("UNCHECKED_CAST")
        val body = response.body() ?: (if (response.code() == 204 || response.code() == 205) Unit as T else null)
        return if (body != null) ApiResult.Success(body) else ApiResult.Failure(ApiError(ErrorKind.Unknown, httpStatus = response.code()))
    }
    return ApiResult.Failure(parseError(response.code(), response.errorBody()?.use { runCatching { it.string() }.getOrNull() }))
}

/**
 * Same as [apiCall], but this endpoint's successful contract requires a response body.
 * In particular, never manufacture `Unit as T` for a 204/205 response: the caller may
 * subsequently cast that value to its DTO and crash outside the transport error boundary.
 */
suspend fun <T> apiCallWithBody(operation: String, block: suspend () -> Response<T>): ApiResult<T> {
    val response = try {
        block()
    } catch (e: kotlinx.coroutines.CancellationException) {
        throw e
    } catch (_: IOException) {
        return ApiResult.Failure(ApiError(ErrorKind.Network))
    } catch (e: RuntimeException) {
        com.trazo.android.diagnostics.Diagnostics.report(e)
        return ApiResult.Failure(ApiError(ErrorKind.Unknown))
    }
    if (!response.isSuccessful) {
        return ApiResult.Failure(parseError(response.code(), response.errorBody()?.use { runCatching { it.string() }.getOrNull() }))
    }
    val body = response.body()
    if (body != null) return ApiResult.Success(body)
    val error = IllegalStateException("$operation expected a response body but received HTTP ${response.code()}.")
    com.trazo.android.diagnostics.Diagnostics.report(error)
    return ApiResult.Failure(ApiError(ErrorKind.Unknown, code = "empty_response", httpStatus = response.code()))
}

fun parseError(status: Int, body: String?): ApiError {
    val json: JsonObject? = runCatching { JsonParser.parseString(body.orEmpty()).asJsonObject }.getOrNull()
    fun text(name: String) = json?.get(name)?.takeIf { it.isJsonPrimitive }?.asString
    val title = text("title")
    // Problem titles are stable codes for plan errors; validation problems carry a sentence, which is not a code.
    val code = title?.takeIf { it.isNotBlank() && !it.contains(' ') } ?: text("error")
    val message = text("detail") ?: text("message")
    val fields = runCatching {
        json?.getAsJsonObject("errors")?.entrySet()?.associate { (k, v) -> k to v.asJsonArray.map { it.asString } }
    }.getOrNull().orEmpty()
    val kind = when {
        status == 401 -> ErrorKind.Unauthorized
        status == 402 -> ErrorKind.SubscriptionRequired
        status == 404 -> ErrorKind.NotFound
        status == 409 -> ErrorKind.Conflict
        status == 400 || status == 422 -> ErrorKind.Validation
        status >= 500 -> ErrorKind.Server
        else -> ErrorKind.Unknown
    }
    return ApiError(kind, code, message, fields, status)
}
