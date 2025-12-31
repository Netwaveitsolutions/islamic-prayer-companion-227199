package org.example.app.ui.learn

import android.os.Bundle
import android.os.SystemClock
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.example.app.R
import org.example.app.learn.TatweejWord

class TatweejFragment : Fragment(R.layout.fragment_tatweej) {

    private var audioPlayer: TatweejAudioPlayer? = null

    private lateinit var wordsAdapter: TatweejWordsAdapter
    private var words: List<TatweejWord> = emptyList()

    // Fragment-level debounce (extra protection if the RecyclerView dispatches rapidly).
    private var lastTapUptimeMs: Long = 0L
    private val tapDebounceMs: Long = 160L

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val rv = view.findViewById<RecyclerView>(R.id.rvWords)
        val tvNowPlaying = view.findViewById<TextView>(R.id.tvNowPlaying)

        words = listOf(
            // Bundled real audio in res/raw (short clips).
            TatweejWord(display = "بِسْمِ الله", transliteration = "Bismillah", audioRes = R.raw.bismillah_ar),
            TatweejWord(display = "الله", transliteration = "Allah", audioRes = R.raw.allah_ar),
            TatweejWord(display = "السلام عليكم", transliteration = "As-salamu alaykum", audioRes = R.raw.assalamu_alaykum_ar),
            TatweejWord(display = "البسملة", transliteration = "Basmala", audioRes = R.raw.basmala_ar)
        )

        wordsAdapter = TatweejWordsAdapter(
            onClick = { index, word ->
                val now = SystemClock.uptimeMillis()
                if (now - lastTapUptimeMs < tapDebounceMs) return@TatweejWordsAdapter
                lastTapUptimeMs = now

                // Seek-to-word + interruption is handled by TatweejAudioPlayer.
                audioPlayer?.onWordTapped(index, word)
            }
        )

        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.adapter = wordsAdapter
        rv.itemAnimator?.changeDuration = 120L
        wordsAdapter.submit(words)

        // Create once (appContext avoids leaking the fragment/activity).
        audioPlayer = TatweejAudioPlayer(requireContext().applicationContext).apply {
            setOnStateListener { state ->
                wordsAdapter.setPlayingIndex(state.playingIndex)

                val idx = state.playingIndex
                if (idx == null) {
                    tvNowPlaying.text = ""
                } else {
                    val label = words.getOrNull(idx)?.transliteration ?: ""
                    tvNowPlaying.text = getString(R.string.tatweej_now_playing, label)
                }
            }

            // Warm-up to reduce first-tap latency.
            preload(words)
        }
    }

    override fun onStop() {
        super.onStop()
        // Stop when leaving screen to avoid audio continuing in background.
        audioPlayer?.stop()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        audioPlayer?.release()
        audioPlayer = null
    }
}
