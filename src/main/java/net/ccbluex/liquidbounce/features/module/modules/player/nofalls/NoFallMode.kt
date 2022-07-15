/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.player.nofalls

import net.ccbluex.liquidbounce.event.EventState
import net.ccbluex.liquidbounce.event.MoveEvent
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.features.module.modules.player.NoFall
import net.ccbluex.liquidbounce.utils.MinecraftInstance
import net.minecraft.entity.Entity
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.server.S08PacketPlayerPosLook

abstract class NoFallMode(val modeName: String) : MinecraftInstance()
{
    var noSpoof: Int
        get() = NoFall.noSpoof
        set(value)
        {
            NoFall.noSpoof = value
        }

    var groundFallDistance: Float
        get() = NoFall.groundFallDistance
        set(value)
        {
            NoFall.groundFallDistance = value
        }

    val jumped: Boolean
        get() = NoFall.jumped

    open fun onEnable()
    {
    }

    open fun onDisable()
    {
    }

    open fun onUpdate()
    {
    }

    open fun onMotion(eventState: EventState)
    {
    }

    open fun onMove(event: MoveEvent)
    {
    }

    open fun onPacket(event: PacketEvent)
    {
    }

    open fun onMovePacket(packet: C03PacketPlayer): Boolean = false

    open fun onTeleport(packet: S08PacketPlayerPosLook)
    {
    }

    fun checkFallDistance(thePlayer: Entity, minimum: Float = 0f) = thePlayer.fallDistance > groundFallDistance + NoFall.thresholdFallDistanceValue.get().coerceAtLeast(minimum)
}
