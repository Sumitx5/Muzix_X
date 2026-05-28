package com.sumit.muzixx.data.network

import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

class MuzixDownloader private constructor(private val client: OkHttpClient) : Downloader() {

    companion object {
        @Volatile
        private var instance: MuzixDownloader? = null

        fun init(client: OkHttpClient): MuzixDownloader {
            return instance ?: synchronized(this) {
                instance ?: MuzixDownloader(client).also { instance = it }
            }
        }

        // 🚀 SAFELY EXPOSED FOR INTERNAL NEWPIPE COMPONENT ENGINE ACCESS
        fun getInstance(): MuzixDownloader {
            return instance ?: throw IllegalStateException("MuzixDownloader must be initialized first via init(client)")
        }
    }

    @Throws(IOException::class, InterruptedException::class)
    override fun execute(request: Request): Response {
        val method = request.httpMethod() ?: "GET"
        val url = request.url() ?: ""
        val headers = request.headers() ?: emptyMap()
        val body = request.dataToSend()

        val okHttpRequestBuilder = okhttp3.Request.Builder()
            .url(url)

        headers.forEach { (key, values) ->
            okHttpRequestBuilder.addHeader(key, values.joinToString(","))
        }

        // Clean Anti-Spam Spoof Routing
        okHttpRequestBuilder.header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")

        if (method.equals("POST", ignoreCase = true)) {
            val content = body ?: ByteArray(0)
            val requestBody = content.toRequestBody(null, 0, content.size)
            okHttpRequestBuilder.post(requestBody)
        } else {
            okHttpRequestBuilder.method(method, null)
        }

        val okHttpResponse = client.newCall(okHttpRequestBuilder.build()).execute()

        val responseCode = okHttpResponse.code
        val responseMessage = okHttpResponse.message
        val responseHeaders = okHttpResponse.headers.toMultimap()

        // 🎯 FIX: Read stream lines cleanly without loading full byte segments into memory strings at once
        val isHtmlOrJson = okHttpResponse.header("Content-Type")?.contains("stream") == false
        val responseBody = if (isHtmlOrJson) {
            okHttpResponse.body?.string() ?: ""
        } else {
            "" // Keep payload description body empty for large video/audio raw binary requests to prevent OOM crashes
        }

        return Response(responseCode, responseMessage, responseHeaders, responseBody, url)
    }
}