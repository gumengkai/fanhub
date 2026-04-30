package com.fantok.app.player

import android.content.Context
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.fantok.app.BuildConfig
import com.fantok.app.data.model.Video
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlayerManager @Inject constructor(
    @ApplicationContext private val context: Context
) : DefaultLifecycleObserver {

    private var serverUrl = BuildConfig.DEFAULT_SERVER_URL

    private var _player: ExoPlayer? = null
    val player: ExoPlayer
        get() = _player ?: createPlayer().also { _player = it }

    val isPlayerCreated: Boolean
        get() = _player != null

    private fun createPlayer(): ExoPlayer {
        return ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_ONE
            playWhenReady = true
            volume = 1f
        }
    }

    /**
     * 延迟初始化player，避免启动时创建
     */
    fun initializePlayer() {
        if (_player == null) {
            _player = createPlayer()
        }
    }

    fun prepareAt(index: Int, videos: List<Video>) {
        if (index < 0 || index >= videos.size) return
        // 延迟创建player，只在需要播放时创建
        initializePlayer()
        val uri = buildStreamUri(videos[index].id)
        val mediaItem = MediaItem.fromUri(uri)
        player.setMediaItem(mediaItem)
        player.prepare()
        player.playWhenReady = true
    }

    fun getServerUrl(): String {
        // Try to get from shared prefs, fallback to default
        return try {
            val prefs = context.getSharedPreferences("fantok_settings", Context.MODE_PRIVATE)
            prefs.getString("server_url", null) ?: BuildConfig.DEFAULT_SERVER_URL
        } catch (e: Exception) {
            BuildConfig.DEFAULT_SERVER_URL
        }
    }

    fun buildStreamUri(videoId: Int): String {
        val url = getServerUrl()
        return "${url.trimEnd('/')}/api/douyin/$videoId/stream"
    }

    fun pause() {
        _player?.playWhenReady = false
    }

    fun resume() {
        _player?.playWhenReady = true
    }

    fun release() {
        _player?.release()
        _player = null
    }

    // DefaultLifecycleObserver - register in MainActivity
    override fun onPause(owner: LifecycleOwner) = pause()
    override fun onResume(owner: LifecycleOwner) = resume()
    override fun onDestroy(owner: LifecycleOwner) = release()
}
