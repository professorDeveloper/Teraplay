package com.saikou.teraplay.data.remote

import com.saikou.teraplay.domain.exceptions.NetworkException
import com.saikou.teraplay.domain.exceptions.UnknownException
import com.saikou.teraplay.utils.toResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.IOException
import retrofit2.Response

suspend fun <T> safeApiCall(apiCall: suspend () -> Response<T>): Result<T> {
    return withContext(Dispatchers.IO) {
        try {
            val response = apiCall()
            response.toResult()
        } catch (e: IOException) {
            Result.failure(NetworkException("Network error: ${e.message}"))
        } catch (e: Exception) {
            Result.failure(UnknownException("Unexpected error: ${e.message}"))
        }
    }
}