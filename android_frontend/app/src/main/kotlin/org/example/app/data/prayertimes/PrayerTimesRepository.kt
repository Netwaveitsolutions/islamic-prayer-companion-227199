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
import java.util.concurrent.TimeUnit

class PrayerTimesRepository(private val context: Context) {

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

    suspend fun getTodayPrayerTimes(city: City, method: Int, school: Int): Result<PrayerTimes> {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.getTimingsByCity(city = city.name, country = city.country, method = method, school = school)
                val pt = response.toPrayerTimes()
                cachePrayerTimes(city, method, school, pt)
                Result.success(pt)
            } catch (t: Throwable) {
                val cached = getCachedPrayerTimes(city, method, school)
                if (cached != null) Result.success(cached) else Result.failure(t)
            }
        }
    }

    private fun AladhanTimingsResponse.toPrayerTimes(): PrayerTimes {
        val dateStr = data.date.gregorian.date // dd-MM-yyyy
        val date = LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("dd-MM-yyyy"))

        // Aladhan sometimes includes timezone suffix like "05:10 (UTC)" in other endpoints;
        // timingsByCity typically provides HH:mm. We'll defensively strip after space.
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

    private fun cachePrayerTimes(city: City, method: Int, school: Int, times: PrayerTimes) {
        prefs.edit()
            .putString(cacheKey(city, method, school), gson.toJson(times))
            .apply()
    }

    private fun getCachedPrayerTimes(city: City, method: Int, school: Int): PrayerTimes? {
        val json = prefs.getString(cacheKey(city, method, school), null) ?: return null
        return runCatching { gson.fromJson(json, PrayerTimes::class.java) }.getOrNull()
    }

    private fun cacheKey(city: City, method: Int, school: Int): String {
        return "cache_prayer_times_${city.name}_${city.country}_m${method}_s${school}"
    }

    fun currentSettings(): Triple<City, Int, Int> {
        val appPrefs = AppPreferences(context)
        return Triple(appPrefs.getSelectedCity(), appPrefs.getCalculationMethod(), appPrefs.getMadhabSchool())
    }
}
