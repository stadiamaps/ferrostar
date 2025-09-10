package com.stadiamaps.ferrostar.core

import android.content.Context
import android.media.AudioManager
import android.speech.tts.TextToSpeech
import android.util.Log
import app.cash.turbine.test
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import java.util.UUID
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertTrue
import kotlin.uuid.ExperimentalUuidApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import uniffi.ferrostar.SpokenInstruction

class AndroidTextToSpeechTest {

  private val engine: String = "ferrostar.test"

  private lateinit var context: Context
  private lateinit var androidTts: AndroidTtsObserver

  @Before
  fun setUp() {
    mockkStatic(Log::class)
    every { Log.e(any(), any()) } returns 0
    every { Log.w(any<String>(), any<String>()) } returns 0

    context = mockk(relaxed = true)
    val audioManager = mockk<AudioManager>(relaxed = true)
    every { context.getSystemService(Context.AUDIO_SERVICE) } returns audioManager

    androidTts = AndroidTtsObserver(context = context, engine = engine)
  }

  @Test
  fun testStart() {
    val observer = mockk<AndroidTtsStatusListener>()
    androidTts.statusObserver = observer
    androidTts.start()

    assertNotNull(androidTts.tts)
  }

  @Test
  fun `test shutdown`() {
    val tts = mockk<TextToSpeech>(relaxed = true)
    androidTts.start(tts)
    androidTts.shutdown()

    verify { tts.shutdown() }
    assertNull(androidTts.tts)
  }

  @Test
  fun `test setMuted while speaking`() = runTest {
    val tts = mockk<TextToSpeech>(relaxed = true) { every { isSpeaking } returns true }
    androidTts.start(tts)
    androidTts.setMuted(true)

    verify { tts.stop() }

    androidTts.muteState.test { assertTrue(awaitItem()) }

    androidTts.setMuted(false)

    androidTts.muteState.test { assertFalse(awaitItem()) }
  }

  @Test
  fun `test setMuted without speaking`() {
    val tts = mockk<TextToSpeech>(relaxed = true) { every { isSpeaking } returns false }
    androidTts.start(tts)
    androidTts.setMuted(true)

    verify(exactly = 0) { tts.stop() }
  }

  @Test
  fun `test stop and clear`() {
    val tts = mockk<TextToSpeech>(relaxed = true)
    androidTts.start(tts)
    androidTts.stopAndClearQueue()

    verify { tts.stop() }
  }

  @OptIn(ExperimentalUuidApi::class)
  @Test
  fun `test on speak`() {
    val tts = mockk<TextToSpeech>(relaxed = true)
    every { tts.setOnUtteranceProgressListener(any()) } returns TextToSpeech.SUCCESS

    androidTts.start(tts)
    androidTts.setMuted(false)
    androidTts.onInit(TextToSpeech.SUCCESS)

    val uuid = UUID.randomUUID()
    val instruction =
        mockk<SpokenInstruction> {
          every { text } returns "Hello, World!"
          every { utteranceId } returns uuid
        }

    androidTts.onSpokenInstructionTrigger(instruction)

    verify { tts.speak("Hello, World!", TextToSpeech.QUEUE_ADD, any(), uuid.toString()) }
  }

  @OptIn(ExperimentalUuidApi::class)
  @Test
  fun `test on speak error`() {
    val tts = mockk<TextToSpeech>(relaxed = true)
    every { tts.speak(any(), any(), any(), any()) } returns TextToSpeech.ERROR

    androidTts.start(tts)
    androidTts.setMuted(false)
    androidTts.onInit(TextToSpeech.SUCCESS)

    val observer = mockk<AndroidTtsStatusListener>()
    every { observer.onTtsSpeakError(any(), any()) } returns Unit

    androidTts.statusObserver = observer

    val uuid = UUID.randomUUID()
    val instruction =
        mockk<SpokenInstruction> {
          every { text } returns "Hello, World!"
          every { utteranceId } returns uuid
        }

    androidTts.onSpokenInstructionTrigger(instruction)

    verify { Log.e(any(), any()) }
    verify { observer.onTtsSpeakError(uuid.toString(), TextToSpeech.ERROR) }
  }

  @OptIn(ExperimentalUuidApi::class)
  @Test
  fun `test on speech while muted`() {
    val tts = mockk<TextToSpeech>(relaxed = true)
    androidTts.start(tts)
    androidTts.setMuted(true)
    androidTts.onInit(TextToSpeech.SUCCESS)

    val uuid = UUID.randomUUID()
    val instruction =
        mockk<SpokenInstruction> {
          every { text } returns "Hello, World!"
          every { utteranceId } returns uuid
        }

    androidTts.onSpokenInstructionTrigger(instruction)

    verify(exactly = 0) {
      tts.speak("Hello, World!", TextToSpeech.QUEUE_ADD, any(), uuid.toString())
    }
  }

  @OptIn(ExperimentalUuidApi::class)
  @Test
  fun `test on speech when not successful`() {
    val tts = mockk<TextToSpeech>(relaxed = true)
    androidTts.start(tts)
    androidTts.setMuted(false)
    androidTts.onInit(TextToSpeech.ERROR)

    val uuid = UUID.randomUUID()
    val instruction =
        mockk<SpokenInstruction> {
          every { text } returns "Hello, World!"
          every { utteranceId } returns uuid
        }

    androidTts.onSpokenInstructionTrigger(instruction)

    verify { Log.e(any(), any()) }
    verify(exactly = 0) {
      tts.speak("Hello, World!", TextToSpeech.QUEUE_ADD, any(), uuid.toString())
    }
  }
}
