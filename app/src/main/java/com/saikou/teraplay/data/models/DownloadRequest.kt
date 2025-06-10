package com.saikou.teraplay.data.models

import com.google.gson.annotations.SerializedName

data class DownloadRequest(
    @SerializedName("url")
    val url: String
)