package com.mindsparq.jarvis.engine.voice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceServiceTest {

    @Test
    fun testVoiceServiceNepaliLocaleAndStateFlow() {
        val voiceService = VoiceService()
        assertEquals(VoiceState.IDLE, voiceService.voiceState.value)

        // Start listening in Nepali
        val started = voiceService.startListening(SupportedVoiceLocale.NEPALI)
        assertTrue(started)
        assertEquals(VoiceState.LISTENING, voiceService.voiceState.value)
        assertEquals("ne-NP", voiceService.getActiveLocaleTag())

        // Stop listening -> enters transcribing
        voiceService.stopListening()
        assertEquals(VoiceState.TRANSCRIBING, voiceService.voiceState.value)

        // Results arrive
        voiceService.onSpeechRecognized("नयाँ प्रोजेक्ट बनाऊ")
        assertEquals(VoiceState.IDLE, voiceService.voiceState.value)
        assertEquals("नयाँ प्रोजेक्ट बनाऊ", voiceService.lastTranscribedText.value)
    }

    @Test
    fun testVoiceErrorState() {
        val voiceService = VoiceService()
        voiceService.startListening(SupportedVoiceLocale.ENGLISH)
        voiceService.onError("Microphone permission denied")

        assertEquals(VoiceState.ERROR, voiceService.voiceState.value)
        assertEquals("Microphone permission denied", voiceService.errorMessage.value)
    }
}

