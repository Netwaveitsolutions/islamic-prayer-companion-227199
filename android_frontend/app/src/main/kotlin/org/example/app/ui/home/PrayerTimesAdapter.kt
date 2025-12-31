package org.example.app.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.example.app.R
import org.example.app.domain.PrayerTimes

class PrayerTimesAdapter : RecyclerView.Adapter<PrayerTimesAdapter.VH>() {

    private var items: List<Pair<String, String>> = emptyList()

    fun submit(times: PrayerTimes) {
        items = times.asPairs().map { it.first.displayName to it.second }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_prayer_time, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val (name, time) = items[position]
        holder.name.text = name
        holder.time.text = time
    }

    override fun getItemCount(): Int = items.size

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val name: TextView = v.findViewById(R.id.tvName)
        val time: TextView = v.findViewById(R.id.tvTime)
    }
}
