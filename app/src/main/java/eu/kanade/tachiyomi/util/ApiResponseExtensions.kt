package eu.kanade.tachiyomi.util

import com.elvishew.xlog.XLog
import com.skydoves.sandwich.ApiResponse

fun ApiResponse.Failure<*>.log(type: String) {
    if (this is ApiResponse.Failure.Exception<*>) {
        XLog.e("Exception $type", this.exception)
    } else if (this is ApiResponse.Failure.Error<*>) {
        XLog.e("error $type ${(this as ApiResponse.Failure.Error<*>).errorBody}")
        XLog.e("error response code ${this.statusCode.code}")
    }
}

fun ApiResponse.Failure<*>.throws(type: String) {
    if (this is ApiResponse.Failure.Error<*>) {
        throw Exception("Error $type http code: ${this.statusCode.code}")
    } else {
        throw Exception("Error $type ")
    }
}
