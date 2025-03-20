package com.ftt2.nev

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import java.security.MessageDigest

suspend fun main() {
    val addr = "http://127.0.0.1:8080"

    println("Starting...")

    // when the first request (*) fails, set the previously unset trueLength
    // from the header value before the exception gets thrown
    var trueLength = 0

    val client = HttpClient(CIO) {
        expectSuccess = false
        HttpResponseValidator {
            validateResponse { req ->
                if (trueLength == 0) {
                    trueLength = req.headers["Content-Length"]?.toInt() ?: 0
                }
            }
        }
    }

    // (*) here
    try { client.get(addr) } catch (_: Exception) {}

    val data = mutableListOf<Byte>()

    // in steps, get a range of data and if the end pointer reached trueLength, stop
    var start = 0
    var step = 50000
    var end = step
    while (true) {
        try {
            if (end > trueLength && trueLength != 0) { end = trueLength }
            val response: HttpResponse = client.get(addr) {
                headers {
                    append("Range", "bytes=$start-$end")
                }
            }
            for (b in response.readRawBytes()) {
                data.add(b)
            }
            println("Fetched: $end, $trueLength ")
            if (end == trueLength) {
                break
            }
            start += step
            end += step
        } catch (_: Exception) { step = (step * 0.8).toInt(); end = step; start = 0; data.clear(); }
        // qol: if the step would be too high, adjust it until it's not instead of manually having to do so
    }

    // https://gist.github.com/lovubuntu/164b6b9021f5ba54cefc67f60f7a1a25
    val md = MessageDigest.getInstance("SHA-256")
    val digest = md.digest(data.toByteArray())
    val hash = digest.fold("") { str, it -> str + "%02x".format(it) }
    // --

    println("Hash: $hash")
    client.close()
}
