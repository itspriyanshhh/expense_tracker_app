package com.example.pulseledger.data.security

interface KeyStorageProvider {
    fun getGeminiApiKey(): String?
    fun setGeminiApiKey(apiKey: String)
    fun clearGeminiApiKey()
}
