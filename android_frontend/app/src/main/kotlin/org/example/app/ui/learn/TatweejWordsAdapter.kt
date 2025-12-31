package org.example.app.ui.learn

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import org.example.app.R
import org.example.app.learn.TatweejWord

class TatweejWordsAdapter(
    private val onClick: (index: Int, word: TatweejWord) -> Unit
) : RecyclerView.Adapter<TatweejWordsAdapter.VH>() {

    private var items: List<TatweejWord> = emptyList()
    private var playingIndex: Int? = null

    fun submit(words: List<TatweejWord>) {
        items = words
        notifyDataSetChanged()
    }

    fun setPlayingIndex(index: Int?) {
        playingIndex = index
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_word_chip, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val word = items[position]
        holder.word.text = "${word.display}  •  ${word.transliteration}"

        val isPlaying = playingIndex == position
        holder.itemView.setBackgroundColor(
            if (isPlaying) ContextCompat.getColor(holder.itemView.context, R.color.ocean_secondary)
            else ContextCompat.getColor(holder.itemView.context, android.R.color.transparent)
        )

        holder.word.setTextColor(
            if (isPlaying) ContextCompat.getColor(holder.itemView.context, android.R.color.black)
            else ContextCompat.getColor(holder.itemView.context, R.color.ocean_text)
        )

        holder.itemView.setOnClickListener { onClick(position, word) }
    }

    override fun getItemCount(): Int = items.size

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val word: TextView = v.findViewById(R.id.tvWord)
    }
}
