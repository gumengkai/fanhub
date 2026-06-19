package com.fanhub.app.player

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.RenderersFactory
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.extractor.DefaultExtractorsFactory
import com.fanhub.app.data.api.BaseUrlInterceptor
import com.fanhub.app.data.local.SettingsDataStore
import com.fanhub.app.data.model.Video
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
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

    @androidx.annotation.OptIn(UnstableApi::class)
    private fun createPlayer(): ExoPlayer {
        // 创建支持扩展格式的 RenderersFactory
        val renderersFactory: RenderersFactory = DefaultRenderersFactory(context)
            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)

        // 配置 OkHttp 数据源以支持流式传输
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(baseUrlInterceptor)
            .build()
        
        val okHttpDataSourceFactory = OkHttpDataSource.Factory(okHttpClient)
        val defaultDataSourceFactory = DefaultDataSource.Factory(context, okHttpDataSourceFactory)
        
        // 配置 ExtractorsFactory 支持更多格式
        val extractorsFactory = DefaultExtractorsFactory()
            .setConstantBitrateSeekingEnabled(true)

        // 创建 MediaSourceFactory
        val mediaSourceFactory = DefaultMediaSourceFactory(defaultDataSourceFactory, extractorsFactory)

        return ExoPlayer.Builder(context)
            .setRenderersFactory(renderersFactory)
            .setMediaSourceFactory(mediaSourceFactory)
            .build()
            .apply {
                repeatMode = Player.REPEAT_MODE_ONE
                playWhenReady = true
                volume = 1f
            }
    }

    /**
     * 准备播放视频。如果视频需要外部播放器（如 AVI 格式），则返回 true 表示需要使用系统播放器。
     */
    fun prepareAt(index: Int, videos: List<Video>): Boolean {
        if (index < 0 || index >= videos.size) return false
        val video = videos[index]
        
        // 检查是否需要外部播放器
        if (video.requiresExternalPlayer) {
            // 对于需要外部播放器的格式，返回 true 让调用方处理
            return true
        }
        
        val uri = buildStreamUri(video.id)
        val mediaItem = MediaItem.fromUri(uri)
        player.setMediaItem(mediaItem)
        player.prepare()
        player.playWhenReady = true
        return false
    }
    
    /**
     * 使用系统播放器播放视频（用于不支持的视频格式如 AVI）
     */
    fun playWithExternalPlayer(video: Video) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(Uri.parse(buildStreamUri(video.id)), "video/*")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        
        // 尝试启动系统播放器
        val packageManager = context.packageManager
        val activities = packageManager.queryIntentActivities(intent, 0)
        
        if (activities.isNotEmpty()) {
            // 创建选择器让用户选择播放器
            val chooser = Intent.createChooser(intent, "选择播放器")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        }
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