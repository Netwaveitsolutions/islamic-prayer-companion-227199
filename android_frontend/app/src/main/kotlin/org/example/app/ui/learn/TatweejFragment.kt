package org.example.app.ui.learn

import android.media.MediaPlayer
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.example.app.R
import org.example.app.learn.TatweejWord

class TatweejFragment : Fragment(R.layout.fragment_tatweej) {

    private var mediaPlayer: MediaPlayer? = null
    private var playingIndex: Int? = null

    private lateinit var wordsAdapter: TatweejWordsAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val rv = view.findViewById<RecyclerView>(R.id.rvWords)
        val tvNowPlaying = view.findViewById<TextView>(R.id.tvNowPlaying)

        val words = listOf(
            // Bundled real audio from Wikimedia Commons in res/raw.
            TatweejWord(display = "بِسْمِ الله", transliteration = "Bismillah", audioRes = R.raw.bismillah_ar),
            TatweejWord(display = "الله", transliteration = "Allah", audioRes = R.raw.allah_ar),
            TatweejWord(display = "السلام عليكم", transliteration = "As-salamu alaykum", audioRes = R.raw.assalamu_alaykum_ar),
            TatweejWord(display = "البسملة", transliteration = "Basmala", audioRes = R.raw.basmala_ar)
        )

        wordsAdapter = TatweejWordsAdapter(
            onClick = { index, word ->
                play(index, word, tvNowPlaying)
            }
        )

        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.adapter = wordsAdapter
        wordsAdapter.submit(words)
    }

    private fun play(index: Int, word: TatweejWord, tvNowPlaying: TextView) {
        stopPlayback()

        playingIndex = index
        wordsAdapter.setPlayingIndex(index)
        tvNowPlaying.text = "Now playing: ${word.transliteration}"

        mediaPlayer = MediaPlayer.create(requireContext(), word.audioRes).apply {
            setOnCompletionListener {
                playingIndex = null
                wordsAdapter.setPlayingIndex(null)
                tvNowPlaying.text = ""
                stopPlayback()
            }
            start()
        }
    }

    private fun stopPlayback() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    override fun onPause() {
        super.onPause()
        stopPlayback()
    }
}
