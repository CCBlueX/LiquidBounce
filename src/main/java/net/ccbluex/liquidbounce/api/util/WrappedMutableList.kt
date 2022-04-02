/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.api.util

class WrappedMutableList<O, T, C : MutableList<O>>(wrapped: C, unwrapper: (T) -> O, wrapper: (O) -> T) : WrappedMutableCollection<O, T, C>(wrapped, unwrapper, wrapper), MutableList<T> {
    override fun get(index: Int): T = wrapper(wrapped[index])

    override fun indexOf(element: T): Int = wrapped.indexOf(unwrapper(element))

    override fun lastIndexOf(element: T): Int = wrapped.lastIndexOf(unwrapper(element))

    override fun add(index: Int, element: T) = wrapped.add(index, unwrapper(element))

    override fun addAll(index: Int, elements: Collection<T>): Boolean = wrapped.addAll(index, elements.map(unwrapper))

    override fun listIterator(): MutableListIterator<T> = WrappedMutableCollectionIterator(wrapped.listIterator(), wrapper, unwrapper)

    override fun listIterator(index: Int): MutableListIterator<T> = WrappedMutableCollectionIterator(wrapped.listIterator(index), wrapper, unwrapper)

    override fun removeAt(index: Int): T = wrapper(wrapped.removeAt(index))

    override fun set(index: Int, element: T): T = wrapper(wrapped.set(index, unwrapper(element)))

    override fun subList(fromIndex: Int, toIndex: Int): MutableList<T> = WrappedMutableList(wrapped.subList(fromIndex, toIndex), unwrapper, wrapper)

    class WrappedMutableCollectionIterator<O, T>(val wrapped: MutableListIterator<O>, val wrapper: (O) -> T, val unwrapper: (T) -> O) : MutableListIterator<T> {
        override fun hasNext(): Boolean = wrapped.hasNext()

        override fun hasPrevious(): Boolean = wrapped.hasPrevious()

        override fun next(): T = wrapper(wrapped.next())

        override fun nextIndex(): Int = wrapped.nextIndex()

        override fun previous(): T = wrapper(wrapped.previous())

        override fun previousIndex(): Int = wrapped.previousIndex()
        override fun add(element: T) = wrapped.add(unwrapper(element))

        override fun remove() = wrapped.remove()

        override fun set(element: T) = wrapped.set(unwrapper(element))
    }
}