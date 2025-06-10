package com.saikou.teraplay.data.repository

import com.google.gson.Gson
import com.saikou.teraplay.data.remote.TeraBoxApi
import com.saikou.teraplay.data.models.DownloadRequest
import com.saikou.teraplay.data.models.DownloadResponse
import com.saikou.teraplay.data.remote.safeApiCall
import com.saikou.teraplay.domain.exceptions.NetworkException
import com.saikou.teraplay.domain.exceptions.UnknownException
import com.saikou.teraplay.domain.repository.DownloadRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException

class DownloadRepositoryImpl(private val apiService: TeraBoxApi):DownloadRepository {
    override suspend fun fetchDownloadInfo(url: String): Result<DownloadResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.fetchDownloadLink( DownloadRequest(url) )

                if (response.isSuccessful) {
                    val rawJson = response.body()?.string().orEmpty()
                    val json = JSONObject(rawJson)
                    if (json.has("error")) {
                        val errMsg = json.optString("error", "Unknown error")
                        return@withContext Result.failure(Exception(errMsg))
                    }

                    val data = Gson().fromJson(rawJson, DownloadResponse::class.java)
                    return@withContext Result.success(data)
                }

                val errBody = response.errorBody()?.string().orEmpty()
                val msg = try {
                    JSONObject(errBody).optString("error", errBody)
                } catch (_: Exception) {
                    "HTTP ${response.code()}: $errBody"
                }
                Result.failure(Exception(msg))

            } catch (io: IOException) {
                Result.failure(Exception("Network error: ${io.message}"))
            } catch (t: Throwable) {
                Result.failure(Exception("Unexpected error: ${t.message}"))
            }
        }
    }

}