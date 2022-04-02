/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.api.util

open class WrappedList<O, T, C : List<O>>(wrapped: C, unwrapper: (T) -> O, wrapper: (O) -> T) : WrappedCollection<O, T, C>(wrapped, unwrapper, wrapper), List<T> {
    override fun get(index: Int): T = wrapper(wrapped[index])

    override fun indexOf(element: T): Int = wrapped.indexOf(unwrapper(element))

    override fun lastIndexOf(element: T): Int = wrapped.indexOf(unwrapper(element))

    override fun listIterator(): ListIterator<T> = WrappedCollectionIterator(wrapped.listIterator(), wrapper)

    override fun listIterator(index: Int): ListIterator<T> = WrappedCollectionIterator(wrapped.listIterator(index), wrapper)

    override fun subList(fromIndex: Int, toIndex: Int): List<T> = WrappedList(wrapped.subList(fromIndex, toIndex), unwrapper, wrapper)

    class WrappedCollectionIterator<O, T>(val wrapped: ListIterator<O>, val wrapper: (O) -> T) : ListIterator<T> {
        override fun hasNext(): Boolean = wrapped.hasNext()

        override fun hasPrevious(): Boolean = wrapped.hasPrevious()

        override fun next(): T = wrapper(wrapped.next())

        override fun nextIndex(): Int = wrapped.nextIndex()

        override fun previous(): T = wrapper(wrapped.previous())

        override fun previousIndex(): Int = wrapped.previousIndex()
    }
}