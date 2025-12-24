package com.example.deliveryapp.data.remote.dto

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize
import java.util.Date

@Parcelize
data class ProductDto(
    val id: Long,
    val name: String,
    val description: String?,
    val price: Double,
    @SerializedName("qty_initial") val qty_initial: Long?,
    @SerializedName("qty_sold") val qty_sold: Long?,
    @SerializedName("created_at") val created_at: Date?,
    val images: List<ProductImageDto> = emptyList(),
    val avg_rate: Double = 0.0,
    val review_count: Int = 0
) : Parcelable

//@Parcelize
//data class ProductImageDto(
//    @SerializedName("ID") val id: Long,
//    @SerializedName("URL") val url: String,
//    @SerializedName("IsMain") val is_main: Boolean
//) : Parcelable


//@Parcelize
//data class ProductImageDto(
//    @SerializedName("id") val id: Long,
//    @SerializedName("url") val url: String,
//    @SerializedName("is_main") val isMain: Boolean
//) : Parcelable


@Parcelize
data class ProductImageDto(
    @SerializedName(value = "id", alternate = ["ID"]) val id: Long,
    @SerializedName(value = "url", alternate = ["URL"]) val url: String,
    @SerializedName(value = "is_main", alternate = ["IsMain"]) val isMain: Boolean
) : Parcelable
