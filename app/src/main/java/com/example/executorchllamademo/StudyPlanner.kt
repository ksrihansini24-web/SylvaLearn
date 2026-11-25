package com.example.executorchllamademo

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.Locale

object StudyPlanner {
    private val defaultSubjects = listOf(
        "Physics", "Chemistry", "Mathematics", "Biology", "English & Computer Science"
    )

    private fun mapTopicToSubject(topic: String): String {
        val t = topic.lowercase()
        return when {
            listOf("ohm", "force", "motion", "light", "optics", "thermo", "electric", "magnet", "wave", "physics").any { t.contains(it) } -> "Physics"
            listOf("organic", "atom", "mole", "acid", "reaction", "bond", "periodic", "chem").any { t.contains(it) } -> "Chemistry"
            listOf("calc", "algebra", "trig", "matrix", "vector", "probab", "integral", "deriv", "math").any { t.contains(it) } -> "Mathematics"
            listOf("cell", "photo", "dna", "gene", "plant", "human", "ecology", "bio", "respir").any { t.contains(it) } -> "Biology"
            listOf("history", "world", "war", "civics", "geo", "econ").any { t.contains(it) } -> "World History & Humanities"
            else -> topic.trim()
        }
    }

    fun skeleton(p: Profile, progress: List<Progress>): String {
        val today = LocalDate.now()
        val exam = try {
            LocalDate.parse(p.examDate)
        } catch (_: Exception) {
            today.plusDays(30)
        }
        val totalDays = ChronoUnit.DAYS.between(today, exam).toInt().coerceAtLeast(5)
        val revisionDays = (totalDays * 0.2).toInt().coerceAtLeast(1)
        val studyDays = (totalDays - revisionDays).coerceAtLeast(4)

        val weakFromProfile = p.weakSubjects.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        // Map low-scoring quiz topics (accuracy < 60%) to subjects and add to priority list
        val weakFromQuizzes = progress
            .filter { it.attempted >= 1 && (it.correct.toDouble() / it.attempted) < 0.6 }
            .map { mapTopicToSubject(it.subject) }

        val combinedPriority = (weakFromProfile + weakFromQuizzes).distinctBy { it.lowercase() }
        val subjects = (defaultSubjects + combinedPriority).distinctBy { it.lowercase() }
        val priorityLower = combinedPriority.map { it.lowercase() }

        val weights = subjects.associateWith { s ->
            var w = 1.0
            if (priorityLower.any { s.lowercase().contains(it) || it.contains(s.lowercase()) }) {
                w += 1.2
            }
            w
        }

        val totalSlots = studyDays * 2
        val sumWeights = weights.values.sum()
        val slotCounts = subjects.associateWith { s ->
            maxOf(1, Math.round(weights.getValue(s) / sumWeights * totalSlots).toInt())
        }.toMutableMap()

        val slotQueue = mutableListOf<String>()
        while (slotQueue.size < totalSlots) {
            var addedInPass = false
            for (s in subjects) {
                val rem = slotCounts[s] ?: 0
                if (rem > 0 && slotQueue.size < totalSlots) {
                    slotQueue.add(s)
                    slotCounts[s] = rem - 1
                    addedInPass = true
                }
            }
            if (!addedInPass) {
                val sortedByWeight = subjects.sortedByDescending { weights.getValue(it) }
                for (s in sortedByWeight) {
                    if (slotQueue.size < totalSlots) slotQueue.add(s)
                }
            }
        }

        val halfHours = String.format(Locale.US, "%.1f", p.hoursPerDay / 2.0)

        val sb = StringBuilder()
        sb.appendLine("Student: ${p.name} | Goal: ${p.examName} (${p.examDate})")
        sb.appendLine("Schedule: $totalDays days total, ${p.hoursPerDay} hrs/day (2 subjects/day, $halfHours hrs each)")
        sb.appendLine("Priority Focus: ${combinedPriority.joinToString(", ").ifBlank { "Balanced across all subjects" }}")
        sb.appendLine()

        val previewDays = minOf(studyDays, 7)
        for (dayIdx in 0 until previewDays) {
            val date = today.plusDays(dayIdx.toLong())
            val firstSub = slotQueue[dayIdx * 2]
            val secondSub = slotQueue[dayIdx * 2 + 1].let {
                if (it == firstSub) subjects.first { cand -> cand != firstSub } else it
            }
            sb.appendLine("• Day ${dayIdx + 1} ($date): $firstSub ($halfHours hrs) + $secondSub ($halfHours hrs)")
        }

        if (studyDays > 7) {
            sb.appendLine("• Days 8–$studyDays: Repeat interleaved 2-subject rotation (extra blocks for ${combinedPriority.joinToString(", ").ifBlank { "core subjects" }})")
        }
        sb.appendLine("• Last $revisionDays days: Full mock tests & mixed-subject revision")
        sb.appendLine()
        sb.appendLine("Weekly Focus & Study Tips:")
        sb.appendLine("• Week 1–2: Build core definitions & formulas; test each topic with a 10-question AI quiz.")
        sb.appendLine("• Week 3–4: Solve timed numericals on weak topics and review mistakes before mock tests.")

        return sb.toString()
    }
}
