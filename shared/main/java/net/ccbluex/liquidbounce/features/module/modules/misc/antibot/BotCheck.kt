package net.ccbluex.liquidbounce.features.module.modules.misc.antibot

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntity
import net.ccbluex.liquidbounce.api.minecraft.client.entity.player.IEntityPlayer
import net.ccbluex.liquidbounce.api.minecraft.client.multiplayer.IWorldClient
import net.ccbluex.liquidbounce.api.minecraft.util.WVec3
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

	abstract fun isBot(theWorld: IWorldClient, thePlayer: IEntity, target: IEntityPlayer): Boolean

	open fun onPacket(event: PacketEvent)
	{
	}

	open fun onAttack(event: AttackEvent)
	{
	}

	open fun onRender3D(event: Render3DEvent)
	{
	}

	open fun onEntityMove(theWorld: IWorldClient, thePlayer: IEntityPlayer, target: IEntityPlayer, isTeleport: Boolean, newPos: WVec3, rotating: Boolean, newYaw: Float, newPitch: Float, onGround: Boolean)
	{
	}

	fun notification(message: () -> String)
	{
		if (AntiBot.notificationValue.get()) LiquidBounce.hud.addNotification(Notification(NotificationIcon.ROBOT, modeName, message(), 6000L))
	}

	fun notification(target: IEntityPlayer, message: () -> String)
	{
		notification { "$message ${target.gameProfile.name} (${target.displayName.formattedText}\u00A7r)" }
	}

	fun remove(theWorld: IWorldClient, entityId: Int?, profileName: String, displayName: String?, reason: String)
	{
		entityId?.let(theWorld::removeEntityFromWorld)
		notification { "Removed $profileName ($displayName\u00A7r) from the game ($reason)" }
	}

	companion object
	{
		fun getPingCorrectionAppliedLocation(thePlayer: IEntityPlayer, offset: Int = 0) = LocationCache.getPlayerLocationBeforeNTicks((ceil(thePlayer.ping / 50F).toInt() + offset + AntiBot.positionPingCorrectionOffsetValue.get()).coerceAtLeast(0), Location(WVec3(thePlayer.posX, thePlayer.entityBoundingBox.minY, thePlayer.posZ), RotationUtils.serverRotation))
	}
}
