package org.example.app.domain

import java.time.LocalDate

data class City(
    val name: String,
    val country: String,
    val latitude: Double,
    val longitude: Double
)

enum class UserMode(val value: String) {
    GUEST("guest"),
    GOOGLE("google")
}

enum class PrayerName(val displayName: String) {
    FAJR("Fajr"),
    DHUHR("Dhuhr"),
    ASR("Asr"),
    MAGHRIB("Maghrib"),
    ISHA("Isha")
}

data class PrayerTimes(
    val date: LocalDate,
    val fajr: String,
    val dhuhr: String,
    val asr: String,
    val maghrib: String,
    val isha: String,
    val timezone: String
) {
    fun asPairs(): List<Pair<PrayerName, String>> {
        return listOf(
            PrayerName.FAJR to fajr,
            PrayerName.DHUHR to dhuhr,
            PrayerName.ASR to asr,
            PrayerName.MAGHRIB to maghrib,
            PrayerName.ISHA to isha
        )
    }
}
