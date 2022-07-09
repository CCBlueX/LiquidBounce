/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speeds

import net.ccbluex.liquidbounce.api.minecraft.client.entity.player.EntityPlayer
import net.ccbluex.liquidbounce.event.EventState
import net.ccbluex.liquidbounce.event.MoveEvent
import net.ccbluex.liquidbounce.utils.MinecraftInstance
import net.ccbluex.liquidbounce.utils.extensions.boost

abstract class SpeedMode(val modeName: String) : MinecraftInstance()
{
    abstract fun onMotion(eventState: EventState)
    abstract fun onUpdate()
    abstract fun onMove(event: MoveEvent)

    open fun onTick()
    {
    }

    open fun onEnable()
    {
    }

    open fun onDisable()
    {
    }

    protected fun jump(thePlayer: EntityPlayer)
    {
        thePlayer.jump() // Jump without sprint-jump boost

        // Apply the sprint-jump boost manually
        thePlayer.boost(0.2f)
    }
}
