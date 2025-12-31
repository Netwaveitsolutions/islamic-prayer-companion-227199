package org.example.app.ui.settings

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.example.app.R
import org.example.app.data.prefs.AppPreferences
import org.example.app.domain.City

class CityPickerActivity : AppCompatActivity() {

    private lateinit var prefs: AppPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = AppPreferences(this)

        setContentView(R.layout.activity_city_picker)

        val all = CityCatalog.cities()
        val adapter = CityAdapter { city ->
            prefs.setSelectedCity(city)
            finish()
        }

        val rv = findViewById<RecyclerView>(R.id.rvCities)
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = adapter

        adapter.submit(all)

        val search = findViewById<EditText>(R.id.etSearch)
        search.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val q = s?.toString()?.trim()?.lowercase().orEmpty()
                if (q.isEmpty()) adapter.submit(all)
                else adapter.submit(all.filter { it.name.lowercase().contains(q) || it.country.lowercase().contains(q) })
            }
            override fun afterTextChanged(s: Editable?) = Unit
        })
    }
}
