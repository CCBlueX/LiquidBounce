/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.injection.backend

import net.ccbluex.liquidbounce.api.minecraft.stats.IStatBase
import net.minecraft.stats.StatBase

class StatBaseImpl(val wrapped: StatBase) : IStatBase {
    override fun equals(other: Any?): Boolean {
        return other is StatBaseImpl && other.wrapped == this.wrapped
    }
}

inline fun IStatBase.unwrap(): StatBase = (this as StatBaseImpl).wrapped
inline fun StatBase.wrap(): IStatBase = StatBaseImpl(this)