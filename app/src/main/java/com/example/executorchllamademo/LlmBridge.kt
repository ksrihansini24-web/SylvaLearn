package com.example.executorchllamademo

import android.content.Context
import android.webkit.JavascriptInterface
import android.webkit.WebView
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

class LlmBridge(private val ctx: Context, private val webView: WebView) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val dao get() = AppDb.get(ctx).dao()

    private fun translateOffline(text: String, sourceLang: String, targetLang: String): String {
        if (sourceLang == targetLang || text.isBlank()) return text
        return try {
            val options = TranslatorOptions.Builder()
                .setSourceLanguage(sourceLang)
                .setTargetLanguage(targetLang)
                .build()
            val translator = Translation.getClient(options)
            Tasks.await(translator.downloadModelIfNeeded(DownloadConditions.Builder().build()))
            val out = Tasks.await(translator.translate(text))
            translator.close()
            out
        } catch (_: Exception) {
            text
        }
    }

    private fun parseProfileFromJson(json: String): Profile {
        val o = try { JSONObject(json) } catch (_: Exception) { JSONObject() }

        val subjectsVal = when {
            o.has("weakSubjects") -> o.optString("weakSubjects")
            o.opt("subjects") is JSONArray -> {
                val arr = o.getJSONArray("subjects")
                (0 until arr.length()).joinToString(", ") { arr.getString(it) }
            }
            else -> o.optString("subjects", "Physics, Chemistry, Mathematics, Biology")
        }

        val levelVal = o.optString("level").ifBlank {
            o.optString("ageGroup").ifBlank {
                o.optString("academicLevel", "Grade 11 / Year 12")
            }
        }

        val examVal = o.optString("examName").ifBlank {
            o.optString("learningGoals", "Final Exams")
        }

        val targetDays = o.optInt("targetDays", o.optInt("daysUntilExam", 30)).coerceAtLeast(5)
        val dateVal = o.optString("examDate").ifBlank {
            LocalDate.now().plusDays(targetDays.toLong()).toString()
        }

        // Reads web UI's `availableStudyHours` first, falling back to `hoursPerDay` or 3.0
        val hoursVal = try {
            when {
                o.has("availableStudyHours") -> o.getDouble("availableStudyHours").toFloat()
                o.has("hoursPerDay") -> o.getDouble("hoursPerDay").toFloat()
                else -> 3.0f
            }.coerceIn(0.5f, 16.0f)
        } catch (_: Exception) {
            3.0f
        }

        return Profile(
            name = o.optString("name", "Student").ifBlank { "Student" },
            examName = examVal,
            examDate = dateVal,
            hoursPerDay = hoursVal,
            level = levelVal,
            weakSubjects = subjectsVal,
            language = o.optString("language", "English").ifBlank { "English" }
        )
    }
    private fun extractCleanQuestion(raw: String): String {
        val trimmed = raw.trim()

        val passageMarker = "Passage:"
        val questionMarkers = listOf("Student question:", "User question:", "Question:", "Query:")
        for (m in questionMarkers) {
            val idx = trimmed.lastIndexOf(m, ignoreCase = true)
            if (idx != -1) {
                val qOnly = trimmed.substring(idx + m.length).trim()
                val pIdx = trimmed.indexOf(passageMarker, ignoreCase = true)
                return if (pIdx != -1 && pIdx < idx) {
                    val contextSnippet = trimmed.substring(pIdx, idx).trim().take(400)
                    "$contextSnippet\nQuestion: $qOnly"
                } else {
                    qOnly
                }
            }
        }

        val lines = trimmed.lines().map { it.trim() }.filter { it.isNotEmpty() }
        val indicLines = lines.filter { line -> line.any { it.code in 0x0900..0x0D7F } }
        if (indicLines.isNotEmpty()) {
            val targetLine = indicLines.last()
            val colonIdx = targetLine.indexOf(':')
            val cleaned = if (colonIdx in 1..25 && targetLine.substring(0, colonIdx).all { it.code < 128 }) {
                targetLine.substring(colonIdx + 1).trim()
            } else {
                targetLine
            }
            if (cleaned.isNotEmpty()) return cleaned
        }

        return lines.lastOrNull() ?: trimmed
    }

    @JavascriptInterface
    fun saveProfile(json: String) {
        scope.launch {
            val profile = parseProfileFromJson(json)
            dao.saveProfile(profile)
            toJs("window.onProfileSaved && window.onProfileSaved()")
        }
    }

    @JavascriptInterface
    fun loadProfile() {
        scope.launch {
            val p = dao.getProfile()
            val json = if (p == null) "null" else JSONObject().apply {
                put("name", p.name)
                put("examName", p.examName)
                put("examDate", p.examDate)
                put("hoursPerDay", p.hoursPerDay.toDouble())
                put("level", p.level)
                put("ageGroup", p.level)
                put("weakSubjects", p.weakSubjects)
                put("subjects", p.weakSubjects)
                put("language", p.language)
            }.toString()
            toJs("window.onProfileLoaded && window.onProfileLoaded($json)")
        }
    }

    @JavascriptInterface
    fun generateStudyPlan(): String {
        return generateStudyPlanWithInput("{}")
    }

    @JavascriptInterface
    fun generateStudyPlanWithInput(json: String): String {
        val fallbackProfile = Profile(
            name = "Student",
            examName = "Final Exams",
            examDate = LocalDate.now().plusDays(30).toString(),
            hoursPerDay = 3.0f,
            level = "Grade 11 / Year 12",
            weakSubjects = "Physics, Chemistry, Mathematics, Biology",
            language = "English"
        )

        val activeProfile = runBlocking(Dispatchers.IO) {
            val existing = dao.getProfile()
            if (json.trim() != "{}" && json.isNotBlank()) {
                val parsed = parseProfileFromJson(json)
                dao.saveProfile(parsed)
                parsed
            } else {
                existing ?: fallbackProfile.also { dao.saveProfile(it) }
            }
        }

        val progress = runBlocking(Dispatchers.IO) { dao.getAllProgress() }
        val skeleton = StudyPlanner.skeleton(activeProfile, progress)
        toJs("window.onPlanSkeleton && window.onPlanSkeleton(${JSONObject.quote(skeleton)})")
        return skeleton
    }
    @JavascriptInterface
    fun loadChatHistory() {
        scope.launch {
            val msgs = dao.getAllMessages()
            val arr = JSONArray()
            for (m in msgs) {
                arr.put(JSONObject().apply {
                    put("role", m.role)
                    put("content", m.content)
                })
            }
            toJs("window.onChatHistoryLoaded && window.onChatHistoryLoaded($arr)")
        }
    }

    @JavascriptInterface
    fun clearChatHistory() {
        scope.launch { dao.clearChatHistory() }
    }

    @JavascriptInterface
    fun ask(question: String, historyJson: String) {
        if (LlmEngine.busy) {
            toJs("window.onError && window.onError('Model is busy')")
            return
        }
        scope.launch {
            val p = dao.getProfile()
            val cleanQuestion = extractCleanQuestion(question)
            val langCode = PromptBuilder.detectLanguageCode(cleanQuestion, p?.language)
            val englishQuestion = if (langCode != "en") {
                translateOffline(cleanQuestion, langCode, TranslateLanguage.ENGLISH)
            } else {
                cleanQuestion
            }

            val dbHistory = dao.getRecentMessages()
            val effectiveHistory = if (dbHistory.isNotEmpty()) {
                JSONArray().apply {
                    for (m in dbHistory) {
                        put(JSONObject().apply {
                            put("role", m.role)
                            put("content", m.content)
                        })
                    }
                }.toString()
            } else {
                "[]"
            }
            dao.insertMessage(ChatMessage(role = "user", content = englishQuestion))
            runModel(
                PromptBuilder.chatPrompt(p, effectiveHistory, englishQuestion),
                seqLen = 380,
                targetLangCode = langCode,
                saveReply = true
            )
        }
    }
    private fun parseMcqsToJson(rawText: String): JSONArray {
        val result = JSONArray()
        // Ensure inline options also break onto their own lines
        val withNewlines = rawText
            .replace(" A)", "\nA)")
            .replace(" B)", "\nB)")
            .replace(" C)", "\nC)")
            .replace(" D)", "\nD)")
            .replace(" ANS:", "\nANS:")
            .replace(" EXP:", "\nEXP:")

        var curQ = ""
        var optA = ""
        var optB = ""
        var optC = ""
        var optD = ""
        var ans = "A"
        var exp = ""

        fun flushCard() {
            if (curQ.isNotEmpty() && optA.isNotEmpty() && optB.isNotEmpty()) {
                result.put(JSONObject().apply {
                    put("q", curQ)
                    put("A", optA)
                    put("B", optB)
                    put("C", optC)
                    put("D", optD)
                    put("ans", ans)
                    put("exp", exp)
                })
            }
            curQ = ""; optA = ""; optB = ""; optC = ""; optD = ""; ans = "A"; exp = ""
        }

        for (rawLine in withNewlines.lines()) {
            val line = rawLine.trim()
            if (line.isEmpty()) continue

            when {
                line.startsWith("Q:", ignoreCase = true) -> {
                    flushCard()
                    curQ = line.substring(2).trim()
                }
                line.startsWith("A)", ignoreCase = true) || line.startsWith("A:", ignoreCase = true) -> {
                    optA = line.substring(2).trim()
                }
                line.startsWith("B)", ignoreCase = true) || line.startsWith("B:", ignoreCase = true) -> {
                    optB = line.substring(2).trim()
                }
                line.startsWith("C)", ignoreCase = true) || line.startsWith("C:", ignoreCase = true) -> {
                    optC = line.substring(2).trim()
                }
                line.startsWith("D)", ignoreCase = true) || line.startsWith("D:", ignoreCase = true) -> {
                    optD = line.substring(2).trim()
                }
                line.startsWith("ANS:", ignoreCase = true) -> {
                    val letter = line.substring(4).trim().uppercase()
                        .firstOrNull { it in listOf('A', 'B', 'C', 'D') }
                    if (letter != null) ans = letter.toString()
                }
                line.startsWith("EXP:", ignoreCase = true) -> {
                    exp = line.substring(4).trim()
                }
            }
        }
        flushCard()
        return result
    }
    @JavascriptInterface
    fun generateTopicQuiz(topic: String, batchNum: Int = 1) {
        if (LlmEngine.busy) {
            toJs("window.onQuizError && window.onQuizError('AI engine is busy. Please wait a moment.')")
            return
        }
        val cleanTopic = topic.trim().ifBlank { "General Science" }
        scope.launch {
            val bos = "<" + "|begin_of_text|" + ">"
            val sh = "<" + "|start_header_id|" + ">"
            val eh = "<" + "|end_header_id|" + ">"
            val eot = "<" + "|eot_id|" + ">"

            val focusNote = if (batchNum == 1) "foundational concepts and definitions" else "applications, reactions, and problem-solving"
            val sys = "You are a Grade 11 exam writer. Write 5 distinct, accurate multiple-choice questions on the topic focusing on $focusNote. " +
                    "Use this exact format:\n" +
                    "Q: <question>\n" +
                    "A: <option A>\n" +
                    "B: <option B>\n" +
                    "C: <option C>\n" +
                    "D: <option D>\n" +
                    "ANS: <A, B, C, or D>\n" +
                    "EXP: <1 short factual sentence explaining why the correct option is scientifically true, without saying 'this question' or 'the student'>"


            val prompt = "$bos${sh}system$eh\n\n$sys$eot${sh}user$eh\n\nTopic: $cleanTopic (Set $batchNum: 5 MCQs)$eot${sh}assistant$eh\n\n"
            val raw = StringBuilder()

            try {
                LlmEngine.generate(
                    ctx,
                    prompt,
                    seqLen = 850,
                    onToken = { tok -> raw.append(tok) },
                    onStats = {}
                )
                val reply = raw.toString().let { cur ->
                    if (cur.startsWith(prompt)) cur.substring(prompt.length) else cur
                }.replace(Regex("(<\\x7C[^\\x7C]+\\x7C>|\\bassistant\\b\\s*)"), "").trim()

                val parsedArray = parseMcqsToJson(reply)
                toJs("window.onQuizGenerated && window.onQuizGenerated(${JSONObject.quote(cleanTopic)}, $batchNum, $parsedArray, ${JSONObject.quote(reply)})")
            } catch (e: Exception) {
                toJs("window.onQuizError && window.onQuizError(${JSONObject.quote(e.message ?: "Quiz generation failed")})")
            }
        }
    }
    @JavascriptInterface
    fun stop() {
        LlmEngine.stop()
    }

    @JavascriptInterface
    fun cancel() {
        LlmEngine.stop()
    }

    @JavascriptInterface
    fun recordAnswer(subject: String, correct: Boolean) {
        scope.launch {
            val cleanSubject = subject.ifBlank { "General Science" }
            val old = dao.getProgress(cleanSubject)
            val att = (old?.attempted ?: 0) + 1
            val cor = (old?.correct ?: 0) + if (correct) 1 else 0
            dao.saveProgress(Progress(cleanSubject, att, cor))
        }
    }

    private fun deduplicateSentences(text: String): String {
        val seen = LinkedHashSet<String>()
        val parts = text.split(Regex("(?<=[.!?।])\\s+"))
        for (part in parts) {
            val clean = part.trim()
            if (clean.isNotEmpty()) {
                seen.add(clean)
            }
        }
        return seen.joinToString(" ")
    }

    private fun runModel(prompt: String, seqLen: Int, targetLangCode: String, saveReply: Boolean = false) {
        val t0 = System.currentTimeMillis()
        var failed = false
        val rawBuffer = StringBuilder()
        var emittedLength = 0
        val specialTagRegex = Regex("(<\\x7C[^\\x7C]+\\x7C>|\\bassistant\\b\\s*)")
        toJs("window.onGenStart && window.onGenStart()")
        try {
            LlmEngine.generate(
                ctx,
                prompt,
                seqLen,
                onToken = { token ->
                    rawBuffer.append(token)
                    val current = rawBuffer.toString()
                    val replyOnly = when {
                        current.startsWith(prompt) -> current.substring(prompt.length)
                        prompt.startsWith(current) -> ""
                        else -> current
                    }.replace(specialTagRegex, "")
                    if (targetLangCode == "en" && replyOnly.length > emittedLength) {
                        val delta = replyOnly.substring(emittedLength)
                        emittedLength = replyOnly.length
                        toJs("window.onToken && window.onToken(" + JSONObject.quote(delta) + ")")
                    }
                },
                onStats = { stats ->
                    toJs("window.onStats && window.onStats(" + JSONObject.quote(stats) + ")")
                }
            )
        } catch (e: Exception) {
            failed = true
            toJs(
                "window.onError && window.onError(" +
                        JSONObject.quote(e.message ?: "Model generation failed") +
                        ")"
            )
        }
        if (!failed) {
            val englishReply = deduplicateSentences(
                rawBuffer.toString().let { current ->
                    if (current.startsWith(prompt)) current.substring(prompt.length) else current
                }.replace(specialTagRegex, "").trim()
            )
            if (targetLangCode != "en") {
                val translated = translateOffline(englishReply, TranslateLanguage.ENGLISH, targetLangCode)
                toJs("window.onToken && window.onToken(" + JSONObject.quote(translated) + ")")
            }
            if (saveReply && englishReply.isNotEmpty()) {
                scope.launch {
                    dao.insertMessage(ChatMessage(role = "assistant", content = englishReply))
                }
            }
            toJs("window.onDone && window.onDone(" + (System.currentTimeMillis() - t0) + ")")
        }
    }
    private fun toJs(script: String) {
        webView.post {
            webView.evaluateJavascript(script, null)
        }
    }
}
