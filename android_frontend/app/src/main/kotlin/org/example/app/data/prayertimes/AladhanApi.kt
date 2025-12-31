package org.example.app.data.prayertimes

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Query

interface AladhanApi {

    @GET("timingsByCity")
    suspend fun getTimingsByCity(
        @Query("city") city: String,
        @Query("country") country: String,
        @Query("method") method: Int,
        @Query("school") school: Int
    ): AladhanTimingsResponse
}

data class AladhanTimingsResponse(
    @SerializedName("code") val code: Int,
    @SerializedName("status") val status: String,
    @SerializedName("data") val data: AladhanData
)

data class AladhanData(
    @SerializedName("timings") val timings: AladhanTimings,
    @SerializedName("meta") val meta: AladhanMeta,
    @SerializedName("date") val date: AladhanDate
)

data class AladhanTimings(
    @SerializedName("Fajr") val fajr: String,
    @SerializedName("Dhuhr") val dhuhr: String,
    @SerializedName("Asr") val asr: String,
    @SerializedName("Maghrib") val maghrib: String,
    @SerializedName("Isha") val isha: String
)

data class AladhanMeta(
    @SerializedName("timezone") val timezone: String
)

data class AladhanDate(
    @SerializedName("gregorian") val gregorian: AladhanGregorian
)

data class AladhanGregorian(
    @SerializedName("date") val date: String // dd-MM-yyyy
)
