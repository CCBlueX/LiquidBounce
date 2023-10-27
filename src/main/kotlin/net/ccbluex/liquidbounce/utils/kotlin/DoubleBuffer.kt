package net.ccbluex.liquidbounce.utils.kotlin

class DoubleBuffer<T>(front: T, back: T) {
    private val buffers = arrayListOf(front, back)
    private var swap = false

    private val frontBufferIndex: Int
        get() = if (this.swap) 1 else 0

    private var front: T
        get() = this.buffers[frontBufferIndex]
        set(value) {
            this.buffers[frontBufferIndex] = value
        }
    private var back: T
        get() = this.buffers[1 - frontBufferIndex]
        set(value) {
            this.buffers[1 - frontBufferIndex] = value
        }

    fun swap() {
        this.swap = !this.swap
    }
}
