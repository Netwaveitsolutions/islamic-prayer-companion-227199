package org.example.app.data.prayertimes

import android.content.Context
import androidx.preference.PreferenceManager
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.example.app.data.prefs.AppPreferences
import org.example.app.domain.City
import org.example.app.domain.PrayerTimes
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.TimeUnit

class PrayerTimesRepository(private val context: Context) {

    enum class Source { NETWORK, CACHE }

    data class PrayerTimesWithSource(
        val times: PrayerTimes,
        val source: Source
    )

    private val gson = Gson()
    private val prefs = PreferenceManager.getDefaultSharedPreferences(context.applicationContext)

    private val api: AladhanApi by lazy {
        val logging = HttpLoggingInterceptor()
        logging.level = HttpLoggingInterceptor.Level.BASIC

        val okHttp = OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()

        Retrofit.Builder()
            .baseUrl("https://api.aladhan.com/v1/")
            .client(okHttp)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AladhanApi::class.java)
    }

    // PUBLIC_INTERFACE
    /**
     * Fetch prayer times for a given date and settings. Uses a cache fallback when offline.
     *
     * Caching + invalidation:
     * - Cache key includes (date, city, method, madhab, high-latitude rule).
     * - For the same (city+method+madhab+highLat), we keep only the most recent day cached
     *   and delete the previous day's cached entry when a new day's data is saved.
     *
     * @return Result.success(PrayerTimesWithSource) on network or cache success, Result.failure on total failure.
     */
    suspend fun getPrayerTimesForDate(
        date: LocalDate,
        city: City,
        method: Int,
        school: Int,
        highLatitudeRule: Int
    ): Result<PrayerTimesWithSource> {
        return withContext(Dispatchers.IO) {
            val settingsKey = settingsKey(city, method, school, highLatitudeRule)
            val key = cacheKey(settingsKey, date)

            try {
                val dateStr = date.format(DateTimeFormatter.ofPattern("dd-MM-yyyy"))
                val response = api.getTimingsByCity(
                    date = dateStr,
                    city = city.name,
                    country = city.country,
                    method = method,
                    school = school,
                    latitudeAdjustmentMethod = highLatitudeRule
                )

                val pt = response.toPrayerTimes()
                cachePrayerTimes(settingsKey, date, pt)
                Result.success(PrayerTimesWithSource(pt, Source.NETWORK))
            } catch (t: Throwable) {
                val cached = getCachedPrayerTimes(key)
                if (cached != null) Result.success(PrayerTimesWithSource(cached, Source.CACHE)) else Result.failure(t)
            }
        }
    }

    private fun AladhanTimingsResponse.toPrayerTimes(): PrayerTimes {
        val dateStr = data.date.gregorian.date // dd-MM-yyyy
        val date = LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("dd-MM-yyyy"))

        // Defensively strip potential timezone suffix (e.g., "05:10 (UTC)").
        fun clean(time: String): String = time.split(" ").first()

        return PrayerTimes(
            date = date,
            fajr = clean(data.timings.fajr),
            dhuhr = clean(data.timings.dhuhr),
            asr = clean(data.timings.asr),
            maghrib = clean(data.timings.maghrib),
            isha = clean(data.timings.isha),
            timezone = data.meta.timezone
        )
    }

    private fun cachePrayerTimes(settingsKey: String, date: LocalDate, times: PrayerTimes) {
        // Invalidate previous day cache for same settings (city/method/madhab/highLat)
        val lastDateKey = lastCachedDateKey(settingsKey)
        val previousDateIso = prefs.getString(lastDateKey, null)

        if (previousDateIso != null && previousDateIso != date.toString()) {
            val previousDate = runCatching { LocalDate.parse(previousDateIso) }.getOrNull()
            if (previousDate != null) {
                val oldCacheKey = cacheKey(settingsKey, previousDate)
                prefs.edit().remove(oldCacheKey).apply()
            }
        }

        val key = cacheKey(settingsKey, date)
        prefs.edit()
            .putString(key, gson.toJson(times))
            .putString(lastDateKey, date.toString())
            .apply()
    }

    private fun getCachedPrayerTimes(cacheKey: String): PrayerTimes? {
        val json = prefs.getString(cacheKey, null) ?: return null
        return runCatching { gson.fromJson(json, PrayerTimes::class.java) }.getOrNull()
    }

    private fun lastCachedDateKey(settingsKey: String): String = "cache_prayer_times_v2_lastDate_$settingsKey"

    private fun cacheKey(settingsKey: String, date: LocalDate): String = "cache_prayer_times_v2_${settingsKey}_d${date}"

    private fun settingsKey(city: City, method: Int, school: Int, highLat: Int): String {
        return "c${safeKey(city.name)}_${safeKey(city.country)}_m${method}_s${school}_lat${highLat}"
    }

    private fun safeKey(value: String): String {
        // SharedPreferences keys are strings; keep them deterministic and safe.
        return value
            .lowercase(Locale.US)
            .replace(Regex("[^a-z0-9]+"), "_")
            .trim('_')
    }

    // PUBLIC_INTERFACE
    /**
     * Convenience helper to get the current prayer-times settings from preferences.
     * Returns: City, method, madhab school, high-latitude rule.
     */
    fun currentSettings(): Settings {
        val appPrefs = AppPreferences(context)
        return Settings(
            city = appPrefs.getSelectedCity(),
            method = appPrefs.getCalculationMethod(),
            school = appPrefs.getMadhabSchool(),
            highLatitudeRule = appPrefs.getHighLatitudeRule()
        )
    }

    data class Settings(
        val city: City,
        val method: Int,
        val school: Int,
        val highLatitudeRule: Int
    )
}
