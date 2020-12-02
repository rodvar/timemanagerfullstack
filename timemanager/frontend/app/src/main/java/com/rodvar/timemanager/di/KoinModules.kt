package com.rodvar.timemanager.di

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.rodvar.timemanager.data.api.TimeManagerAPI
import com.rodvar.timemanager.data.networking.AuthInterceptor
import com.rodvar.timemanager.data.repository.TimeLogRepository
import com.rodvar.timemanager.data.repository.UserRepository
import com.rodvar.timemanager.feature.login.LoginViewModel
import com.rodvar.timemanager.feature.timelogs.MainViewModel
import com.rodvar.timemanager.feature.timelogs.TimeLogsViewModel
import com.rodvar.timemanager.feature.users.UsersViewModel
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory

const val BASE_URL = "http://10.0.2.2:8080/"

val viewModelModule = module {
    factory { UsersViewModel(get()) }
    factory { TimeLogsViewModel(get(), get()) }
    factory { MainViewModel(get()) }
    factory { LoginViewModel(get()) }
}

val repositoryModule = module {
    single { TimeLogRepository(get()) }
    single { UserRepository(get()) }
}

val networkingModule = module {
    factory { AuthInterceptor() }
    factory { HttpLoggingInterceptor().apply { this.level = HttpLoggingInterceptor.Level.BODY } }
    factory { provideGson() }
    factory { provideOkHttpClient(get(), get()) }
    single { provideApi(get()) }
    single { provideRetrofit(get(), get()) }
}

fun provideRetrofit(okHttpClient: OkHttpClient, gson: Gson): Retrofit {
    return Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(ScalarsConverterFactory.create())
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()
}

fun provideGson(): Gson = GsonBuilder()
    .setDateFormat("yyyy-MM-dd'T'HH:mm:ss")
    .create()

fun provideOkHttpClient(loggerInterceptor: HttpLoggingInterceptor,
                        authInterceptor: AuthInterceptor): OkHttpClient {
    return OkHttpClient().newBuilder()
//        .addNetworkInterceptor(NetworkInterceptor())
        .addInterceptor(loggerInterceptor)
        .addInterceptor(authInterceptor)
        .build()
}

//class NetworkInterceptor: Interceptor {
//    override fun intercept(chain: Interceptor.Chain): Response {
//        return try {
//            chain.proceed(chain.request())
//        } catch (t: Throwable) {
//            Log.e("Network Interceptor", t.localizedMessage, t)
//            fakeManageableResponse()
//        }
//    }
//
//    private fun fakeManageableResponse() = Response.Builder().build()
//
//}

fun provideApi(retrofit: Retrofit): TimeManagerAPI = retrofit.create(TimeManagerAPI::class.java)
