/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.api.util

open class WrappedCollection<O, T, C : Collection<O>>(val wrapped: C, val unwrapper: (T) -> O, val wrapper: (O) -> T) : Collection<T> {
    override val size: Int
        get() = wrapped.size

    override fun contains(element: T): Boolean = wrapped.contains(unwrapper(element))

    override fun containsAll(elements: Collection<T>): Boolean {
        elements.forEach {
            if (wrapped.contains(unwrapper(it)))
                return true
        }

        return false
    }

    override fun isEmpty(): Boolean = wrapped.isEmpty()

    override fun iterator(): Iterator<T> = WrappedCollectionIterator(wrapped.iterator(), wrapper)

    class WrappedCollectionIterator<O, T>(val wrapped: Iterator<O>, val unwrapper: (O) -> T) : Iterator<T> {
        override fun hasNext(): Boolean = wrapped.hasNext()

        override fun next(): T = unwrapper(wrapped.next())
    }
}