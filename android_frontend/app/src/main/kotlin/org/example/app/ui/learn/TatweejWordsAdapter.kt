package org.example.app.ui.learn

import android.os.SystemClock
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import org.example.app.R
import org.example.app.learn.TatweejWord

class TatweejWordsAdapter(
    private val onClick: (index: Int, word: TatweejWord) -> Unit
) : RecyclerView.Adapter<TatweejWordsAdapter.VH>() {

    private var items: List<TatweejWord> = emptyList()
    private var playingIndex: Int? = null

    // Adapter-level debounce to avoid rapid tap spam and repeated player resets.
    private var lastClickUptimeMs: Long = 0L
    private val debounceMs: Long = 180L

    fun submit(words: List<TatweejWord>) {
        items = words
        notifyDataSetChanged()
    }

    fun setPlayingIndex(index: Int?) {
        val old = playingIndex
        playingIndex = index

        // Update only affected rows to avoid UI jank.
        if (old != null) notifyItemChanged(old)
        if (index != null) notifyItemChanged(index)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_word_chip, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val word = items[position]
        val isPlaying = playingIndex == position

        holder.tvArabic.text = word.display
        holder.tvTransliteration.text = word.transliteration

        // Drive stateful background/stroke via selector using 'activated' state (works for any View).
        holder.card.isActivated = isPlaying

        // Badge visibility with a small alpha animation for polish.
        if (isPlaying) {
            if (holder.tvPlayingBadge.visibility != View.VISIBLE) {
                holder.tvPlayingBadge.alpha = 0f
                holder.tvPlayingBadge.visibility = View.VISIBLE
                holder.tvPlayingBadge.animate().alpha(1f).setDuration(120L).start()
            }
        } else {
            if (holder.tvPlayingBadge.visibility == View.VISIBLE) {
                holder.tvPlayingBadge.animate()
                    .alpha(0f)
                    .setDuration(120L)
                    .withEndAction {
                        holder.tvPlayingBadge.visibility = View.GONE
                        holder.tvPlayingBadge.alpha = 1f
                    }
                    .start()
            }
        }

        // Subtle scale highlight for the "playing" word.
        val targetScale = if (isPlaying) 1.02f else 1.0f
        holder.itemView.animate()
            .scaleX(targetScale)
            .scaleY(targetScale)
            .setDuration(140L)
            .start()

        holder.itemView.setOnClickListener {
            val now = SystemClock.uptimeMillis()
            if (now - lastClickUptimeMs < debounceMs) return@setOnClickListener
            lastClickUptimeMs = now

            onClick(position, word)
        }
    }

    override fun getItemCount(): Int = items.size

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val card: MaterialCardView = v.findViewById(R.id.cardWord)
        val tvArabic: TextView = v.findViewById(R.id.tvArabic)
        val tvTransliteration: TextView = v.findViewById(R.id.tvTransliteration)
        val tvPlayingBadge: TextView = v.findViewById(R.id.tvPlayingBadge)
    }
}
