package com.example.addictionreductionapp

import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.runBlocking
import org.junit.Test

class GeminiApiTest {
    @Test
    fun testGeminiApi() {
        runBlocking {
            val generativeModel = GenerativeModel(
                modelName = "gemini-2.0-flash",
                apiKey = "BuildConfig.GEMINI_API_KEY"
            )
            try {
                generativeModel.generateContent("Hello")
            } catch (e: Exception) {
                e.printStackTrace()
                throw e
            }
        }
    }
}
