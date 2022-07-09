/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.api.util

open class WrappedCollection<WRAPPED, UNWRAPPED, out C : Collection<WRAPPED>>(val wrapped: C, val wrapper: (UNWRAPPED) -> WRAPPED, val unwrapper: (WRAPPED) -> UNWRAPPED) : Collection<UNWRAPPED>
{
    override val size: Int
        get() = wrapped.size

    override fun contains(element: UNWRAPPED): Boolean = wrapped.contains(wrapper(element))

    override fun containsAll(elements: Collection<UNWRAPPED>): Boolean
    {
        elements.forEach {
            if (wrapped.contains(wrapper(it))) return@containsAll true
        }

        return false
    }

    override fun isEmpty(): Boolean = wrapped.isEmpty()

    override fun iterator(): Iterator<UNWRAPPED> = WrappedCollectionIterator(wrapped.iterator(), unwrapper)

    class WrappedCollectionIterator<WRAPPED, out UNWRAPPED>(val wrapped: Iterator<WRAPPED>, val unwrapper: (WRAPPED) -> UNWRAPPED) : Iterator<UNWRAPPED>
    {
        override fun hasNext(): Boolean = wrapped.hasNext()

        override fun next(): UNWRAPPED = unwrapper(wrapped.next())
    }
}
