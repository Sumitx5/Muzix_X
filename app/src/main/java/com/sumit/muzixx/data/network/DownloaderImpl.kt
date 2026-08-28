package com.sumit.muzixx.data.network

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request as NewPipeRequest
import org.schabi.newpipe.extractor.downloader.Response as NewPipeResponse

class DownloaderImpl private constructor(private val client: OkHttpClient) : Downloader() {

    override fun execute(request: NewPipeRequest): NewPipeResponse {
        val httpMethod = request.httpMethod()
        val url = request.url()
        val headers = request.headers()
        val dataToSend = request.dataToSend()

        val requestBuilder = Request.Builder()
            .url(url)

        headers.forEach { (key, values) ->
            values.forEach { value ->
                requestBuilder.addHeader(key, value)
            }
        }

        val requestBody = dataToSend?.toRequestBody(null, 0, dataToSend.size)

        when (httpMethod) {
            "GET" -> requestBuilder.get()
            "POST" -> requestBuilder.post(requestBody ?: run {
                val content = ByteArray(0)
                content.toRequestBody(null, 0, content.size)
            })
            "HEAD" -> requestBuilder.head()
            else -> requestBuilder.method(httpMethod, requestBody)
        }

        val response = client.newCall(requestBuilder.build()).execute()
        val responseBody = response.body?.string() ?: ""

        val responseHeaders = mutableMapOf<String, List<String>>()
        response.headers.names().forEach { name ->
            responseHeaders[name] = response.headers(name)
        }

        return NewPipeResponse(
            response.code,
            response.message,
            responseHeaders,
            responseBody,
            request.url()
        )
    }

    companion object {
        @Volatile
        private var instance: DownloaderImpl? = null

        fun getInstance(client: OkHttpClient): DownloaderImpl {
            return instance ?: synchronized(this) {
                instance ?: DownloaderImpl(client).also { instance = it }
            }
        }
    }
}