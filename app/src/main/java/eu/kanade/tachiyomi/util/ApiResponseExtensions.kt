package eu.kanade.tachiyomi.util

import com.elvishew.xlog.XLog
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.skydoves.sandwich.ApiResponse
import com.skydoves.sandwich.onFailure
import com.skydoves.sandwich.onSuccess
import eu.kanade.tachiyomi.source.online.models.dto.ErrorResponse
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.jsoup.Jsoup
import org.nekomanga.domain.network.ResultError

/**
 * Maps the ApiResponse Error to a Result Error, trying to decode the json response if its a mangadex api error
 */
fun ApiResponse.Failure.Error<*>.toResultError(errorType: String): ResultError {
    val errorBody = this.errorBody?.string() ?: ""
    XLog.e("error $errorType, \n error response code ${this.statusCode.code}\n $errorBody")

    val parsedJsoup = Jsoup.parse(this.toString())
    val textOfHtmlString: String = parsedJsoup.text()

    val error = when (textOfHtmlString != this.toString()) {
        true -> "Http error code: ${this.statusCode.code}. \n'${parsedJsoup.body().select("h1").text()}'"
        false -> {
            val json = Json { ignoreUnknownKeys = true }
            runCatching {
                val error = json.decodeFromString<ErrorResponse>(errorBody)
                "Http error code: ${this.statusCode.code}. \n'${error.errors.joinToString("\n") { it.detail ?: it.title ?: "no details or titles in error" }}'"
            }.getOrElse { "Received http error code: ${this.statusCode.code}" }
        }
    }

    return ResultError.HttpError(this.statusCode.code, error)
}

fun ApiResponse.Failure<*>.toResultError(errorType: String): ResultError {
    return when (this) {
        is ApiResponse.Failure.Error -> this.toResultError(errorType)
        is ApiResponse.Failure.Exception -> this.toResultError(errorType)
    }
}

fun <T> ApiResponse<T>.getOrResultError(errorType: String): Result<T, ResultError> {
    var result: Result<T, ResultError> = Err(ResultError.Generic(errorString = "Unknown Error"))
    this.onFailure {
        result = Err(this.toResultError(errorType))
    }.onSuccess {
        result = Ok(this.data)
    }
    return result
}

/**
 * Maps the ApiResponse Exception to a Result Error
 */
fun ApiResponse.Failure.Exception<*>.toResultError(errorType: String): ResultError {
    XLog.enableStackTrace(10).e("Exception $errorType ${this.message}", this.exception)

    return ResultError.Generic(errorString = "Unknown Error: '${this.message}'")
}

fun ApiResponse<*>.log(type: String) {
    return when (this) {
        is ApiResponse.Failure.Exception -> {
            XLog.enableStackTrace(10).e("Exception $type ${this.message}", this.exception)
        }
        is ApiResponse.Failure.Error -> {
            XLog.e("error $type, \n error response code ${this.statusCode.code}\n ${this.errorBody?.string()}")
        }
        else -> {
            XLog.e("error $type")
        }
    }
}

fun ApiResponse<*>.throws(type: String) {
    when (this) {
        is ApiResponse.Failure.Error -> {
            throw Exception("Error $type http code: ${this.statusCode.code}")
        }
        is ApiResponse.Failure.Exception -> {
            throw Exception("Error $type ${this.message} ${this.exception}")
        }
        else -> {
            throw Exception("Error $type ")
        }
    }
}
