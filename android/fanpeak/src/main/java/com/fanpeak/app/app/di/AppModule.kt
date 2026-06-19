package com.fanpeak.app.di

import com.fanpeak.app.data.api.ApiClient
import com.fanpeak.app.data.api.ApiService
import com.fanpeak.app.data.api.BaseUrlInterceptor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideBaseUrlInterceptor(): BaseUrlInterceptor = BaseUrlInterceptor()

    @Provides
    @Singleton
    fun provideApiClient(interceptor: BaseUrlInterceptor): ApiClient =
        ApiClient(interceptor)

    @Provides
    @Singleton
    fun provideApiService(client: ApiClient): ApiService = client.service
}
