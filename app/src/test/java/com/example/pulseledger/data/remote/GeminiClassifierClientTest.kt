package com.example.pulseledger.data.remote

import com.example.pulseledger.data.security.KeyStorageProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GeminiClassifierClientTest {

    private val fakeKeyStorage = object : KeyStorageProvider {
        override fun getGeminiApiKey(): String? = "fake_test_key"
        override fun setGeminiApiKey(apiKey: String) {}
        override fun clearGeminiApiKey() {}
    }

    private val client = GeminiClassifierClient(fakeKeyStorage)

    @Test
    fun testParseValidCommercialResponse() {
        val sampleResponse = """
            {
              "candidates": [
                {
                  "content": {
                    "parts": [
                      {
                        "text": "{\"category\":\"Food & Dining\",\"cleanMerchantName\":\"Haldiram's\",\"isP2pPersonName\":false,\"confidence\":0.96}"
                      }
                    ]
                  }
                }
              ]
            }
        """.trimIndent()

        val parsed = client.parseGeminiStructuredResponse(sampleResponse)
        assertNotNull(parsed)
        assertEquals("Food & Dining", parsed!!.category)
        assertEquals("Haldiram's", parsed.cleanMerchantName)
        assertFalse(parsed.isP2pPersonName)
        assertEquals(0.96, parsed.confidence, 0.001)
    }

    @Test
    fun testParseStrictP2pResponse() {
        val sampleP2pResponse = """
            {
              "candidates": [
                {
                  "content": {
                    "parts": [
                      {
                        "text": "{\"category\":\"P2P_Unknown\",\"cleanMerchantName\":\"Suresh Yadav\",\"isP2pPersonName\":true,\"confidence\":1.0}"
                      }
                    ]
                  }
                }
              ]
            }
        """.trimIndent()

        val parsed = client.parseGeminiStructuredResponse(sampleP2pResponse)
        assertNotNull(parsed)
        assertEquals("P2P_Unknown", parsed!!.category)
        assertEquals("Suresh Yadav", parsed.cleanMerchantName)
        assertTrue(parsed.isP2pPersonName)
        assertEquals(1.0, parsed.confidence, 0.001)
    }

}

