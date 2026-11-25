package com.example.executorchllamademo

import org.json.JSONArray

object PromptBuilder {
    fun detectLanguageCode(question: String, profileLang: String?): String {
        for (ch in question) {
            when (ch.code) {
                in 0x0C00..0x0C7F -> return "te" // Telugu
                in 0x0B80..0x0BFF -> return "ta" // Tamil
                in 0x0C80..0x0CFF -> return "kn" // Kannada
                in 0x0D00..0x0D7F -> return "ml" // Malayalam
                in 0x0980..0x09FF -> return "bn" // Bengali / Assamese
                in 0x0A80..0x0AFF -> return "gu" // Gujarati
                in 0x0900..0x097F -> return if (profileLang == "Marathi") "mr" else "hi" // Hindi / Marathi
                in 0x0600..0x06FF -> return "ur" // Urdu
            }
        }
        return "en"
    }

    private fun system(p: Profile?): String {
        val profile = if (p == null) "Learner level: Grade 11 / High School and College. " else
            "Learner: ${p.name}. Goal/Exam: ${p.examName} (${p.examDate}). " +
                    "Level: ${p.level}. Focus subjects: ${p.weakSubjects}. "
        return "You are SylvaLearn, a clear and accurate science and math tutor. $profile" +
                "Answer in simple, clear English in 3 to 4 short sentences. " +
                "State the definition, the exact formula, and one short numerical example. Never repeat sentences."
    }

    private fun block(role: String, text: String): String {
        val sh = "<" + "|start_header_id|" + ">"
        val eh = "<" + "|end_header_id|" + ">"
        val eot = "<" + "|eot_id|" + ">"
        return "$sh$role$eh\n\n$text$eot"
    }

    fun chatPrompt(p: Profile?, historyJson: String, englishQuestion: String): String {
        val bos = "<" + "|begin_of_text|" + ">"
        val sh = "<" + "|start_header_id|" + ">"
        val eh = "<" + "|end_header_id|" + ">"

        val sb = StringBuilder(bos)
        sb.append(block("system", system(p)))
        val arr = try { JSONArray(historyJson) } catch (_: Exception) { JSONArray() }
        val start = maxOf(0, arr.length() - 2)
        for (i in start until arr.length()) {
            val o = arr.getJSONObject(i)
            sb.append(block(o.getString("role"), o.getString("content").take(350)))
        }
        sb.append(block("user", englishQuestion))
        sb.append("${sh}assistant$eh\n\n")
        return if (sb.length > 2400) trimOldest(p, englishQuestion) else sb.toString()
    }

    private fun trimOldest(p: Profile?, q: String): String {
        val bos = "<" + "|begin_of_text|" + ">"
        val sh = "<" + "|start_header_id|" + ">"
        val eh = "<" + "|end_header_id|" + ">"
        return bos + block("system", system(p)) + block("user", q.take(800)) + "${sh}assistant$eh\n\n"
    }

    fun studyPlanPrompt(p: Profile, skeleton: String): String {
        val bos = "<" + "|begin_of_text|" + ">"
        val sh = "<" + "|start_header_id|" + ">"
        val eh = "<" + "|end_header_id|" + ">"
        val user = "Here is my schedule:\n$skeleton\n\n" +
                "For each week, write 2 short lines: what to focus on and one study tip. " +
                "Then finish with a 2 line motivation note. Do not change the dates or subjects."
        return bos + block("system", system(p)) + block("user", user) + "${sh}assistant$eh\n\n"
    }
}
