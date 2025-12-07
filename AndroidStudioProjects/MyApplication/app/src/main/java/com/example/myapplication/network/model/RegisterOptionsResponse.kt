package com.example.myapplication.network.model

data class RegisterOptionsResponse(
    val rpId: String,
    val challenge: String,
    val user: UserEntity,
    val pubKeyCredParams: List<PublicKeyParameter>
)

data class UserEntity(
    val id: String,
    val name: String,
    val displayName: String
)

data class PublicKeyParameter(
    val type: String,
    val alg: Int
)
