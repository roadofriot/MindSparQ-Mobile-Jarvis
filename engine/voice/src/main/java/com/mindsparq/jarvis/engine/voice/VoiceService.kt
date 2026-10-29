package com.mindsparq.jarvis.engine.voice

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class VoiceState {
    IDLE,
    LISTENING,
    TRANSCRIBING,
    ERROR
}

enum class SupportedVoiceLocale(val tag: String, val displayName: String) {
    NEPALI("ne-NP", "नेपाली (Nepali)"),
    ENGLISH("en-US", "English (United States)")
}

class VoiceService {

    private val _voiceState = MutableStateFlow(VoiceState.IDLE)
    val voiceState: StateFlow<VoiceState> = _voiceState.asStateFlow()

    private val _lastTranscribedText = MutableStateFlow("")
    val lastTranscribedText: StateFlow<String> = _lastTranscribedText.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private var activeLocale: SupportedVoiceLocale = SupportedVoiceLocale.ENGLISH

    fun startListening(locale: SupportedVoiceLocale): Boolean {
        if (_voiceState.value == VoiceState.LISTENING) return false
        activeLocale = locale
        _errorMessage.value = null
        _voiceState.value = VoiceState.LISTENING
        return true
    }

    fun stopListening() {
        if (_voiceState.value == VoiceState.LISTENING) {
            _voiceState.value = VoiceState.TRANSCRIBING
        }
    }

    fun onSpeechRecognized(text: String) {
        val trimmed = text.trim()
        _lastTranscribedText.value = trimmed
        _voiceState.value = VoiceState.IDLE
    }

    fun onError(error: String) {
        _errorMessage.value = error
        _voiceState.value = VoiceState.ERROR
    }

    fun getActiveLocaleTag(): String = activeLocale.tag
}

