package com.ttt.liveassistant.translation

import com.ttt.liveassistant.TranslationLanguage
import org.json.JSONObject
import java.io.BufferedReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class RemoteTranslator {
    fun translate(
        endpoint: String,
        text: String,
        source: TranslationLanguage,
        target: TranslationLanguage,
    ): String {
        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            setRequestProperty("Accept", "application/json")
        }

        val body = JSONObject()
            .put("text", text)
            .put("sourceLanguage", source.code)
            .put("targetLanguage", target.code)

        OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { writer ->
            writer.write(body.toString())
        }

        val responseCode = connection.responseCode
        val stream = if (responseCode in 200..299) {
            connection.inputStream
        } else {
            connection.errorStream
        }

        val response = stream.bufferedReader(Charsets.UTF_8).use(BufferedReader::readText)
        if (responseCode !in 200..299) {
            throw IllegalStateException("翻譯後端錯誤 $responseCode: $response")
        }

        val json = JSONObject(response)
        return json.optString("text", json.optString("translation")).trim()
    }

    private companion object {
        const val TIMEOUT_MS = 12000
    }
}
