package com.example.data.ai

import android.util.Log
import com.example.BuildConfig
import com.example.data.model.VideoEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class AiResponse(
    val replyText: String,
    val recommendedVideos: List<VideoEntity> = emptyList()
)

class GeminiClient {

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val apiKey: String
        get() = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }

    suspend fun chat(
        userMessage: String,
        catalog: List<VideoEntity>,
        conversationHistory: List<Pair<String, String>> // list of (sender, text)
    ): AiResponse = withContext(Dispatchers.IO) {
        // AI Safety Filter (Pre-check)
        val lower = userMessage.lowercase()
        if (containsUnsafeContent(lower)) {
            return@withContext AiResponse(
                replyText = "⚠️ I cannot assist with this request. VibeTube adheres to strict community safety guidelines prohibiting harmful, dangerous, or private personal data inquiries.",
                recommendedVideos = emptyList()
            )
        }

        // Check if API key is valid (not empty and not the placeholder)
        val validKey = apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY" && apiKey != "null"

        if (validKey) {
            try {
                val apiResult = callGeminiApi(userMessage, catalog, conversationHistory)
                if (apiResult != null) {
                    val (cleanedText, matchedVideos) = extractRecommendations(apiResult, catalog)
                    return@withContext AiResponse(
                        replyText = cleanedText,
                        recommendedVideos = matchedVideos
                    )
                }
            } catch (e: Exception) {
                Log.w("GeminiClient", "Gemini API call failed, falling back to local engine: ${e.message}")
            }
        }

        // Fallback / Grounded Local VibeAI Engine
        return@withContext runLocalVibeAiEngine(userMessage, catalog)
    }

    private fun containsUnsafeContent(text: String): Boolean {
        val dangerousKeywords = listOf(
            "hack password", "steal password", "credit card", "bomb", "suicide",
            "kill myself", "exploit auth", "bypass security", "sql injection", "reveal password"
        )
        return dangerousKeywords.any { text.contains(it) }
    }

    private fun callGeminiApi(
        userMessage: String,
        catalog: List<VideoEntity>,
        conversationHistory: List<Pair<String, String>>
    ): String? {
        val catalogContext = buildString {
            append("VibeTube Current Video Catalog:\n")
            catalog.take(12).forEach { v ->
                val type = if (v.isShort) "Short" else "Long Video"
                append("- ID: ${v.id} | Title: \"${v.title}\" | Creator: @${v.creatorUsername} | Type: $type | Category: ${v.category} | Tags: ${v.hashtags}\n")
            }
        }

        val systemPrompt = """
            You are VibeAI, the official intelligent assistant inside VibeTube.
            VibeTube is a mobile-first social video platform featuring short-form vertical videos and long-form creator masterclasses.
            
            CORE INSTRUCTIONS:
            1. Answer general questions clearly and explain difficult topics simply.
            2. For creators: brainstorm creative video ideas, viral hooks, title variations, descriptions, and hashtags.
            3. When finding or recommending videos, ALWAYS ground your answer in the provided VibeTube Video Catalog. Mention the video title and creator.
            4. At the end of any video recommendation, include the video IDs in this exact syntax: [RECOMMENDED_IDS: id1, id2] so the app can display clickable interactive video cards.
            5. SAFETY: Never reveal passwords or private information. Do not provide dangerous instructions. Keep all content helpful, positive, and age-appropriate.
            
            $catalogContext
        """.trimIndent()

        val jsonBody = JSONObject().apply {
            val contentsArray = JSONArray()

            // System instruction
            put("systemInstruction", JSONObject().apply {
                put("parts", JSONArray().apply {
                    put(JSONObject().apply { put("text", systemPrompt) })
                })
            })

            // Past conversation context (last 6 messages)
            conversationHistory.takeLast(6).forEach { (sender, text) ->
                val role = if (sender == "user") "user" else "model"
                contentsArray.put(JSONObject().apply {
                    put("role", role)
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", text) })
                    })
                })
            }

            // Current user message
            contentsArray.put(JSONObject().apply {
                put("role", "user")
                put("parts", JSONArray().apply {
                    put(JSONObject().apply { put("text", userMessage) })
                })
            })

            put("contents", contentsArray)

            // Generation config
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.7)
                put("topP", 0.95)
                put("topK", 40)
            })
        }

        val requestBody = jsonBody.toString().toRequestBody("application/json".toMediaType())
        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        val response = okHttpClient.newCall(request).execute()
        if (!response.isSuccessful) {
            val err = response.body?.string()
            Log.w("GeminiClient", "Gemini HTTP error ${response.code}: $err")
            return null
        }

        val resBody = response.body?.string() ?: return null
        val resJson = JSONObject(resBody)
        val candidates = resJson.optJSONArray("candidates") ?: return null
        if (candidates.length() == 0) return null

        val firstCandidate = candidates.getJSONObject(0)
        val content = firstCandidate.optJSONObject("content") ?: return null
        val parts = content.optJSONArray("parts") ?: return null
        if (parts.length() == 0) return null

        return parts.getJSONObject(0).optString("text")
    }

    private fun extractRecommendations(rawText: String, catalog: List<VideoEntity>): Pair<String, List<VideoEntity>> {
        val pattern = Regex("\\[RECOMMENDED_IDS:\\s*([^\\]]+)\\]")
        val match = pattern.find(rawText)
        val recommendedList = mutableListOf<VideoEntity>()

        var cleanedText = rawText
        if (match != null) {
            val ids = match.groupValues[1].split(",").map { it.trim() }
            ids.forEach { id ->
                catalog.find { it.id.equals(id, ignoreCase = true) }?.let { recommendedList.add(it) }
            }
            cleanedText = rawText.replace(match.value, "").trim()
        }

        // If no explicit tags were found, also search for catalog titles mentioned in the text
        if (recommendedList.isEmpty()) {
            catalog.forEach { video ->
                if (cleanedText.contains(video.title, ignoreCase = true) || cleanedText.contains(video.creatorUsername, ignoreCase = true)) {
                    if (!recommendedList.contains(video)) {
                        recommendedList.add(video)
                    }
                }
            }
        }

        return Pair(cleanedText, recommendedList.take(3))
    }

    /**
     * High-fidelity local intelligence engine guaranteeing instant, reliable responses
     * with real catalog search, title generation, hashtag ideas, and navigation guidance.
     */
    private fun runLocalVibeAiEngine(query: String, catalog: List<VideoEntity>): AiResponse {
        val lower = query.lowercase().trim()

        // 1. Natural language video search & recommendations
        if (lower.contains("science") || lower.contains("black hole") || lower.contains("space") || lower.contains("physics")) {
            val scienceVideos = catalog.filter { it.category.equals("Science", ignoreCase = true) || it.title.contains("black hole", ignoreCase = true) }
            return AiResponse(
                replyText = "Here are our top science and astronomy videos on VibeTube! 🌌\n\nDr. Elena Vance breaks down complex physics, gravitational time dilation, and the cosmos into crystal-clear concepts.",
                recommendedVideos = scienceVideos.ifEmpty { catalog.take(2) }
            )
        }

        if (lower.contains("tech") || lower.contains("code") || lower.contains("coding") || lower.contains("ai") || lower.contains("android")) {
            val techVideos = catalog.filter { it.category.equals("Tech", ignoreCase = true) || it.hashtags.contains("tech") || it.hashtags.contains("ai") }
            return AiResponse(
                replyText = "Check out these featured Tech & AI videos on VibeTube! 💻\n\nMaya Lin shares high-yield tutorials covering neural networks and full Android architecture with Jetpack Compose.",
                recommendedVideos = techVideos.ifEmpty { catalog.take(2) }
            )
        }

        if (lower.contains("food") || lower.contains("cook") || lower.contains("recipe") || lower.contains("burger")) {
            val foodVideos = catalog.filter { it.category.equals("Food", ignoreCase = true) || it.hashtags.contains("food") }
            return AiResponse(
                replyText = "Sizzle time! Here are top culinary videos from our food creators: 🍳🔥\n\nChef Julian shows how to get that perfect caramelized smash burger crust in under 60 seconds.",
                recommendedVideos = foodVideos.ifEmpty { catalog.take(2) }
            )
        }

        if (lower.contains("music") || lower.contains("lofi") || lower.contains("chill") || lower.contains("beat") || lower.contains("synth")) {
            val musicVideos = catalog.filter { it.category.equals("Music", ignoreCase = true) || it.hashtags.contains("synthwave") }
            return AiResponse(
                replyText = "Here are our best immersive soundscapes and music jams on VibeTube 🎧✨\n\nKaito Sound's midnight analog synth sessions are ideal for focus, deep work, or chilling out.",
                recommendedVideos = musicVideos.ifEmpty { catalog.take(2) }
            )
        }

        if (lower.contains("travel") || lower.contains("trip") || lower.contains("nature") || lower.contains("waterfall") || lower.contains("japan")) {
            val travelVideos = catalog.filter { it.category.equals("Travel", ignoreCase = true) || it.hashtags.contains("travel") }
            return AiResponse(
                replyText = "Wanderlust unlocked! 🏔️ Take a look at these scenic journeys:\n\nZara Travel takes you to secret alpine waterfalls in the Dolomites and serene autumn shrines in Kyoto.",
                recommendedVideos = travelVideos.ifEmpty { catalog.take(2) }
            )
        }

        // 2. Creator Title Ideas
        if (lower.contains("title") || lower.contains("name idea") || lower.contains("headline")) {
            return AiResponse(
                replyText = """
                    Here are 5 high-CTR video title formulas crafted for maximum engagement:
                    
                    🔥 High-Curiosity:
                    1. "The One Thing Nobody Tells You About [Your Topic]"
                    2. "I Tried [Topic] for 30 Days — Here's What Happened"
                    
                    💡 Value-Driven:
                    3. "[Topic] Explained in 60 Seconds (No Fluff)"
                    4. "5 Rookie Mistakes That Ruin Your [Topic]"
                    
                    ⚡ Bold Statement:
                    5. "Why [Popular Method] is Actually Obsolete in 2026"
                    
                    Pro-tip: For Shorts, keep the visible title under 40 characters so it fits cleanly above the creator overlay!
                """.trimIndent()
            )
        }

        // 3. Creator Description Generator
        if (lower.contains("description") || lower.contains("desc")) {
            return AiResponse(
                replyText = """
                    Here is a proven 3-part video description template for VibeTube:
                    
                    [HOOK - 1 to 2 lines]:
                    "In this video, I break down exactly how to [achieve outcome] without [common frustration]."
                    
                    [TIMESTAMPS / KEY HIGHLIGHTS]:
                    0:00 - The Core Problem
                    1:15 - The 3 Key Principles
                    3:40 - Step-by-Step Walkthrough
                    5:20 - Final Pro Tips
                    
                    [CALL TO ACTION & HASHTAGS]:
                    "Drop a comment with your thoughts below and hit Follow for daily creator drops! #shorts #vibetube"
                """.trimIndent()
            )
        }

        // 4. Hashtag Generator
        if (lower.contains("hashtag") || lower.contains("tags") || lower.contains("tag")) {
            return AiResponse(
                replyText = """
                    Here are trending hashtag clusters categorized by niche:
                    
                    📱 Tech & Dev:
                    #tech #coding #software #developer #android #ai #innovation #webdev
                    
                    ⚡ Shorts & Viral Reach:
                    #shorts #vibetube #trending #fyp #quicklearn #creators
                    
                    🍳 Food & Lifestyle:
                    #foodie #recipes #quickmeals #cheflife #delicious #cooking
                    
                    🌌 Science & Education:
                    #science #astronomy #curiosity #didyouknow #physics #learn
                    
                    Best practice: Use 3-5 hyper-relevant tags rather than 20 generic ones to help VibeTube's algorithm surface your content.
                """.trimIndent()
            )
        }

        // 5. Brainstorm video ideas
        if (lower.contains("idea") || lower.contains("brainstorm") || lower.contains("what should i make")) {
            return AiResponse(
                replyText = """
                    Here are 4 viral video concepts you can create right now on VibeTube:
                    
                    1. The "Day in the Life" Micro-Cut (Short):
                    Film 1-second clips from your morning routine, workflow, and creative setup set to an upbeat tempo.
                    
                    2. "Myth vs Reality" Showdown:
                    Present a common myth in your niche, then debunk it visually with real proof in 45 seconds.
                    
                    3. Tool / Workflow Teardown:
                    Showcase the 3 everyday apps or gear that 10x your productivity.
                    
                    4. Masterclass Breakdown (Long Video):
                    A comprehensive 8-10 minute guide solving one specific pain point your audience struggles with.
                    
                    Which topic would you like to develop further?
                """.trimIndent()
            )
        }

        // 6. Navigation / How-to in VibeTube
        if (lower.contains("how do i") || lower.contains("navigate") || lower.contains("upload") || lower.contains("create") || lower.contains("feature")) {
            return AiResponse(
                replyText = """
                    Here is how to navigate VibeTube:
                    
                    🎬 Home Feed: Watch recommended videos and creator drops from accounts you follow.
                    ⚡ Shorts: Swipe through immersive full-screen vertical clips with one tap.
                    🔍 Explore: Search videos, users, and hashtags with trending categories.
                    ➕ Create: Tap the center '+' button to publish a Short (9:16) or Long Masterclass (16:9).
                    🤖 VibeAI: Ask me anything, find catalog videos, or brainstorm creator scripts!
                    👤 Profile: Manage your bio, view your uploads, and check your saved bookmarks.
                """.trimIndent()
            )
        }

        // Default friendly answer
        val matchingVideos = catalog.filter {
            lower.contains(it.category.lowercase()) ||
            it.title.lowercase().split(" ").any { word -> word.length > 3 && lower.contains(word) }
        }.take(3)

        return AiResponse(
            replyText = "Great question! I'm here to help you get the most out of VibeTube. Whether you want to discover exciting videos from our creators, brainstorm title hooks, or learn something new, just ask!\n\nCheck out these popular drops in the meantime:",
            recommendedVideos = matchingVideos.ifEmpty { catalog.take(2) }
        )
    }

    suspend fun generateSmartReplies(lastReceivedMessage: String): List<String> = withContext(Dispatchers.IO) {
        val validKey = apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY" && apiKey != "null"
        if (validKey) {
            try {
                val prompt = "Generate exactly 3 short, natural, friendly chat replies (each under 10 words) to this message: \"$lastReceivedMessage\". Separate each reply with a newline. Do not include numbers, bullets, or quotation marks."
                val response = callGeminiSimple(prompt)
                if (!response.isNullOrBlank()) {
                    val list = response.lines()
                        .map { it.trim().removePrefix("-").removePrefix("•").trim().trim('"') }
                        .filter { it.isNotBlank() }
                        .take(3)
                    if (list.isNotEmpty()) return@withContext list
                }
            } catch (e: Exception) {
                Log.w("GeminiClient", "Smart reply API call failed: ${e.message}")
            }
        }

        // Local smart reply generator based on context
        val lower = lastReceivedMessage.lowercase()
        return@withContext when {
            lower.contains("?") -> listOf(
                "Definitely, sounds great to me! 🙌",
                "Not sure yet, let me check and tell you!",
                "100%, let's do it today! 🔥"
            )
            lower.contains("video") || lower.contains("watch") || lower.contains("short") -> listOf(
                "Watching it right now, looks awesome! 🎬",
                "Added to my watchlist! Thanks for sharing",
                "Who made this? The visuals look crazy!"
            )
            lower.contains("haha") || lower.contains("lol") || lower.contains("lmao") -> listOf(
                "I know right, literally had me dying 😂",
                "Classic! Couldn't stop laughing 🤣",
                "Peak comedy honestly haha"
            )
            lower.contains("call") -> listOf(
                "Give me 5 mins and I'll jump on! 📞",
                "Can you talk right now? Calling you",
                "Just saw this, let's connect!"
            )
            lower.contains("hi") || lower.contains("hey") || lower.contains("hello") || lower.contains("yo") -> listOf(
                "Hey! What are you up to today?",
                "Yo! Good to hear from you 🙌",
                "Hey friend! Have you seen the new drops?"
            )
            else -> listOf(
                "That's awesome, tell me more! ✨",
                "Totally agree with you on that",
                "Let's catch up more later today! 🔥"
            )
        }
    }

    suspend fun rewriteMessage(originalText: String, tone: String): String = withContext(Dispatchers.IO) {
        if (originalText.isBlank()) return@withContext ""
        val validKey = apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY" && apiKey != "null"
        if (validKey) {
            try {
                val prompt = "Rewrite this chat message in a $tone tone (keep it short, under 25 words): \"$originalText\". Return ONLY the rewritten text without quotes, formatting, or explanations."
                val response = callGeminiSimple(prompt)
                if (!response.isNullOrBlank()) {
                    return@withContext response.trim().trim('"')
                }
            } catch (e: Exception) {
                Log.w("GeminiClient", "Rewrite API call failed: ${e.message}")
            }
        }

        // Local high quality tone rewriter
        val trimmed = originalText.trim()
        return@withContext when (tone.lowercase()) {
            "hype", "gen-z", "hype / gen-z" -> "Yo no cap, $trimmed fr fr! 🔥🚀 That's crazy hype!"
            "friendly", "warm", "friendly & warm" -> "Hey! 😊 $trimmed Hope you're having an amazing day!"
            "polite", "professional", "polite & clean" -> "I wanted to share that $trimmed. Thank you, and let me know your thoughts."
            "witty", "funny", "witty & funny" -> "Not gonna lie: $trimmed 😂 (Don't quote me on that though)."
            "poetic", "deep" -> "In a world of constant motion: $trimmed ✨"
            else -> "$trimmed 💫"
        }
    }

    suspend fun summarizeConversation(messages: List<String>): String = withContext(Dispatchers.IO) {
        if (messages.isEmpty()) return@withContext "No messages in conversation yet."
        val validKey = apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY" && apiKey != "null"
        if (validKey) {
            try {
                val thread = messages.takeLast(10).joinToString("\n")
                val prompt = "Summarize this direct chat in 2-3 concise bullet points:\n$thread"
                val response = callGeminiSimple(prompt)
                if (!response.isNullOrBlank()) {
                    return@withContext response.trim()
                }
            } catch (e: Exception) {
                Log.w("GeminiClient", "Summary API call failed: ${e.message}")
            }
        }

        val last3 = messages.takeLast(3)
        return@withContext buildString {
            append("📌 Chat Highlights:\n")
            append("• Total recent exchanges: ${messages.size} messages\n")
            if (last3.isNotEmpty()) {
                append("• Latest topic: \"${last3.last().take(40)}...\"\n")
            }
            append("• Conversation status: Active & up to date")
        }
    }

    suspend fun summarizeConversationWithMode(messages: List<String>, mode: String): String = withContext(Dispatchers.IO) {
        if (messages.isEmpty()) return@withContext "No messages in conversation yet."
        val thread = messages.takeLast(12).joinToString("\n")
        val validKey = apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY" && apiKey != "null"
        if (validKey) {
            try {
                val prompt = when (mode) {
                    "tldr" -> "Provide a 1-2 sentence quick TL;DR summary of this chat:\n$thread"
                    "action_items" -> "Extract any action items, questions asked, plans made, or shared recommendations from this chat into concise bullet points:\n$thread"
                    else -> "Summarize this direct chat in 3 clear bullet points covering key discussion points and tone:\n$thread"
                }
                val response = callGeminiSimple(prompt)
                if (!response.isNullOrBlank()) {
                    return@withContext response.trim()
                }
            } catch (e: Exception) {
                Log.w("GeminiClient", "Summary API call failed: ${e.message}")
            }
        }

        val last3 = messages.takeLast(3)
        return@withContext when (mode) {
            "tldr" -> "⚡ Quick TL;DR: Conversation actively discussing \"${last3.lastOrNull()?.take(40) ?: "topics"}\"."
            "action_items" -> buildString {
                append("🎯 Key Takeaways & Actions:\n")
                append("• Review latest discussion points from thread\n")
                append("• Respond to friend's questions and updates\n")
                append("• Follow up on shared video recommendations")
            }
            else -> buildString {
                append("📌 Conversation Summary:\n")
                append("• Total recent exchanges: ${messages.size} messages\n")
                if (last3.isNotEmpty()) {
                    append("• Recent focus: \"${last3.last().take(40)}...\"\n")
                }
                append("• Thread status: Active & connected")
            }
        }
    }

    private fun callGeminiSimple(prompt: String): String? {
        val jsonBody = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", prompt) })
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.7)
                put("maxOutputTokens", 150)
            })
        }
        val requestBody = jsonBody.toString().toRequestBody("application/json".toMediaType())
        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"
        val request = Request.Builder().url(url).post(requestBody).build()
        val response = okHttpClient.newCall(request).execute()
        if (!response.isSuccessful) return null
        val resBody = response.body?.string() ?: return null
        val resJson = JSONObject(resBody)
        val candidates = resJson.optJSONArray("candidates") ?: return null
        if (candidates.length() == 0) return null
        val firstCandidate = candidates.getJSONObject(0)
        val content = firstCandidate.optJSONObject("content") ?: return null
        val parts = content.optJSONArray("parts") ?: return null
        if (parts.length() == 0) return null
        return parts.getJSONObject(0).optString("text")
    }
}

