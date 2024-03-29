package net.ccbluex.liquidbounce.web.socket.netty

import java.nio.file.Path
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.io.path.readBytes

fun readImageAsBase64(path: Path): String {
    return path.readBytes().encodeBase64()
}

@OptIn(ExperimentalEncodingApi::class)
private fun ByteArray.encodeBase64() = Base64.encode(this)
