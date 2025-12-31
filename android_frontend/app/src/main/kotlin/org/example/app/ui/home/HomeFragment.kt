package org.example.app.ui.home

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.CheckBox
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch
import org.example.app.R
import org.example.app.data.prefs.AppPreferences
import org.example.app.data.prayertimes.PrayerTimesRepository
import org.example.app.domain.PrayerName
import org.example.app.domain.PrayerTimes
import org.example.app.prayer.SalahRepository
import org.example.app.prayer.notifications.PrayerNotificationScheduler
import org.example.app.qibla.CompassSensorManager
import org.example.app.qibla.QiblaCalculator
import org.example.app.qibla.ui.QiblaCompassView
import org.example.app.ui.prayer.SalahHistoryActivity
import org.example.app.ui.qibla.QiblaActivity
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class HomeFragment : Fragment(R.layout.fragment_home) {

    private lateinit var prefs: AppPreferences
    private lateinit var prayerRepo: PrayerTimesRepository
    private lateinit var salahRepo: SalahRepository

    private var compassManager: CompassSensorManager? = null

    private val uiHandler = Handler(Looper.getMainLooper())
    private val clockRunnable = object : Runnable {
        override fun run() {
            updateHeader()
            uiHandler.postDelayed(this, 15_000)
        }
    }

    private var currentPrayerTimes: PrayerTimes? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        prefs = AppPreferences(requireContext())
        prayerRepo = PrayerTimesRepository(requireContext())
        salahRepo = SalahRepository(requireContext())

        val rv = view.findViewById<RecyclerView>(R.id.rvPrayerTimes)
        rv.layoutManager = LinearLayoutManager(requireContext())
        val adapter = PrayerTimesAdapter()
        rv.adapter = adapter

        view.findViewById<View>(R.id.btnHistory).setOnClickListener {
            startActivity(Intent(requireContext(), SalahHistoryActivity::class.java))
        }

        view.findViewById<View>(R.id.btnOpenQibla).setOnClickListener {
            startActivity(Intent(requireContext(), QiblaActivity::class.java))
        }

        val btnRetry = view.findViewById<MaterialButton>(R.id.btnRetryPrayerTimes)
        btnRetry.setOnClickListener {
            lifecycleScope.launch { refresh(adapter, userInitiated = true) }
        }

        // Checklist bindings
        val cbFajr = view.findViewById<CheckBox>(R.id.cbFajr)
        val cbDhuhr = view.findViewById<CheckBox>(R.id.cbDhuhr)
        val cbAsr = view.findViewById<CheckBox>(R.id.cbAsr)
        val cbMaghrib = view.findViewById<CheckBox>(R.id.cbMaghrib)
        val cbIsha = view.findViewById<CheckBox>(R.id.cbIsha)

        fun bindChecklist(date: LocalDate) {
            val state = salahRepo.getChecklist(date)
            cbFajr.isChecked = state[PrayerName.FAJR] == true
            cbDhuhr.isChecked = state[PrayerName.DHUHR] == true
            cbAsr.isChecked = state[PrayerName.ASR] == true
            cbMaghrib.isChecked = state[PrayerName.MAGHRIB] == true
            cbIsha.isChecked = state[PrayerName.ISHA] == true
        }

        val today = LocalDate.now()
        bindChecklist(today)

        fun setUpCheckbox(cb: CheckBox, prayer: PrayerName) {
            cb.setOnCheckedChangeListener { _, isChecked ->
                salahRepo.setPrayerCompleted(today, prayer, isChecked)
            }
        }
        setUpCheckbox(cbFajr, PrayerName.FAJR)
        setUpCheckbox(cbDhuhr, PrayerName.DHUHR)
        setUpCheckbox(cbAsr, PrayerName.ASR)
        setUpCheckbox(cbMaghrib, PrayerName.MAGHRIB)
        setUpCheckbox(cbIsha, PrayerName.ISHA)

        updateHeader()

        lifecycleScope.launch {
            refresh(adapter, userInitiated = false)
        }

        // Qibla preview setup
        val qiblaView = view.findViewById<QiblaCompassView>(R.id.qiblaPreview)
        val city = prefs.getSelectedCity()
        val qiblaBearing = QiblaCalculator.bearingToKaabaDegrees(city.latitude, city.longitude)
        qiblaView.setQiblaBearingDegrees(qiblaBearing)
        view.findViewById<TextView>(R.id.tvQiblaAngle).text = "Qibla bearing: ${qiblaBearing.toInt()}°"

        compassManager = CompassSensorManager(requireContext(), object : CompassSensorManager.Listener {
            override fun onAzimuthDegrees(azimuth: Double, accuracy: Int) {
                qiblaView.setDeviceAzimuthDegrees(azimuth)
            }
        })
    }

    private suspend fun refresh(adapter: PrayerTimesAdapter, userInitiated: Boolean) {
        val root = requireView()
        val status = root.findViewById<TextView>(R.id.tvStatus)
        val loading = root.findViewById<ProgressBar>(R.id.pbPrayerTimes)
        val error = root.findViewById<TextView>(R.id.tvPrayerTimesError)
        val retry = root.findViewById<MaterialButton>(R.id.btnRetryPrayerTimes)

        val settings = prayerRepo.currentSettings()
        val date = LocalDate.now()

        val madhabLabel = if (settings.school == 1) "Hanafi" else "Shafi"
        val highLatLabel = when (settings.highLatitudeRule) {
            1 -> "MiddleOfNight"
            2 -> "OneSeventh"
            else -> "AngleBased"
        }

        root.findViewById<TextView>(R.id.tvCity).text =
            "${settings.city.name}, ${settings.city.country} • method ${settings.method} • madhab $madhabLabel • $highLatLabel"

        // Loading state
        loading.visibility = View.VISIBLE
        error.visibility = View.GONE
        retry.visibility = View.GONE
        status.text = if (userInitiated) getString(R.string.prayer_times_refreshing) else getString(R.string.prayer_times_loading)

        val result = prayerRepo.getPrayerTimesForDate(
            date = date,
            city = settings.city,
            method = settings.method,
            school = settings.school,
            highLatitudeRule = settings.highLatitudeRule
        )

        loading.visibility = View.GONE

        result.onSuccess { withSource ->
            currentPrayerTimes = withSource.times
            adapter.submit(withSource.times)
            updateNextPrayer(withSource.times)

            error.visibility = View.GONE
            retry.visibility = View.GONE

            status.text = when (withSource.source) {
                PrayerTimesRepository.Source.NETWORK -> getString(R.string.prayer_times_updated)
                PrayerTimesRepository.Source.CACHE -> getString(R.string.prayer_times_offline_showing_saved)
            }

            maybeScheduleNotifications(withSource.times)
        }.onFailure {
            // Total failure (no cache)
            status.text = getString(R.string.prayer_times_failed_status)
            error.text = getString(R.string.prayer_times_failed_body)
            error.visibility = View.VISIBLE
            retry.visibility = View.VISIBLE
        }
    }

    private fun updateHeader() {
        val tvHeader = requireView().findViewById<TextView>(R.id.tvHeader)
        val now = LocalDateTime.now()
        val dateStr = now.format(DateTimeFormatter.ofPattern("EEE, MMM d"))
        val timeStr = now.format(DateTimeFormatter.ofPattern("HH:mm"))
        tvHeader.text = "$dateStr • $timeStr"
    }

    private fun updateNextPrayer(times: PrayerTimes) {
        val tvNext = requireView().findViewById<TextView>(R.id.tvNextPrayer)
        val now = LocalTime.now()

        val entries = times.asPairs().map { (name, timeStr) ->
            val t = runCatching { LocalTime.parse(timeStr) }.getOrNull()
            Triple(name, timeStr, t)
        }.filter { it.third != null }

        val next = entries.firstOrNull { it.third!!.isAfter(now) } ?: entries.firstOrNull()
        tvNext.text = if (next != null) "Next: ${next.first.displayName} • ${next.second}" else "Next: —"
    }

    private fun maybeScheduleNotifications(times: PrayerTimes) {
        if (!prefs.notificationsEnabled()) return
        val scheduler = PrayerNotificationScheduler(requireContext())
        scheduler.scheduleForToday(times, prefs.notifyBeforeMinutes())
    }

    override fun onResume() {
        super.onResume()
        uiHandler.post(clockRunnable)
        compassManager?.start()
        // Refresh city/method/high-lat changes immediately when coming back.
        lifecycleScope.launch {
            val rv = requireView().findViewById<RecyclerView>(R.id.rvPrayerTimes)
            val adapter = rv.adapter as? PrayerTimesAdapter ?: return@launch
            refresh(adapter, userInitiated = false)
        }
    }

    override fun onPause() {
        super.onPause()
        uiHandler.removeCallbacks(clockRunnable)
        compassManager?.stop()
    }
}
