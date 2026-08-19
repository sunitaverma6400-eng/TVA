package com.sudhanshu.tva.network

import com.sudhanshu.tva.BuildConfig
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Central relay client. Every request automatically carries the X-App-Secret
 * header so the relay can authenticate the app without any AI provider key
 * ever touching this codebase.
 */
object RelayClient {

    private val authInterceptor = Interceptor { chain ->
        val request = chain.request().newBuilder()
            .addHeader("X-App-Secret", BuildConfig.RELAY_APP_SECRET)
            .build()
        chain.proceed(request)
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        // BODY logging can expose timeline text, locations, people and auth
        // headers in Logcat. Even debug builds should keep logs metadata-only.
        level = if (BuildConfig.DEBUG) {
            HttpLoggingInterceptor.Level.BASIC
        } else {
            HttpLoggingInterceptor.Level.NONE
        }
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(RetryInterceptor())
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(ensureTrailingSlash(BuildConfig.RELAY_URL))
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val api: RelayApi by lazy { retrofit.create(RelayApi::class.java) }

    private fun ensureTrailingSlash(url: String): String {
        require(url.startsWith("https://")) { "RELAY_URL must use HTTPS" }
        require(!url.contains("replace-me.onrender.com")) { "RELAY_URL is not configured" }
        return if (url.endsWith("/")) url else "$url/"
    }
}
