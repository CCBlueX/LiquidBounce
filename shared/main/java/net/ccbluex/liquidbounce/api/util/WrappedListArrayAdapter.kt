/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.api.util

class WrappedListArrayAdapter<WRAPPED, UNWRAPPED>(val wrapped: MutableList<WRAPPED>, val wrapper: (UNWRAPPED) -> WRAPPED, val unwrapper: (WRAPPED) -> UNWRAPPED) : IWrappedArray<UNWRAPPED>
{
    override fun get(index: Int): UNWRAPPED = unwrapper(wrapped[index])

    override fun set(index: Int, value: UNWRAPPED)
    {
        wrapped[index] = wrapper(value)
    }

    override fun iterator(): Iterator<UNWRAPPED> = WrappedCollection.WrappedCollectionIterator(wrapped.iterator(), unwrapper)
}
