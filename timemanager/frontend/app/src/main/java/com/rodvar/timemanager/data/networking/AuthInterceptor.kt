package com.rodvar.timemanager.data.networking

import android.util.Log
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor : Interceptor {

    companion object {
        var sessionKey: String? = null
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        chain.request().let { request ->
            if (sessionKey == null)
                return chain.proceed(chain.request())
            else {
                Log.d(javaClass.simpleName, "Executing request with session $sessionKey")
                request.newBuilder().header("x-api-key", sessionKey!!)
                    .method(request.method, request.body)
                    .build().apply {
                        return chain.proceed(this)
                    }
            }
        }
    }
}