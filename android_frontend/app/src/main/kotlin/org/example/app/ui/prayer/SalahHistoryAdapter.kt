package org.example.app.ui.prayer

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.example.app.R
import org.example.app.prayer.SalahDayRecord
import org.example.app.prayer.SalahRepository

class SalahHistoryAdapter(
    private val repo: SalahRepository
) : RecyclerView.Adapter<SalahHistoryAdapter.VH>() {

    private var items: List<SalahDayRecord> = emptyList()

    fun submit(records: List<SalahDayRecord>) {
        items = records
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_salah_history, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val record = items[position]
        holder.date.text = record.dateIso
        holder.summary.text = "Completed ${repo.countCompleted(record)}/5"
    }

    override fun getItemCount(): Int = items.size

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val date: TextView = v.findViewById(R.id.tvDate)
        val summary: TextView = v.findViewById(R.id.tvSummary)
    }
}
