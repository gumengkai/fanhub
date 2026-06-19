package com.fanhub.app.data.api

import kotlinx.coroutines.flow.MutableStateFlow
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Dynamically replaces the host:port of every request with the user-configured server URL.
 * The serverUrl StateFlow is updated by SettingsViewModel whenever the user saves a new URL.
 * No Retrofit rebuild required.
 */
@Singleton
class BaseUrlInterceptor @Inject constructor() : Interceptor {

    val serverUrl = MutableStateFlow("http://192.168.31.40:11303")

    fun setBaseUrl(url: String) {
        serverUrl.value = url
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val base = serverUrl.value.trimEnd('/')

        val scheme = if (base.startsWith("https")) "https" else "http"
        val hostPort = base.removePrefix("https://").removePrefix("http://")
        val host = hostPort.substringBefore(":")
        val port = hostPort.substringAfter(":", "11303").toIntOrNull() ?: 11303

        val newUrl = original.url.newBuilder()
            .scheme(scheme)
            .host(host)
            .port(port)
            .build()

        return chain.proceed(original.newBuilder().url(newUrl).build())
    }
}
