/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.api.util

import com.google.common.base.Predicate

class WrappedPredicate<WRAPPED, UNWRAPPED>(val wrapped: Predicate<WRAPPED>, val wrapper: (UNWRAPPED) -> WRAPPED) : Predicate<UNWRAPPED>
{
    override fun apply(input: UNWRAPPED?): Boolean = wrapped.apply(input?.let { wrapper(it) })
}
