// data/api/TeraBoxApi.kt
package com.saikou.teraplay.data.remote

import com.saikou.teraplay.data.models.DownloadRequest
import com.saikou.teraplay.data.models.DownloadResponse
import okhttp3.Response
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface TeraBoxApi {
    @Headers("Accept: application/json", "Content-Type: application/json")
    @POST("api.php")
    suspend fun fetchDownloadLink(
        @Body request: DownloadRequest
    ): retrofit2.Response<ResponseBody>
}
