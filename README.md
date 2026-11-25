# SylvaLearn — Offline AI Personalised Learning Assistant (Android + ExecuTorch)

SylvaLearn is an offline-first, on-device AI learning assistant for Android powered by PyTorch ExecuTorch and Meta Llama 3.2 1B.

## Core Features
- **Offline AI Tutor (LlmBridge.kt & LlmEngine.kt):** Runs Meta Llama 3.2 1B (ctx2048) locally on-device with persistent Room SQLite multi-turn conversation memory (Db.kt).
- **Offline Custom AI Quiz Generator (WebActivity.kt & LlmBridge.kt):** Generates 10 interactive MCQs on any custom topic entered by the student, provides instant factual explanations, and syncs quiz accuracy to SQLite.
- **Adaptive Interleaved Study Planner (StudyPlanner.kt):** Builds a 2-subject daily interleaved study schedule weighted toward weak subjects and low-scoring quiz topics, complete with weekly focus tips.
- **22 Official Indian Languages Support:** Multi-language UI localization and on-device ML Kit translation.

## Setup & Local Execution
1. Open this project in **Android Studio** and sync Gradle.
2. Place llama3_2_1b_ctx2048.pte and 	okenizer.model in /sdcard/Download/ on your Android device (or select them via **Load Local Model** on the launch screen).
3. Tap **Open SylvaLearn** to run 100% offline.
