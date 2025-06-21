// data/models/DownloadResponse.kt
package com.saikou.teraplay.data.models

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class DownloadResponse(
    @SerializedName("file_name")
    val fileName: String,

    @SerializedName("direct_link")
    val directLink: String,

    @SerializedName("thumb")
    val thumb: String,

    @SerializedName("size")
    val size: String,

    @SerializedName("sizebytes")
    val sizeBytes: Long
):Parcelable
