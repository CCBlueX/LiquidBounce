/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.injection.backend

import net.ccbluex.liquidbounce.api.minecraft.util.ITimer
import net.minecraft.util.Timer

class TimerImpl(val wrapped: Timer) : ITimer {
    override var timerSpeed: Float
        get() = wrapped.timerSpeed
        set(value) {
            wrapped.timerSpeed = value
        }
    override val renderPartialTicks: Float
        get() = wrapped.renderPartialTicks

    override fun equals(other: Any?): Boolean {
        return other is TimerImpl && other.wrapped == this.wrapped
    }
}

inline fun ITimer.unwrap(): Timer = (this as TimerImpl).wrapped
inline fun Timer.wrap(): ITimer = TimerImpl(this)