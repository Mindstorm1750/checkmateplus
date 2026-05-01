package org.checkmateplus.voicecommandservice.service

import com.openai.models.chat.completions.ChatCompletionCreateParams
import com.fasterxml.jackson.annotation.JsonProperty
import com.openai.client.okhttp.OpenAIOkHttpClient
import com.openai.models.audio.AudioModel
import com.openai.models.audio.AudioResponseFormat
import com.openai.models.audio.transcriptions.TranscriptionCreateParams
import com.openai.models.audio.transcriptions.TranscriptionCreateResponse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.nio.file.Files
import java.nio.file.Path

/**
 * Service for querying OpenAI to get STT transcription and if necessary determine most likely move (if basic parsing fails)
 */
@Service
class OpenAiService {

    private val logger = LoggerFactory.getLogger(javaClass)
    private val client = OpenAIOkHttpClient.fromEnv()

    // dumb workaround because openai official lib expects path to file so we make a temp file
    fun transcribe(file: MultipartFile): String {
        // 1. Persist MultipartFile to a temp file to get a Path
        val suffix = inferExtension(file.originalFilename)
        val tempPath: Path = Files.createTempFile("upload-", suffix)

        file.inputStream.use { input ->
            Files.newOutputStream(tempPath).use { output ->
                input.copyTo(output)
            }
        }

        // Delete file on exit
        tempPath.toFile().deleteOnExit()

        // 2. Build request to openai
        val createParams = TranscriptionCreateParams.builder()
            .file(tempPath)
            .model(AudioModel.WHISPER_1)              // same as your snippet
            .responseFormat(AudioResponseFormat.TEXT) // or TEXT, JSON, etc.
            .build()

        // 3. Call API
        val rawResponse: TranscriptionCreateResponse = client.audio().transcriptions().create(createParams)

        // 4. Convert to the typed transcription helper and return text
        val transcription = rawResponse.asTranscription()

        // 5. Grab & trim text
        val transcriptionText = transcription.text().trim()

        logger.info("Transcribed audio into text: '${transcriptionText}'")

        return transcriptionText
    }

    private fun inferExtension(originalFilename: String?): String {
        if (originalFilename.isNullOrBlank()) return ".webm"
        val lower = originalFilename.lowercase()
        return when {
            lower.endsWith(".webm") -> ".webm"
            lower.endsWith(".wav")  -> ".wav"
            lower.endsWith(".mp3")  -> ".mp3"
            lower.contains('.')     -> originalFilename.substringAfterLast('.', missingDelimiterValue = "bin").let { ".$it" }
            else                    -> ".bin"
        }
    }

    data class DisambiguationResult(
        @JsonProperty(required = true) val from: String?,
        @JsonProperty(required = true) val to: String?,
        @JsonProperty(required = true) val promotion: String?,
        @JsonProperty(required = true) val confidence: Double,
        @JsonProperty(required = true) val explanation: String?
    )

    fun disambiguateMove(transcript: String, legalMoves: List<String>): DisambiguationResult {
        val movesCsv = legalMoves.take(60).joinToString(",")

        val prompt = """
        Transcript: "$transcript"
        Legal moves: $movesCsv
        RULES:
        - Choose the single best move from the list above that matches the speaker's intent.
        - If unsure, return null for from/to/promotion and 0.0 for confidence.
    """.trimIndent()

        val params = ChatCompletionCreateParams.builder()
            .addSystemMessage("You are a strict parser.")
            .addUserMessage(prompt)
            .model("gpt-4o-mini") // Highly efficient for constraint-based parsing
            .temperature(0.0) // Neutral weighting
            .responseFormat(DisambiguationResult::class.java) // Enforce output to adhere to this data class, so we don't have to increase prompt size to ask it to return in format, or waste time parsing it.
            .build()

        return client.chat().completions().create(params)
            .choices().first()
            .message()
            .content()
            .orElseThrow { IllegalStateException("Model returned an empty or invalid response format") }
    }

}
