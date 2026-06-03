package com.example.data.api

import com.example.BuildConfig
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class GenerateContentRequest(
    val contents: List<Content>,
    val generationConfig: GenerationConfig? = null,
    val systemInstruction: Content? = null
)

@JsonClass(generateAdapter = true)
data class Content(
    val parts: List<Part>
)

@JsonClass(generateAdapter = true)
data class Part(
    val text: String? = null
)

@JsonClass(generateAdapter = true)
data class GenerationConfig(
    val responseMimeType: String? = null,
    val temperature: Float? = null
)

@JsonClass(generateAdapter = true)
data class GenerateContentResponse(
    val candidates: List<Candidate>?
)

@JsonClass(generateAdapter = true)
data class Candidate(
    val content: Content?
)

@JsonClass(generateAdapter = true)
data class GeminiSongOutput(
    val title: String,
    val artist: String,
    val originalKey: String,
    val content: String // Lyrics with inline chords in bracket format [C] [G] etc.
)

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

object RetrofitClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    val service: GeminiApiService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
        retrofit.create(GeminiApiService::class.java)
    }

    val songAdapter by lazy {
        moshi.adapter(GeminiSongOutput::class.java)
    }
}

object GeminiRepository {
    suspend fun fetchSongChords(query: String, searchModeUrl: Boolean = false): GeminiSongOutput? {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            throw IllegalStateException("Gemini API key is not configured. Please add your key in the Secrets panel in AI Studio.")
        }

        // Formulate a robust prompt to search and structure the song.
        val searchTypeMessage = if (searchModeUrl) {
            "extract the lyrics and aligned chords from this web page: '$query'."
        } else {
            "find the lyrics and aligned chords for the song: '$query'."
        }

        val prompt = """
            Please $searchTypeMessage 
            
            Format the response as a valid JSON object. Do not wrap the JSON output inside markdown block tags (like ```json).
            
            The JSON object MUST have exactly these property names:
            - "title": The title of the song (e.g. "Hotel California").
            - "artist": The artist or band name (e.g. "Eagles").
            - "originalKey": The estimated key of the song (e.g. "Bm", "C", "G", "Am").
            - "content": The lyrics with chords inline inside square brackets, rendered precisely above or at the beginning of the word/syllable they apply to. 
              Example content style:
              "[Am]On a dark desert highway, [E7]cool wind in my hair
              [G]Warm smell of colitas, [D]rising up through the air"

            Make sure the chords are accurate, standard musician chords (e.g. C, G, Am, F, Dm, C#m, Bb) and placed exactly where they should be played within the lyrics. Do not truncate the lyrics; provide the full song.
        """.trimIndent()

        val request = GenerateContentRequest(
            contents = listOf(
                Content(parts = listOf(Part(text = prompt)))
            ),
            generationConfig = GenerationConfig(
                responseMimeType = "application/json",
                temperature = 0.2f
            ),
            systemInstruction = Content(
                parts = listOf(Part(text = "You are an expert musicologist, chords transcriber, and lyric extractor."))
            )
        )

        val response = RetrofitClient.service.generateContent(apiKey, request)
        val textResponse = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
        
        return if (!textResponse.isNullOrBlank()) {
            try {
                RetrofitClient.songAdapter.fromJson(textResponse)
            } catch (e: Exception) {
                // In case of any loose formatting or brackets, clean up and try to parse
                val cleanedText = textResponse.trim().removeSurrounding("```json", "```").trim()
                RetrofitClient.songAdapter.fromJson(cleanedText)
            }
        } else {
            null
        }
    }
}
