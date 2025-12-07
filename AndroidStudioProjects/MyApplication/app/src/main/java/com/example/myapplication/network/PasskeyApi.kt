package com.example.myapplication.network

import com.example.myapplication.network.model.VerifyRequest
import com.example.myapplication.network.model.VerifyResponse
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface PasskeyApi {
    @GET("register/options")
    suspend fun fetchRegisterOptions(): ResponseBody

    @POST("register/verify")
    suspend fun verifyRegistration(@Body body: VerifyRequest): VerifyResponse
}
