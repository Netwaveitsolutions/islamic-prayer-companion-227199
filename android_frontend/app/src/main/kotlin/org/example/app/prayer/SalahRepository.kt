package org.example.app.prayer

import android.content.Context
import androidx.preference.PreferenceManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.example.app.data.prefs.AppPreferences
import org.example.app.domain.PrayerName
import java.time.LocalDate

data class SalahDayRecord(
    val dateIso: String,
    val completed: Map<String, Boolean>
)

class SalahRepository(context: Context) {

    private val appContext = context.applicationContext
    private val gson = Gson()
    private val prefs = PreferenceManager.getDefaultSharedPreferences(appContext)
    private val appPrefs = AppPreferences(appContext)

    fun getChecklist(date: LocalDate): Map<PrayerName, Boolean> {
        val record = getDayRecord(date)
        return PrayerName.entries.associateWith { prayer ->
            record?.completed?.get(prayer.name) ?: false
        }
    }

    fun setPrayerCompleted(date: LocalDate, prayer: PrayerName, done: Boolean) {
        val record = getDayRecord(date)
        val current = (record?.completed ?: emptyMap()).toMutableMap()
        current[prayer.name] = done

        val updated = SalahDayRecord(
            dateIso = date.toString(),
            completed = current
        )

        val all = getAllRecords().toMutableList()
        val idx = all.indexOfFirst { it.dateIso == date.toString() }
        if (idx >= 0) all[idx] = updated else all.add(updated)

        saveAllRecords(all)
    }

    fun getHistory(lastDays: Int): List<SalahDayRecord> {
        val all = getAllRecords()
        val cutoff = LocalDate.now().minusDays(lastDays.toLong() - 1)
        return all
            .mapNotNull { r ->
                runCatching { LocalDate.parse(r.dateIso) to r }.getOrNull()
            }
            .filter { (d, _) -> !d.isBefore(cutoff) }
            .sortedByDescending { (d, _) -> d }
            .map { it.second }
    }

    fun countCompleted(record: SalahDayRecord): Int {
        return PrayerName.entries.count { p -> record.completed[p.name] == true }
    }

    private fun getDayRecord(date: LocalDate): SalahDayRecord? {
        return getAllRecords().firstOrNull { it.dateIso == date.toString() }
    }

    private fun saveAllRecords(records: List<SalahDayRecord>) {
        prefs.edit().putString(recordsKey(), gson.toJson(records)).apply()
    }

    private fun getAllRecords(): List<SalahDayRecord> {
        val json = prefs.getString(recordsKey(), null) ?: return emptyList()
        val type = object : TypeToken<List<SalahDayRecord>>() {}.type
        return runCatching { gson.fromJson<List<SalahDayRecord>>(json, type) }.getOrElse { emptyList() }
    }

    private fun recordsKey(): String {
        // Associate local data with user mode. If Google is active, key by account id; otherwise guest.
        val mode = appPrefs.getUserMode()
        val suffix = if (mode == org.example.app.domain.UserMode.GOOGLE) {
            appPrefs.getGoogleAccountId() ?: "google_unknown"
        } else {
            "guest"
        }
        return "salah_records_$suffix"
    }
}
