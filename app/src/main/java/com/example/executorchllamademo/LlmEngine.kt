package com.example.executorchllamademo

import android.content.Context
import org.pytorch.executorch.extension.llm.LlmCallback
import org.pytorch.executorch.extension.llm.LlmModule
import java.io.File

object LlmEngine {
    private var module: LlmModule? = null

    @Volatile
    var busy = false
        private set

    @Synchronized
    fun ensureLoaded(context: Context) {
        if (module != null) return

        val appCtx = context.applicationContext ?: context
        val prefs = DemoSharedPreferences(appCtx)
        val settings = prefs.getModuleSettings()

        // 1. Try paths saved from "Load Local Model" first, then check app filesDir / Download
        val modelPath = listOf(
            settings?.modelFilePath,
            File(appCtx.filesDir, "llama3_2_1b_ctx2048.pte").absolutePath,
            "/sdcard/Download/llama3_2_1b_ctx2048.pte"
        ).firstOrNull { !it.isNullOrBlank() && File(it).exists() }
            ?: throw IllegalStateException("Model file not found. Go back and select it in 'Load Local Model'.")

        val tokenizerPath = listOf(
            settings?.tokenizerFilePath,
            File(appCtx.filesDir, "tokenizer.model").absolutePath,
            "/sdcard/Download/tokenizer.model"
        ).firstOrNull { !it.isNullOrBlank() && File(it).exists() }
            ?: throw IllegalStateException("Tokenizer file not found. Go back and select it in 'Load Local Model'.")

        val temp = try {
            settings?.temperature?.toFloat() ?: 0.7f
        } catch (_: Exception) {
            0.7f
        }

        val category = LlmModule.MODEL_TYPE_TEXT
        val llm = LlmModule(category, modelPath, tokenizerPath, temp)
        val status = llm.load()
        if (status != 0) {
            throw IllegalStateException("Model failed to load (code $status) from $modelPath")
        }
        module = llm
    }

    fun generate(
        context: Context,
        prompt: String,
        seqLen: Int,
        onToken: (String) -> Unit,
        onStats: (String) -> Unit
    ) {
        synchronized(this) {
            if (busy) throw IllegalStateException("Model is busy")
            busy = true
        }

        try {
            ensureLoaded(context)
            module!!.generate(
                prompt,
                seqLen,
                object : LlmCallback {
                    override fun onResult(result: String) {
                        onToken(result)
                    }

                    override fun onStats(stats: String) {
                        onStats(stats)
                    }
                }
            )
        } finally {
            busy = false
        }
    }

    fun stop() {
        module?.stop()
    }
}
