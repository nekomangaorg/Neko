package eu.kanade.tachiyomi.util

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.skydoves.sandwich.ApiResponse
import com.skydoves.sandwich.onFailure
import com.skydoves.sandwich.onSuccess
import com.skydoves.sandwich.retrofit.errorBody
import com.skydoves.sandwich.retrofit.statusCode
import eu.kanade.tachiyomi.source.online.models.dto.ErrorResponse
import kotlinx.serialization.json.Json
import org.jsoup.Jsoup
import org.nekomanga.domain.network.ResultError
import org.nekomanga.logging.TimberKt

/**
 * Maps the ApiResponse Error to a Result Error, trying to decode the json response if its a
 * mangadex api error
 */
fun ApiResponse.Failure.Error.toResultError(errorType: String): ResultError {
    val errorBody = this.errorBody?.string() ?: ""

    TimberKt.e {
        """
            error $errorType
            error response code ${this.statusCode.code}
            $errorBody
        """
            .trimIndent()
    }
    val parsedJsoup = Jsoup.parse(this.toString())
    val textOfHtmlString: String = parsedJsoup.text()

    val error =
        when (textOfHtmlString != this.toString()) {
            true ->
                "Http error code: ${this.statusCode.code}. \n'${parsedJsoup.body().select("h1").text()}'"
            false -> {
                val json = Json { ignoreUnknownKeys = true }
                runCatching {
                        val error = json.decodeFromString<ErrorResponse>(errorBody)
                        if (this.statusCode.code == 401) {
                            "Http error code: ${this.statusCode.code}. \n Unable to authenticate, please login"
                        } else {

                            "Http error code: ${this.statusCode.code}. \n'${error.errors.joinToString("\n") { it.detail ?: it.title ?: "no details or titles in error" }}'"
                        }
                    }
                    .getOrElse { "Received http error code: ${this.statusCode.code}" }
            }
        }

    return ResultError.HttpError(this.statusCode.code, error)
}

fun <T> ApiResponse.Failure<T>.toResultError(errorType: String): ResultError {
    return when (this) {
        is ApiResponse.Failure.Error -> this.toResultError(errorType)
        is ApiResponse.Failure.Exception -> this.toResultError(errorType)
    }
}

fun <T> ApiResponse<T>.getOrResultError(errorType: String): Result<T, ResultError> {
    var result: Result<T, ResultError> = Err(ResultError.Generic(errorString = "Unknown Error"))
    this.onFailure { result = Err(this.toResultError(errorType)) }
        .onSuccess { result = Ok(this.data) }
    return result
}

/** Maps the ApiResponse Exception to a Result Error */
fun ApiResponse.Failure.Exception.toResultError(errorType: String): ResultError {
    TimberKt.e(this.throwable) { "Exception $errorType ${this.message}" }

    return ResultError.Generic(errorString = "Unknown Error: '${this.message}'")
}

fun ApiResponse<*>.log(type: String) {
    return when (this) {
        is ApiResponse.Failure.Exception -> {
            TimberKt.e(this.throwable) { "Exception $type ${this.message}" }
        }
        is ApiResponse.Failure.Error -> {
            TimberKt.e {
                """
                    error $type
                    error response code ${this.statusCode.code}
                    ${this.errorBody?.string()}
                """
                    .trimIndent()
            }
        }
        else -> {
            TimberKt.e { "error $type" }
        }
    }
}

fun ApiResponse<*>.throws(type: String) {
    when (this) {
        is ApiResponse.Failure.Error -> {
            throw Exception("Error $type http code: ${this.statusCode.code}")
        }
        is ApiResponse.Failure.Exception -> {
            throw Exception("Error $type ${this.message} ${this.throwable}")
        }
        else -> {
            throw Exception("Error $type ")
        }
    }
}
