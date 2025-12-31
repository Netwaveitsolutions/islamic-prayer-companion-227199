package org.example.app.ui.settings

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.example.app.R
import org.example.app.data.prefs.AppPreferences
import org.example.app.domain.City

class CityPickerActivity : AppCompatActivity() {

    private lateinit var prefs: AppPreferences

    private val handler = Handler(Looper.getMainLooper())
    private var pendingFilter: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = AppPreferences(this)

        setContentView(R.layout.activity_city_picker)

        val all = CityCatalog.cities()
        val selected = prefs.getSelectedCity()

        val tvSelection = findViewById<TextView>(R.id.tvSelectionSummary)
        tvSelection.text = getString(R.string.city_picker_current_selection_value, selected.name, selected.country)

        val tvEmpty = findViewById<TextView>(R.id.tvEmpty)

        val adapter = CityAdapter { city: City ->
            prefs.setSelectedCity(city)
            // Keep UX snappy: save and close immediately.
            finish()
        }
        adapter.setSelectedCity(selected)

        val rv = findViewById<RecyclerView>(R.id.rvCities)
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = adapter

        adapter.submit(all)

        fun applyFilter(queryRaw: String) {
            val q = queryRaw.trim()
            adapter.setQuery(q)

            val filtered = if (q.isEmpty()) {
                all
            } else {
                val qLower = q.lowercase()
                all.filter {
                    it.name.lowercase().contains(qLower) || it.country.lowercase().contains(qLower)
                }
            }

            adapter.submit(filtered)
            tvEmpty.visibility = if (filtered.isEmpty()) TextView.VISIBLE else TextView.GONE
        }

        val search = findViewById<EditText>(R.id.etSearch)
        search.addTextChangedListener(SimpleTextWatcher { text ->
            // Debounce to avoid jank on large lists.
            pendingFilter?.let { handler.removeCallbacks(it) }
            pendingFilter = Runnable { applyFilter(text) }.also {
                handler.postDelayed(it, 250L)
            }
        })
    }

    override fun onDestroy() {
        pendingFilter?.let { handler.removeCallbacks(it) }
        super.onDestroy()
    }
}
