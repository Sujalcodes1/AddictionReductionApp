package com.example.addictionreductionapp.utils

sealed class AuthResult {
    object Success : AuthResult()
    /** Supabase email confirmation is enabled — sign-up succeeded but session is pending. */
    object EmailConfirmationRequired : AuthResult()
    data class Failure(val message: String) : AuthResult()
}
