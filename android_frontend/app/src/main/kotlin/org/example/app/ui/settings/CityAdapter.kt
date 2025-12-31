package org.example.app.ui.settings

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.example.app.R
import org.example.app.domain.City

class CityAdapter(private val onClick: (City) -> Unit) : RecyclerView.Adapter<CityAdapter.VH>() {

    private var items: List<City> = emptyList()

    fun submit(cities: List<City>) {
        items = cities
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_city, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val city = items[position]
        holder.name.text = city.name
        holder.country.text = city.country
        holder.itemView.setOnClickListener { onClick(city) }
    }

    override fun getItemCount(): Int = items.size

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val name: TextView = v.findViewById(R.id.tvCityName)
        val country: TextView = v.findViewById(R.id.tvCountry)
    }
}
