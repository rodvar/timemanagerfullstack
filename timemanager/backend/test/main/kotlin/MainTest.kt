package main.kotlin

import io.ktor.client.*
import kotlin.test.*
import io.ktor.client.engine.mock.*
import kotlinx.coroutines.*
import io.ktor.http.*
import io.ktor.client.request.*
import io.ktor.client.statement.HttpResponse
import io.ktor.utils.io.ByteReadChannel

class MainTest {
    @Test
    fun testClientMock() {
        runBlocking {
            val client = HttpClient(MockEngine) {
                engine {
                    addHandler { request -> 
                        when (request.url.fullPath) {
                            "/" -> respond(
                                ByteReadChannel(byteArrayOf(1, 2, 3)),
                                headers = headersOf("X-MyHeader", "MyValue")
                            )
                            else -> respond("Not Found ${request.url.encodedPath}", HttpStatusCode.NotFound)
                        }
                    }
                }
                expectSuccess = false
            }
            assertEquals(byteArrayOf(1, 2, 3).toList(), client.get<ByteArray>("/").toList())
            assertEquals("MyValue", client.request<HttpResponse>("/") {}.headers["X-MyHeader"])
            assertEquals("Not Found other/path", client.get("/other/path"))
        }
    }
}
