package eu.kanade.tachiyomi.util

import com.elvishew.xlog.XLog
import com.skydoves.sandwich.ApiResponse

fun ApiResponse<*>.log(type: String) {
    when (this) {
        is ApiResponse.Failure.Exception -> {
            XLog.e("Exception $type ${this.message}", this.exception)
        }
        is ApiResponse.Failure.Error -> {
            XLog.e("error $type ${this.errorBody}")
            XLog.e("error response code ${this.statusCode.code}")
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
