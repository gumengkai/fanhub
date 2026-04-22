package com.fanhub.app.player

import android.content.Context
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.fanhub.app.data.api.BaseUrlInterceptor
import com.fanhub.app.data.local.SettingsDataStore
import com.fanhub.app.data.model.Video
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlayerManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsDataStore: SettingsDataStore,
    private val baseUrlInterceptor: BaseUrlInterceptor
) : DefaultLifecycleObserver {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var serverUrl = "http://192.168.31.40:11303"

    private var _player: ExoPlayer? = null
    val player: ExoPlayer
        get() = _player ?: createPlayer().also { _player = it }

    init {
        scope.launch {
            serverUrl = settingsDataStore.serverUrl.first()
            baseUrlInterceptor.setBaseUrl(serverUrl)
            settingsDataStore.serverUrl.collect {
                serverUrl = it
                baseUrlInterceptor.setBaseUrl(it)
            }
        }
    }

    private fun createPlayer(): ExoPlayer {
        return ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_ONE
            playWhenReady = true
            volume = 1f
        }
    }

    fun prepareAt(index: Int, videos: List<Video>) {
        if (index < 0 || index >= videos.size) return
        val uri = buildStreamUri(videos[index].id)
        val mediaItem = MediaItem.fromUri(uri)
        player.setMediaItem(mediaItem)
        player.prepare()
        player.playWhenReady = true
    }

    fun getServerUrl(): String = serverUrl

    fun buildStreamUri(videoId: Int): String =
        "${serverUrl.trimEnd('/')}/api/videos/$videoId/stream"

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