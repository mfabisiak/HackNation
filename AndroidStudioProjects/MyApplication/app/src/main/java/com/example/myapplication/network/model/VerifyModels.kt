package com.example.myapplication.network.model

data class VerifyRequest(
    val clientResponse: String
)

data class VerifyResponse(
    val success: Boolean,
    val message: String? = null
)
