/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

@file:Suppress("NOTHING_TO_")

package net.ccbluex.liquidbounce.injection.backend.utils

import net.ccbluex.liquidbounce.api.minecraft.client.entity.player.WEnumPlayerModelParts
import net.ccbluex.liquidbounce.api.minecraft.event.IClickEvent
import net.ccbluex.liquidbounce.api.minecraft.network.play.client.*
import net.ccbluex.liquidbounce.api.minecraft.util.*
import net.ccbluex.liquidbounce.api.minecraft.world.IWorldSettings
import net.minecraft.entity.player.EnumPlayerModelParts
import net.minecraft.event.ClickEvent
import net.minecraft.network.play.client.*
import net.minecraft.util.*
import net.minecraft.world.WorldSettings

fun WVec3.unwrap(): Vec3 = Vec3(xCoord, yCoord, zCoord)
fun WVec3i.unwrap(): Vec3i = Vec3i(x, y, z)
fun WBlockPos.unwrap(): BlockPos = BlockPos(x, y, z)

fun BlockPos.wrap(): WBlockPos = WBlockPos(x, y, z)
fun Vec3.wrap(): WVec3 = WVec3(xCoord, yCoord, zCoord)
fun Vec3i.wrap(): WVec3i = WVec3i(x, y, z)

fun MovingObjectPosition.MovingObjectType.wrap(): IMovingObjectPosition.WMovingObjectType
{
	return when (this)
	{
		MovingObjectPosition.MovingObjectType.MISS -> IMovingObjectPosition.WMovingObjectType.MISS
		MovingObjectPosition.MovingObjectType.BLOCK -> IMovingObjectPosition.WMovingObjectType.BLOCK
		MovingObjectPosition.MovingObjectType.ENTITY -> IMovingObjectPosition.WMovingObjectType.ENTITY
	}
}

fun WEnumPlayerModelParts.unwrap(): EnumPlayerModelParts
{
	return when (this)
	{
		WEnumPlayerModelParts.CAPE -> EnumPlayerModelParts.CAPE
		WEnumPlayerModelParts.JACKET -> EnumPlayerModelParts.JACKET
		WEnumPlayerModelParts.LEFT_SLEEVE -> EnumPlayerModelParts.LEFT_SLEEVE
		WEnumPlayerModelParts.RIGHT_SLEEVE -> EnumPlayerModelParts.RIGHT_SLEEVE
		WEnumPlayerModelParts.LEFT_PANTS_LEG -> EnumPlayerModelParts.LEFT_PANTS_LEG
		WEnumPlayerModelParts.RIGHT_PANTS_LEG -> EnumPlayerModelParts.RIGHT_PANTS_LEG
		WEnumPlayerModelParts.HAT -> EnumPlayerModelParts.HAT
	}
}

fun EnumPlayerModelParts.wrap(): WEnumPlayerModelParts
{
	return when (this)
	{
		EnumPlayerModelParts.CAPE -> WEnumPlayerModelParts.CAPE
		EnumPlayerModelParts.JACKET -> WEnumPlayerModelParts.JACKET
		EnumPlayerModelParts.LEFT_SLEEVE -> WEnumPlayerModelParts.LEFT_SLEEVE
		EnumPlayerModelParts.RIGHT_SLEEVE -> WEnumPlayerModelParts.RIGHT_SLEEVE
		EnumPlayerModelParts.LEFT_PANTS_LEG -> WEnumPlayerModelParts.LEFT_PANTS_LEG
		EnumPlayerModelParts.RIGHT_PANTS_LEG -> WEnumPlayerModelParts.RIGHT_PANTS_LEG
		EnumPlayerModelParts.HAT -> WEnumPlayerModelParts.HAT
	}
}

fun EnumChatFormatting.wrap(): WEnumChatFormatting
{
	return when (this)
	{
		EnumChatFormatting.BLACK -> WEnumChatFormatting.BLACK
		EnumChatFormatting.DARK_BLUE -> WEnumChatFormatting.DARK_BLUE
		EnumChatFormatting.DARK_GREEN -> WEnumChatFormatting.DARK_GREEN
		EnumChatFormatting.DARK_AQUA -> WEnumChatFormatting.DARK_AQUA
		EnumChatFormatting.DARK_RED -> WEnumChatFormatting.DARK_RED
		EnumChatFormatting.DARK_PURPLE -> WEnumChatFormatting.DARK_PURPLE
		EnumChatFormatting.GOLD -> WEnumChatFormatting.GOLD
		EnumChatFormatting.GRAY -> WEnumChatFormatting.GRAY
		EnumChatFormatting.DARK_GRAY -> WEnumChatFormatting.DARK_GRAY
		EnumChatFormatting.BLUE -> WEnumChatFormatting.BLUE
		EnumChatFormatting.GREEN -> WEnumChatFormatting.GREEN
		EnumChatFormatting.AQUA -> WEnumChatFormatting.AQUA
		EnumChatFormatting.RED -> WEnumChatFormatting.RED
		EnumChatFormatting.LIGHT_PURPLE -> WEnumChatFormatting.LIGHT_PURPLE
		EnumChatFormatting.YELLOW -> WEnumChatFormatting.YELLOW
		EnumChatFormatting.WHITE -> WEnumChatFormatting.WHITE
		EnumChatFormatting.OBFUSCATED -> WEnumChatFormatting.OBFUSCATED
		EnumChatFormatting.BOLD -> WEnumChatFormatting.BOLD
		EnumChatFormatting.STRIKETHROUGH -> WEnumChatFormatting.STRIKETHROUGH
		EnumChatFormatting.UNDERLINE -> WEnumChatFormatting.UNDERLINE
		EnumChatFormatting.ITALIC -> WEnumChatFormatting.ITALIC
		EnumChatFormatting.RESET -> WEnumChatFormatting.RESET
	}
}

