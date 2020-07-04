/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.api.util

open class WrappedMutableCollection<O, T, C : MutableCollection<O>>(wrapped: C, unwrapper: (T) -> O, wrapper: (O) -> T) : WrappedCollection<O, T, C>(wrapped, unwrapper, wrapper), MutableCollection<T> {
    override fun add(element: T): Boolean = wrapped.add(unwrapper.invoke(element))

    override fun addAll(elements: Collection<T>): Boolean = wrapped.addAll(elements.map(unwrapper))

    override fun clear() = wrapped.clear()

    override fun iterator(): MutableIterator<T> = WrappedCollectionIterator(wrapped.iterator(), wrapper)
    override fun remove(element: T): Boolean = wrapped.remove(unwrapper.invoke(element))

    override fun removeAll(elements: Collection<T>): Boolean = wrapped.addAll(elements.map(unwrapper))

    override fun retainAll(elements: Collection<T>): Boolean = wrapped.addAll(elements.map(unwrapper))

    class WrappedCollectionIterator<O, T>(val wrapped: MutableIterator<O>, val unwrapper: (O) -> T) : MutableIterator<T> {
        override fun hasNext(): Boolean = wrapped.hasNext()

        override fun next(): T = unwrapper(wrapped.next())
        override fun remove() = wrapped.remove()
    }
}