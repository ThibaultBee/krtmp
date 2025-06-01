package io.github.thibaultbee.krtmp.rtmp.util.connections.http

import io.ktor.client.HttpClient
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.post
import io.ktor.client.statement.HttpResponse
import io.ktor.http.URLBuilder
import io.ktor.http.appendPathSegments

suspend fun HttpClient.post(
    urlBuilder: URLBuilder,
    encodedPath: String,
    block: HttpRequestBuilder.() -> Unit = {}
): HttpResponse =
    post(urlBuilder.appendPathSegments(encodedPath).buildString(), block)