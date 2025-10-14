package net.ccbluex.liquidbounce.utils.client.error.errors

class JcefIsntCompatible : ClientError(
    message = "JCEF Isn't compatible",
    needToReport = false
) {
    fun readResolve(): Any = JcefIsntCompatible()
}
