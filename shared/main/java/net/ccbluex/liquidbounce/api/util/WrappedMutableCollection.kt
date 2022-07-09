/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.api.util

open class WrappedMutableCollection<WRAPPED, UNWRAPPED, out COLLECTION : MutableCollection<WRAPPED>>(wrapped: COLLECTION, wrapper: (UNWRAPPED) -> WRAPPED, unwrapper: (WRAPPED) -> UNWRAPPED) : WrappedCollection<WRAPPED, UNWRAPPED, COLLECTION>(wrapped, wrapper, unwrapper), MutableCollection<UNWRAPPED>
{
    override fun add(element: UNWRAPPED): Boolean = wrapped.add(wrapper.invoke(element))

    override fun addAll(elements: Collection<UNWRAPPED>): Boolean = wrapped.addAll(elements.map(wrapper))

    override fun clear() = wrapped.clear()

    override fun iterator(): MutableIterator<UNWRAPPED> = WrappedCollectionIterator(wrapped.iterator(), unwrapper)
    override fun remove(element: UNWRAPPED): Boolean = wrapped.remove(wrapper.invoke(element))

    override fun removeAll(elements: Collection<UNWRAPPED>): Boolean = wrapped.addAll(elements.map(wrapper))

    override fun retainAll(elements: Collection<UNWRAPPED>): Boolean = wrapped.addAll(elements.map(wrapper))

    class WrappedCollectionIterator<WRAPPED, out UNWRAPPED>(val wrapped: MutableIterator<WRAPPED>, val unwrapper: (WRAPPED) -> UNWRAPPED) : MutableIterator<UNWRAPPED>
    {
        override fun hasNext(): Boolean = wrapped.hasNext()

        override fun next(): UNWRAPPED = unwrapper(wrapped.next())
        override fun remove() = wrapped.remove()
    }
}
