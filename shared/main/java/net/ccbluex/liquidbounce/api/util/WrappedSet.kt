/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.api.util

class WrappedSet<WRAPPED, UNWRAPPED>(wrapped: Set<WRAPPED>, wrapper: (UNWRAPPED) -> WRAPPED, unwrapper: (WRAPPED) -> UNWRAPPED) : WrappedCollection<WRAPPED, UNWRAPPED, Collection<WRAPPED>>(wrapped, wrapper, unwrapper), Set<UNWRAPPED>
