import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import kotlin.system.exitProcess

fun getTrueLength(addr: String): Int {
    val connection = URL(addr).openConnection() as HttpURLConnection
    connection.requestMethod = "GET"
    connection.connect()
    return connection.headerFields["Content-Length"]?.first()?.toInt() ?: 0
}

fun getBytes(addr: String, start: Int, end: Int): ByteArray? {
    val connection = URL(addr).openConnection() as HttpURLConnection
    connection.requestMethod = "GET"
    connection.setRequestProperty("Range", "bytes=$start-$end")
    connection.connect()
    val bytes = connection.inputStream.readBytes()
    connection.disconnect()
    return when(connection.responseCode) {
        HttpURLConnection.HTTP_OK, HttpURLConnection.HTTP_PARTIAL -> bytes
        else -> null
    }
}

fun main() {
    val addr = "http://127.0.0.1:8080"

    println("Starting...")

    val trueLength = getTrueLength(addr)
    if (trueLength == 0) { println("Couldn't get trueLength"); exitProcess(1) }

    val data = mutableListOf<Byte>()

    var start = 0
    var step = 50000
    var end = step

    while (true) {
        if (end > trueLength) { end = trueLength }
        val responseBytes = getBytes(addr, start, end)
        if (responseBytes != null) {
            for (b in responseBytes) {
                data.add(b)
            }
            println("Fetched: $end, $trueLength ")
            if (end == trueLength) {
                break
            }
            start += step
            end += step
        } else {
            step = (step * 0.8).toInt(); end = step; start = 0; data.clear()
        }
    }

    // https://gist.github.com/lovubuntu/164b6b9021f5ba54cefc67f60f7a1a25
    val md = MessageDigest.getInstance("SHA-256")
    val digest = md.digest(data.toByteArray())
    val hash = digest.fold("") { str, it -> str + "%02x".format(it) }
    // --

    println("Hash: $hash")
}