fun WEnumChatFormatting.unwrap(): EnumChatFormatting
{
	return when (this)
	{
		WEnumChatFormatting.BLACK -> EnumChatFormatting.BLACK
		WEnumChatFormatting.DARK_BLUE -> EnumChatFormatting.DARK_BLUE
		WEnumChatFormatting.DARK_GREEN -> EnumChatFormatting.DARK_GREEN
		WEnumChatFormatting.DARK_AQUA -> EnumChatFormatting.DARK_AQUA
		WEnumChatFormatting.DARK_RED -> EnumChatFormatting.DARK_RED
		WEnumChatFormatting.DARK_PURPLE -> EnumChatFormatting.DARK_PURPLE
		WEnumChatFormatting.GOLD -> EnumChatFormatting.GOLD
		WEnumChatFormatting.GRAY -> EnumChatFormatting.GRAY
		WEnumChatFormatting.DARK_GRAY -> EnumChatFormatting.DARK_GRAY
		WEnumChatFormatting.BLUE -> EnumChatFormatting.BLUE
		WEnumChatFormatting.GREEN -> EnumChatFormatting.GREEN
		WEnumChatFormatting.AQUA -> EnumChatFormatting.AQUA
		WEnumChatFormatting.RED -> EnumChatFormatting.RED
		WEnumChatFormatting.LIGHT_PURPLE -> EnumChatFormatting.LIGHT_PURPLE
		WEnumChatFormatting.YELLOW -> EnumChatFormatting.YELLOW
		WEnumChatFormatting.WHITE -> EnumChatFormatting.WHITE
		WEnumChatFormatting.OBFUSCATED -> EnumChatFormatting.OBFUSCATED
		WEnumChatFormatting.BOLD -> EnumChatFormatting.BOLD
		WEnumChatFormatting.STRIKETHROUGH -> EnumChatFormatting.STRIKETHROUGH
		WEnumChatFormatting.UNDERLINE -> EnumChatFormatting.UNDERLINE
		WEnumChatFormatting.ITALIC -> EnumChatFormatting.ITALIC
		WEnumChatFormatting.RESET -> EnumChatFormatting.RESET
	}
}

fun IWorldSettings.WGameType.unwrap(): WorldSettings.GameType
{
	return when (this)
	{
		IWorldSettings.WGameType.NOT_SET -> WorldSettings.GameType.NOT_SET
		IWorldSettings.WGameType.SURVIVAL -> WorldSettings.GameType.SURVIVAL
		IWorldSettings.WGameType.CREATIVE -> WorldSettings.GameType.CREATIVE
		IWorldSettings.WGameType.ADVENTUR -> WorldSettings.GameType.ADVENTURE
		IWorldSettings.WGameType.SPECTATOR -> WorldSettings.GameType.SPECTATOR
	}
}

fun WorldSettings.GameType.wrap(): IWorldSettings.WGameType
{
	return when (this)
	{
		WorldSettings.GameType.NOT_SET -> IWorldSettings.WGameType.NOT_SET
		WorldSettings.GameType.SURVIVAL -> IWorldSettings.WGameType.SURVIVAL
		WorldSettings.GameType.CREATIVE -> IWorldSettings.WGameType.CREATIVE
		WorldSettings.GameType.ADVENTURE -> IWorldSettings.WGameType.ADVENTUR
		WorldSettings.GameType.SPECTATOR -> IWorldSettings.WGameType.SPECTATOR
	}
}

fun C02PacketUseEntity.Action.wrap(): ICPacketUseEntity.WAction
{
	return when (this)
	{
		C02PacketUseEntity.Action.INTERACT -> ICPacketUseEntity.WAction.INTERACT
		C02PacketUseEntity.Action.ATTACK -> ICPacketUseEntity.WAction.ATTACK
		C02PacketUseEntity.Action.INTERACT_AT -> ICPacketUseEntity.WAction.INTERACT_AT
	}
}

