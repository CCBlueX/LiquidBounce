package net.ccbluex.liquidbounce.utils.kotlin

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class MutexWithContent<T>(val inner: T) {
    val lockObject = Any()

    inline fun lock(fn: (T) -> Unit) {
        synchronized(this.lockObject) {
            fn(this.inner)
        }
    }
}
