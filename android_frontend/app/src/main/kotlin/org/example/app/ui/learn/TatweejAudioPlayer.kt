package org.example.app.ui.learn

import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.RawResourceDataSource
import androidx.media3.exoplayer.ExoPlayer
import org.example.app.learn.TatweejWord

/**
 * Tatweej audio playback helper.
 *
 * Uses a single ExoPlayer instance (instead of creating MediaPlayer each tap) to:
 * - keep playback smooth,
 * - allow instant interruption/switching between words,
 * - minimize UI jank caused by synchronous media initialization.
 */
class TatweejAudioPlayer(
    private val appContext: Context
) {

    data class PlaybackUiState(
        val playingIndex: Int?,
        val isPlaying: Boolean,
        val isBuffering: Boolean
    )

    private val player: ExoPlayer = ExoPlayer.Builder(appContext).build().apply {
        val attrs = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
            .setUsage(C.USAGE_MEDIA)
            .build()
        setAudioAttributes(attrs, /* handleAudioFocus= */ true)
        playWhenReady = true
    }

    private var items: List<TatweejWord> = emptyList()
    private var playingIndex: Int? = null
    private var onState: ((PlaybackUiState) -> Unit)? = null

    private val listener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            emitState()
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_ENDED) {
                playingIndex = null
                emitState()
            } else {
                emitState()
            }
        }

        override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
            // Fail gracefully: clear highlight and let user tap again.
            playingIndex = null
            emitState()
        }
    }

    init {
        player.addListener(listener)
    }

    // PUBLIC_INTERFACE
    /**
     * Prepares the player and warms up the first item to reduce first-tap latency.
     *
     * @param words Tatweej words with raw bundled audio resource IDs.
     */
    fun preload(words: List<TatweejWord>) {
        items = words
        if (items.isEmpty()) return

        // Warm-up: prepare first clip paused. This reduces the chance of first-tap jank.
        val first = mediaItemFor(items[0])
        player.setMediaItem(first, /* resetPosition= */ true)
        player.prepare()
        player.playWhenReady = false
        emitState()
    }

    // PUBLIC_INTERFACE
    /**
     * Sets a listener for UI state changes (playing index, buffering, etc.).
     */
    fun setOnStateListener(listener: ((PlaybackUiState) -> Unit)?) {
        onState = listener
        emitState()
    }

    // PUBLIC_INTERFACE
    /**
     * Handles a word tap:
     * - If tapping the currently playing word, restart it from the beginning (seek-to-word).
     * - If tapping a different word, interrupt current playback and switch immediately.
     */
    fun onWordTapped(index: Int, word: TatweejWord) {
        if (playingIndex == index) {
            // Restart current word (seek-to-word)
            if (player.mediaItemCount > 0) {
                player.seekTo(0)
                player.playWhenReady = true
                player.play()
                emitState()
                return
            }
        }

        playingIndex = index
        val item = mediaItemFor(word)

        // Switch cleanly to the tapped word.
        player.setMediaItem(item, /* resetPosition= */ true)
        player.prepare()
        player.playWhenReady = true
        player.play()
        emitState()
    }

    // PUBLIC_INTERFACE
    /**
     * Stops playback and clears any "playing" UI state.
     */
    fun stop() {
        playingIndex = null
        player.stop()
        emitState()
    }

    // PUBLIC_INTERFACE
    /**
     * Releases the underlying ExoPlayer instance.
     */
    fun release() {
        player.removeListener(listener)
        player.release()
    }

    private fun emitState() {
        val isBuffering = player.playbackState == Player.STATE_BUFFERING
        onState?.invoke(
            PlaybackUiState(
                playingIndex = playingIndex,
                isPlaying = player.isPlaying,
                isBuffering = isBuffering
            )
        )
    }

    @androidx.annotation.OptIn(markerClass = [UnstableApi::class])
    private fun mediaItemFor(word: TatweejWord): MediaItem {
        val uri = RawResourceDataSource.buildRawResourceUri(word.audioRes)
        return MediaItem.fromUri(uri)
    }
}
