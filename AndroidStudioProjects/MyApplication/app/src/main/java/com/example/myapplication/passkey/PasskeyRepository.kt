package com.example.myapplication.passkey

import android.content.Context
import android.util.Log
import androidx.credentials.CreatePublicKeyCredentialRequest
import androidx.credentials.CreatePublicKeyCredentialResponse
import androidx.credentials.CredentialManager
import com.example.myapplication.network.PasskeyApi
import com.example.myapplication.network.model.VerifyRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

private const val PASSKEY_BASE_URL = "http://localhost:3005/"

class PasskeyRepository(context: Context) {
    private val api: PasskeyApi
    private val credentialManager = CredentialManager.create(context)

    init {
        val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }
        val httpClient = OkHttpClient.Builder().addInterceptor(logging).build()

        api = Retrofit.Builder()
            .baseUrl(PASSKEY_BASE_URL)
            .client(httpClient)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
            .create(PasskeyApi::class.java)
    }

    suspend fun registerPasskey(credentialContext: Context): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val optionsJson = api.fetchRegisterOptions().useToString()
            val createResponse = createCredential(optionsJson, credentialContext)
            verifyWithServer(createResponse)
        }.onSuccess { Log.d("PasskeyRepo", "Passkey registered" ) }
            .onFailure { Log.e("PasskeyRepo", "Registration failed", it) }
            .getOrDefault(false)
    }

    private suspend fun createCredential(optionsJson: String, credentialContext: Context): CreatePublicKeyCredentialResponse {
        val request = CreatePublicKeyCredentialRequest(optionsJson)
        val result = credentialManager.createCredential(credentialContext, request)
        return result as CreatePublicKeyCredentialResponse
    }

    private suspend fun verifyWithServer(response: CreatePublicKeyCredentialResponse): Boolean {
        val verify = api.verifyRegistration(VerifyRequest(response.registrationResponseJson))
        return verify.success
    }
}

private fun ResponseBody.useToString(): String = use { it.string() }
