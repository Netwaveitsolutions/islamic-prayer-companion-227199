package org.example.app.ui.settings

import android.graphics.Typeface
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import org.example.app.R
import org.example.app.domain.City
import java.util.Locale

class CityAdapter(private val onClick: (City) -> Unit) : RecyclerView.Adapter<CityAdapter.VH>() {

    private var items: List<City> = emptyList()
    private var query: String = ""
    private var selected: City? = null

    fun submit(cities: List<City>) {
        items = cities
        notifyDataSetChanged()
    }

    fun setQuery(query: String) {
        this.query = query
        notifyDataSetChanged()
    }

    fun setSelectedCity(city: City?) {
        selected = city
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_city, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val city = items[position]

        holder.name.text = highlight(city.name, query, holder.itemView)
        holder.country.text = highlight(city.country, query, holder.itemView)

        val isSelected = selected?.let { it.name == city.name && it.country == city.country } == true
        holder.selectedChip.visibility = if (isSelected) View.VISIBLE else View.GONE

        holder.itemView.setOnClickListener { onClick(city) }
    }

    private fun highlight(value: String, query: String, view: View): CharSequence {
        if (query.isBlank()) return value
        val q = query.trim().lowercase(Locale.US)
        val sourceLower = value.lowercase(Locale.US)

        val start = sourceLower.indexOf(q)
        if (start < 0) return value

        val end = start + q.length
        val span = SpannableString(value)
        val color = ContextCompat.getColor(view.context, R.color.ocean_secondary)

        span.setSpan(ForegroundColorSpan(color), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        span.setSpan(StyleSpan(Typeface.BOLD), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        return span
    }

    override fun getItemCount(): Int = items.size

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val name: TextView = v.findViewById(R.id.tvCityName)
        val country: TextView = v.findViewById(R.id.tvCountry)
        val selectedChip: TextView = v.findViewById(R.id.tvSelectedChip)
    }
}
