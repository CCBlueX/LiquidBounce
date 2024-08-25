/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils.extensions

import net.minecraft.util.hit.BlockHitResult.Type

val Type.isMiss
    get() = this == Type.MISS

val Type.isBlock
    get() = this == Type.BLOCK

val Type.isEntity
    get() = this == Type.ENTITY