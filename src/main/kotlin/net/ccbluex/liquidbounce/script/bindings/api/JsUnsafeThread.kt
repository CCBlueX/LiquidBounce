package net.ccbluex.liquidbounce.script.bindings.api

import net.ccbluex.liquidbounce.utils.kotlin.virtualThread

object JsUnsafeThread {

    @JvmName("run")
    fun run(callback: () -> Unit) = virtualThread {
        callback()
    }
}
