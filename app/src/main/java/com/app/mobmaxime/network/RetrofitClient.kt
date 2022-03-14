package com.app.mobmaxime.network

import com.app.mobmaxime.BuildConfig
import com.google.gson.GsonBuilder
import com.ihsanbal.logging.Level
import com.ihsanbal.logging.LoggingInterceptor
import okhttp3.OkHttpClient
import okhttp3.internal.platform.Platform
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.Executors

object RetrofitClient {

    private var retrofit: Retrofit? = null
    private val gson = GsonBuilder().setLenient().create()

    val client: Retrofit
        get() {
            if (retrofit == null) {
                synchronized(Retrofit::class.java) {
                    if (retrofit == null) {

                        val httpClient = OkHttpClient.Builder()
                                .addInterceptor(QueryParameterAddInterceptor())

                        httpClient.addInterceptor(
                                LoggingInterceptor.Builder()
                                        .loggable(BuildConfig.DEBUG)
                                        .setLevel(Level.BASIC)
                                        .log(Platform.INFO)
                                        .request("LOG")
                                        .response("LOG")
                                        .executor(Executors.newSingleThreadExecutor())
                                        .build())

                        val client = httpClient.build()

                        retrofit = Retrofit.Builder()
                                .baseUrl(BuildConfig.BASE_URL)
                                .addConverterFactory(GsonConverterFactory.create(gson))
                                .client(client)
                                .build()
                    }
                }

            }
            return retrofit!!
        }
}