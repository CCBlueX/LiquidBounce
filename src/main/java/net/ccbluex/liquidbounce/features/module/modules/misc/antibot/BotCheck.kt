package net.ccbluex.liquidbounce.features.module.modules.misc.antibot

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.api.minecraft.client.entity.Entity
import net.ccbluex.liquidbounce.api.minecraft.client.entity.player.EntityPlayer
import net.ccbluex.liquidbounce.api.minecraft.client.multiplayer.WorldClient
import net.ccbluex.liquidbounce.api.minecraft.util.Vec3
import net.ccbluex.liquidbounce.event.AttackEvent
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.Render3DEvent
import net.ccbluex.liquidbounce.features.module.modules.misc.AntiBot
import net.ccbluex.liquidbounce.ui.client.hud.element.elements.Notification
import net.ccbluex.liquidbounce.ui.client.hud.element.elements.NotificationIcon
import net.ccbluex.liquidbounce.utils.Location
import net.ccbluex.liquidbounce.utils.LocationCache
import net.ccbluex.liquidbounce.utils.MinecraftInstance
import net.ccbluex.liquidbounce.utils.RotationUtils
import net.ccbluex.liquidbounce.utils.extensions.ping
import kotlin.math.ceil

abstract class BotCheck(val modeName: String) : MinecraftInstance()
{
    abstract val isActive: Boolean

    open fun clear()
    {
    }

    abstract fun isBot(theWorld: WorldClient, thePlayer: Entity, target: EntityPlayer): Boolean

    open fun onPacket(event: PacketEvent)
    {
    }

    open fun onAttack(event: AttackEvent)
    {
    }

    open fun onRender3D(event: Render3DEvent)
    {
    }

    open fun onEntityMove(theWorld: WorldClient, thePlayer: EntityPlayer, target: EntityPlayer, isTeleport: Boolean, newPos: Vec3, rotating: Boolean, newYaw: Float, newPitch: Float, onGround: Boolean)
    {
    }

    fun notification(messageSupplier: () -> Array<String>)
    {
        if (AntiBot.notificationValue.get()) LiquidBounce.hud.addNotification(Notification(NotificationIcon.BOT, "AntiBot", arrayOf("module=$modeName", *messageSupplier()), 6000L))
    }

    fun notification(target: EntityPlayer, message: () -> Array<String>)
    {
        notification { arrayOf("profileName=${target.gameProfile.name}", "displayName=${target.displayName.formattedText}\u00A7r", *message()) }
    }

    fun remove(theWorld: WorldClient?, entityId: Int?, profileName: String, displayName: String?, reason: Array<String> = arrayOf())
    {
        entityId?.let { theWorld?.removeEntityFromWorld(it) }
        if (AntiBot.notificationValue.get()) LiquidBounce.hud.addNotification(Notification(NotificationIcon.BOT, "AntiBot (Remove)", arrayOf("module=$modeName", "profileName=$profileName", "displayName=$displayName\u00A7r", *reason), 6000L))
    }

    companion object
    {
        fun getPingCorrectionAppliedLocation(thePlayer: EntityPlayer, offset: Int = 0) = LocationCache.getPlayerLocationBeforeNTicks((ceil(thePlayer.ping / 50F).toInt() + offset + AntiBot.positionPingCorrectionOffsetValue.get()).coerceAtLeast(0), Location(Vec3(thePlayer.posX, thePlayer.entityBoundingBox.minY, thePlayer.posZ), RotationUtils.serverRotation))
    }
}
