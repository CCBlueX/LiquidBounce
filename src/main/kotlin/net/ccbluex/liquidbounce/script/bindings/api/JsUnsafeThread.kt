package net.ccbluex.liquidbounce.script.bindings.api

import kotlin.concurrent.thread

object JsUnsafeThread {

    @JvmName("run")
    fun run(callback: () -> Unit) = thread {
        callback()
    }
}
