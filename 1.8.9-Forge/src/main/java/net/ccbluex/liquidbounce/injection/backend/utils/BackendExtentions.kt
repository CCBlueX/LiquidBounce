/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

@file:Suppress("NOTHING_TO_INLINE")

package net.ccbluex.liquidbounce.injection.backend.utils

import net.ccbluex.liquidbounce.api.minecraft.client.entity.player.WEnumPlayerModelParts
import net.ccbluex.liquidbounce.api.minecraft.event.IClickEvent
import net.ccbluex.liquidbounce.api.minecraft.network.play.client.ICPacketUseEntity
import net.ccbluex.liquidbounce.api.minecraft.util.*
import net.ccbluex.liquidbounce.api.minecraft.world.IWorldSettings
import net.ccbluex.liquidbounce.api.util.WrappedMutableList
import net.minecraft.entity.player.EnumPlayerModelParts
import net.minecraft.event.ClickEvent
import net.minecraft.network.play.client.C02PacketUseEntity
import net.minecraft.util.*
import net.minecraft.world.WorldSettings

inline fun WVec3.unwrap(): Vec3 = Vec3(this.xCoord, this.yCoord, this.zCoord)
inline fun WVec3i.unwrap(): Vec3i = Vec3i(this.x, this.y, this.z)
inline fun WBlockPos.unwrap(): BlockPos = BlockPos(this.x, this.y, this.z)

inline fun <O, T> MutableList<O>.unwrap(): MutableList<T> = ((this as WrappedMutableList<O, T, *>).wrapped) as MutableList<T>

inline fun BlockPos.wrap(): WBlockPos = WBlockPos(this.x, this.y, this.z)
inline fun Vec3.wrap(): WVec3 = WVec3(this.xCoord, this.yCoord, this.zCoord)
inline fun Vec3i.wrap(): WVec3i = WVec3i(this.x, this.y, this.z)

inline fun MovingObjectPosition.MovingObjectType.wrap(): IMovingObjectPosition.WMovingObjectType {
    return when (this) {
        MovingObjectPosition.MovingObjectType.MISS -> IMovingObjectPosition.WMovingObjectType.MISS
        MovingObjectPosition.MovingObjectType.BLOCK -> IMovingObjectPosition.WMovingObjectType.BLOCK
        MovingObjectPosition.MovingObjectType.ENTITY -> IMovingObjectPosition.WMovingObjectType.ENTITY
    }
}

inline fun WEnumPlayerModelParts.unwrap(): EnumPlayerModelParts {
    return when (this) {
        WEnumPlayerModelParts.CAPE -> EnumPlayerModelParts.CAPE
        WEnumPlayerModelParts.JACKET -> EnumPlayerModelParts.JACKET
        WEnumPlayerModelParts.LEFT_SLEEVE -> EnumPlayerModelParts.LEFT_SLEEVE
        WEnumPlayerModelParts.RIGHT_SLEEVE -> EnumPlayerModelParts.RIGHT_SLEEVE
        WEnumPlayerModelParts.LEFT_PANTS_LEG -> EnumPlayerModelParts.LEFT_PANTS_LEG
        WEnumPlayerModelParts.RIGHT_PANTS_LEG -> EnumPlayerModelParts.RIGHT_PANTS_LEG
        WEnumPlayerModelParts.HAT -> EnumPlayerModelParts.HAT
    }
}

inline fun EnumPlayerModelParts.wrap(): WEnumPlayerModelParts {
    return when (this) {
        EnumPlayerModelParts.CAPE -> WEnumPlayerModelParts.CAPE
        EnumPlayerModelParts.JACKET -> WEnumPlayerModelParts.JACKET
        EnumPlayerModelParts.LEFT_SLEEVE -> WEnumPlayerModelParts.LEFT_SLEEVE
        EnumPlayerModelParts.RIGHT_SLEEVE -> WEnumPlayerModelParts.RIGHT_SLEEVE
        EnumPlayerModelParts.LEFT_PANTS_LEG -> WEnumPlayerModelParts.LEFT_PANTS_LEG
        EnumPlayerModelParts.RIGHT_PANTS_LEG -> WEnumPlayerModelParts.RIGHT_PANTS_LEG
        EnumPlayerModelParts.HAT -> WEnumPlayerModelParts.HAT
    }
}

inline fun EnumChatFormatting.wrap(): WEnumChatFormatting {
    return when (this) {
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

inline fun WEnumChatFormatting.unwrap(): EnumChatFormatting {
    return when (this) {
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


inline fun IWorldSettings.WGameType.unwrap(): WorldSettings.GameType {
    return when (this) {
        IWorldSettings.WGameType.NOT_SET -> WorldSettings.GameType.NOT_SET
        IWorldSettings.WGameType.SURVIVAL -> WorldSettings.GameType.SURVIVAL
        IWorldSettings.WGameType.CREATIVE -> WorldSettings.GameType.CREATIVE
        IWorldSettings.WGameType.ADVENTUR -> WorldSettings.GameType.ADVENTURE
        IWorldSettings.WGameType.SPECTATOR -> WorldSettings.GameType.SPECTATOR
    }
}

inline fun WorldSettings.GameType.wrap(): IWorldSettings.WGameType {
    return when (this) {
        WorldSettings.GameType.NOT_SET -> IWorldSettings.WGameType.NOT_SET
        WorldSettings.GameType.SURVIVAL -> IWorldSettings.WGameType.SURVIVAL
        WorldSettings.GameType.CREATIVE -> IWorldSettings.WGameType.CREATIVE
        WorldSettings.GameType.ADVENTURE -> IWorldSettings.WGameType.ADVENTUR
        WorldSettings.GameType.SPECTATOR -> IWorldSettings.WGameType.SPECTATOR
    }
}

inline fun C02PacketUseEntity.Action.wrap(): ICPacketUseEntity.WAction {
    return when (this) {
        C02PacketUseEntity.Action.INTERACT -> ICPacketUseEntity.WAction.INTERACT
        C02PacketUseEntity.Action.ATTACK -> ICPacketUseEntity.WAction.ATTACK
        C02PacketUseEntity.Action.INTERACT_AT -> ICPacketUseEntity.WAction.INTERACT_AT
    }
}

inline fun ICPacketUseEntity.WAction.unwrap(): C02PacketUseEntity.Action {
    return when (this) {
        ICPacketUseEntity.WAction.INTERACT -> C02PacketUseEntity.Action.INTERACT
        ICPacketUseEntity.WAction.ATTACK -> C02PacketUseEntity.Action.ATTACK
        ICPacketUseEntity.WAction.INTERACT_AT -> C02PacketUseEntity.Action.INTERACT_AT
    }
}

inline fun IClickEvent.WAction.unwrap(): ClickEvent.Action {
    return when (this) {
        IClickEvent.WAction.OPEN_URL -> ClickEvent.Action.OPEN_URL
    }
}