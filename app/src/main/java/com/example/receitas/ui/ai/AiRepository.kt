package com.example.receitas.ui.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class AiRepository(
    private val baseUrl: String = "https://worker.pedro-melo-junqueira.workers.dev"
) {
    suspend fun suggest(
        ingredients: String,
        restrictions: String?,
        cuisine: String?,
        servings: Int,
        allowExtras: Boolean
    ): List<AiRecipeSuggestion> = withContext(Dispatchers.IO) {

        val body = JSONObject().apply {
            put("ingredients", ingredients)
            put("servings", servings.coerceIn(1, 12))
            put("allowExtras", allowExtras) // ✅ NOVO (toggle do app)
            if (!restrictions.isNullOrBlank()) put("restrictions", restrictions)
            if (!cuisine.isNullOrBlank()) put("cuisine", cuisine)
        }

        val url = "${baseUrl.trimEnd('/')}/generate"
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            setRequestProperty("Accept", "application/json")
            doOutput = true
            connectTimeout = 15000
            readTimeout = 25000
        }

        try {
            conn.outputStream.use { os ->
                os.write(body.toString().toByteArray(Charsets.UTF_8))
            }

            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream

            val responseText = BufferedReader(InputStreamReader(stream)).use { it.readText() }

            if (code !in 200..299) {
                // tenta extrair msg do worker (quando ele manda ok:false)
                val msg = runCatching {
                    JSONObject(responseText).optString("error")
                }.getOrNull().orEmpty()

                throw IllegalStateException(
                    if (msg.isNotBlank()) msg
                    else "Erro $code: ${responseText.take(280)}"
                )
            }

            val json = JSONObject(responseText)
            val ok = json.optBoolean("ok", false)
            if (!ok) {
                throw IllegalStateException(json.optString("error", "Erro desconhecido"))
            }

            val suggestionsArr = json.getJSONArray("suggestions")
            parseRecipes(suggestionsArr)
        } finally {
            conn.disconnect()
        }
    }

    private fun parseRecipes(arr: JSONArray): List<AiRecipeSuggestion> {
        val out = mutableListOf<AiRecipeSuggestion>()

        for (i in 0 until arr.length()) {
            val r = arr.getJSONObject(i)

            fun getStringList(key: String): List<String> {
                val a = r.optJSONArray(key) ?: JSONArray()
                return buildList {
                    for (j in 0 until a.length()) {
                        val v = a.optString(j).trim()
                        if (v.isNotBlank() && v.lowercase() != "null") add(v)
                    }
                }
            }

            val notesRaw = r.optString("notes", "")
            val notes = notesRaw.takeIf { it.isNotBlank() && it.lowercase() != "null" }

            out.add(
                AiRecipeSuggestion(
                    id = r.optString("id", "ai_$i"),
                    title = r.optString("title", "Receita ${i + 1}"),
                    servings = r.optInt("servings", 1).coerceAtLeast(1),
                    timeMinutes = r.optInt("timeMinutes", 15).coerceAtLeast(1),
                    category = r.optString("category", "Geral").ifBlank { "Geral" },
                    ingredients = getStringList("ingredients"),
                    steps = getStringList("steps"),
                    extraNeeded = getStringList("extraNeeded"),
                    notes = notes
                )
            )
        }
        return out
    }
}