fun ICPacketUseEntity.WAction.unwrap(): C02PacketUseEntity.Action
{
	return when (this)
	{
		ICPacketUseEntity.WAction.INTERACT -> C02PacketUseEntity.Action.INTERACT
		ICPacketUseEntity.WAction.ATTACK -> C02PacketUseEntity.Action.ATTACK
		ICPacketUseEntity.WAction.INTERACT_AT -> C02PacketUseEntity.Action.INTERACT_AT
	}
}

fun IClickEvent.WAction.unwrap(): ClickEvent.Action
{
	return when (this)
	{
		IClickEvent.WAction.OPEN_URL -> ClickEvent.Action.OPEN_URL
	}
}

fun ICPacketClientStatus.WEnumState.unwrap(): C16PacketClientStatus.EnumState
{
	return when (this)
	{
		ICPacketClientStatus.WEnumState.PERFORM_RESPAWN -> C16PacketClientStatus.EnumState.PERFORM_RESPAWN
		ICPacketClientStatus.WEnumState.REQUEST_STATS -> C16PacketClientStatus.EnumState.REQUEST_STATS
		ICPacketClientStatus.WEnumState.OPEN_INVENTORY_ACHIEVEMENT -> C16PacketClientStatus.EnumState.OPEN_INVENTORY_ACHIEVEMENT
	}
}

fun ICPacketPlayerDigging.WAction.unwrap(): C07PacketPlayerDigging.Action
{
	return when (this)
	{
		ICPacketPlayerDigging.WAction.START_DESTROY_BLOCK -> C07PacketPlayerDigging.Action.START_DESTROY_BLOCK
		ICPacketPlayerDigging.WAction.ABORT_DESTROY_BLOCK -> C07PacketPlayerDigging.Action.ABORT_DESTROY_BLOCK
		ICPacketPlayerDigging.WAction.STOP_DESTROY_BLOCK -> C07PacketPlayerDigging.Action.STOP_DESTROY_BLOCK
		ICPacketPlayerDigging.WAction.DROP_ALL_ITEMS -> C07PacketPlayerDigging.Action.DROP_ALL_ITEMS
		ICPacketPlayerDigging.WAction.DROP_ITEM -> C07PacketPlayerDigging.Action.DROP_ITEM
		ICPacketPlayerDigging.WAction.RELEASE_USE_ITEM -> C07PacketPlayerDigging.Action.RELEASE_USE_ITEM
	}
}

fun ICPacketResourcePackStatus.WAction.unwrap(): C19PacketResourcePackStatus.Action
{
	return when (this)
	{
		ICPacketResourcePackStatus.WAction.SUCCESSFULLY_LOADED -> C19PacketResourcePackStatus.Action.SUCCESSFULLY_LOADED
		ICPacketResourcePackStatus.WAction.DECLINED -> C19PacketResourcePackStatus.Action.DECLINED
		ICPacketResourcePackStatus.WAction.FAILED_DOWNLOAD -> C19PacketResourcePackStatus.Action.FAILED_DOWNLOAD
		ICPacketResourcePackStatus.WAction.ACCEPTED -> C19PacketResourcePackStatus.Action.ACCEPTED
	}
}

fun ICPacketEntityAction.WAction.unwrap(): C0BPacketEntityAction.Action
{
	return when (this)
	{
		ICPacketEntityAction.WAction.START_SNEAKING -> C0BPacketEntityAction.Action.START_SNEAKING
		ICPacketEntityAction.WAction.STOP_SNEAKING -> C0BPacketEntityAction.Action.STOP_SNEAKING
		ICPacketEntityAction.WAction.STOP_SLEEPING -> C0BPacketEntityAction.Action.STOP_SLEEPING
		ICPacketEntityAction.WAction.START_SPRINTING -> C0BPacketEntityAction.Action.START_SPRINTING
		ICPacketEntityAction.WAction.STOP_SPRINTING -> C0BPacketEntityAction.Action.STOP_SPRINTING
		ICPacketEntityAction.WAction.OPEN_INVENTORY -> C0BPacketEntityAction.Action.OPEN_INVENTORY
	}
}

fun C07PacketPlayerDigging.Action.wrap(): ICPacketPlayerDigging.WAction
{
	return when (this)
	{
		C07PacketPlayerDigging.Action.START_DESTROY_BLOCK -> ICPacketPlayerDigging.WAction.START_DESTROY_BLOCK
		C07PacketPlayerDigging.Action.ABORT_DESTROY_BLOCK -> ICPacketPlayerDigging.WAction.ABORT_DESTROY_BLOCK
		C07PacketPlayerDigging.Action.STOP_DESTROY_BLOCK -> ICPacketPlayerDigging.WAction.STOP_DESTROY_BLOCK
		C07PacketPlayerDigging.Action.DROP_ALL_ITEMS -> ICPacketPlayerDigging.WAction.DROP_ALL_ITEMS
		C07PacketPlayerDigging.Action.DROP_ITEM -> ICPacketPlayerDigging.WAction.DROP_ITEM
		C07PacketPlayerDigging.Action.RELEASE_USE_ITEM -> ICPacketPlayerDigging.WAction.RELEASE_USE_ITEM
	}
}
