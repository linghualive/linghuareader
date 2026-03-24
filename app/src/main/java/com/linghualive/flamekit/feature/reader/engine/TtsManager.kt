package com.linghualive.flamekit.feature.reader.engine

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TtsManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private var tts: TextToSpeech? = null
    private val _state = MutableStateFlow(TtsState())
    val state: StateFlow<TtsState> = _state.asStateFlow()

    private var sentences: List<String> = emptyList()
    private var currentIndex: Int = 0

    data class TtsState(
        val isPlaying: Boolean = false,
        val isPaused: Boolean = false,
        val isInitialized: Boolean = false,
        val speed: Float = 1.0f,
        val pitch: Float = 1.0f,
        val currentSentenceIndex: Int = 0,
        val totalSentences: Int = 0,
        val error: String? = null,
    )

    fun initialize() {
        if (tts != null) return
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.CHINESE
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        val index = utteranceId?.removePrefix("sentence_")?.toIntOrNull() ?: return
                        _state.update { it.copy(currentSentenceIndex = index) }
                    }

                    override fun onDone(utteranceId: String?) {
                        val index = utteranceId?.removePrefix("sentence_")?.toIntOrNull() ?: return
                        if (index + 1 < sentences.size) {
                            currentIndex = index + 1
                            speakSentence(currentIndex)
                        } else {
                            _state.update { it.copy(isPlaying = false, isPaused = false) }
                        }
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        _state.update { it.copy(error = "TTS 朗读出错", isPlaying = false) }
                    }
                })
                _state.update { it.copy(isInitialized = true) }
            } else {
                _state.update { it.copy(error = "TTS 初始化失败") }
            }
        }
    }

    fun speak(text: String) {
        sentences = splitSentences(text)
        if (sentences.isEmpty()) return
        currentIndex = 0
        _state.update {
            it.copy(
                isPlaying = true,
                isPaused = false,
                currentSentenceIndex = 0,
                totalSentences = sentences.size,
                error = null,
            )
        }
        tts?.setSpeechRate(_state.value.speed)
        tts?.setPitch(_state.value.pitch)
        speakSentence(0)
    }

    fun pause() {
        tts?.stop()
        _state.update { it.copy(isPlaying = false, isPaused = true) }
    }

    fun resume() {
        if (!_state.value.isPaused) return
        _state.update { it.copy(isPlaying = true, isPaused = false) }
        speakSentence(currentIndex)
    }

    fun stop() {
        tts?.stop()
        currentIndex = 0
        sentences = emptyList()
        _state.update {
            it.copy(
                isPlaying = false,
                isPaused = false,
                currentSentenceIndex = 0,
                totalSentences = 0,
            )
        }
    }

    fun setSpeed(speed: Float) {
        val clamped = speed.coerceIn(0.5f, 3.0f)
        tts?.setSpeechRate(clamped)
        _state.update { it.copy(speed = clamped) }
    }

    fun setPitch(pitch: Float) {
        val clamped = pitch.coerceIn(0.5f, 2.0f)
        tts?.setPitch(clamped)
        _state.update { it.copy(pitch = clamped) }
    }

    fun release() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        _state.update { TtsState() }
    }

    private fun speakSentence(index: Int) {
        if (index >= sentences.size) return
        val sentence = sentences[index]
        tts?.speak(sentence, TextToSpeech.QUEUE_FLUSH, null, "sentence_$index")
    }

    private fun splitSentences(text: String): List<String> {
        return text.split(Regex("[。！？；\n]+"))
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }
}
