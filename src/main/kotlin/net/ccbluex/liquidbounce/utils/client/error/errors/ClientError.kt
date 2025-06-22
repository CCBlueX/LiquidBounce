package net.ccbluex.liquidbounce.utils.client.error.errors

open class ClientError(
    message: String = "",
    val needToReport: Boolean = true
) : RuntimeException(message)
