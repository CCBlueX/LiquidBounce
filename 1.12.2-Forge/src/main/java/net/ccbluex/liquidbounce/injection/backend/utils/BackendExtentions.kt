/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

@file:Suppress("NOTHING_TO_INLINE")

package net.ccbluex.liquidbounce.injection.backend.utils

import net.ccbluex.liquidbounce.api.enums.WEnumHand
import net.ccbluex.liquidbounce.api.minecraft.client.entity.player.WEnumPlayerModelParts
import net.ccbluex.liquidbounce.api.minecraft.event.IClickEvent
import net.ccbluex.liquidbounce.api.minecraft.network.play.client.*
import net.ccbluex.liquidbounce.api.minecraft.util.*
import net.ccbluex.liquidbounce.api.minecraft.world.IWorldSettings
import net.ccbluex.liquidbounce.injection.backend.Backend.BACKEND_UNSUPPORTED
import net.minecraft.entity.player.EnumPlayerModelParts
import net.minecraft.inventory.ClickType
import net.minecraft.inventory.EntityEquipmentSlot
import net.minecraft.network.play.client.*
import net.minecraft.util.EnumHand
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.RayTraceResult
import net.minecraft.util.math.Vec3d
import net.minecraft.util.math.Vec3i
import net.minecraft.util.text.TextFormatting
import net.minecraft.util.text.event.ClickEvent
import net.minecraft.world.GameType

inline fun WVec3.unwrap(): Vec3d = Vec3d(this.xCoord, this.yCoord, this.zCoord)
inline fun WVec3i.unwrap(): Vec3i = Vec3i(this.x, this.y, this.z)
inline fun WBlockPos.unwrap(): BlockPos = BlockPos(this.x, this.y, this.z)

inline fun BlockPos.wrap(): WBlockPos = WBlockPos(this.x, this.y, this.z)
inline fun Vec3d.wrap(): WVec3 = WVec3(this.x, this.y, this.z)
inline fun Vec3i.wrap(): WVec3i = WVec3i(this.x, this.y, this.z)

inline fun RayTraceResult.Type.wrap(): IMovingObjectPosition.WMovingObjectType {
    return when (this) {
        RayTraceResult.Type.MISS -> IMovingObjectPosition.WMovingObjectType.MISS
        RayTraceResult.Type.BLOCK -> IMovingObjectPosition.WMovingObjectType.BLOCK
        RayTraceResult.Type.ENTITY -> IMovingObjectPosition.WMovingObjectType.ENTITY
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

inline fun TextFormatting.wrap(): WEnumChatFormatting {
    return when (this) {
        TextFormatting.BLACK -> WEnumChatFormatting.BLACK
        TextFormatting.DARK_BLUE -> WEnumChatFormatting.DARK_BLUE
        TextFormatting.DARK_GREEN -> WEnumChatFormatting.DARK_GREEN
        TextFormatting.DARK_AQUA -> WEnumChatFormatting.DARK_AQUA
        TextFormatting.DARK_RED -> WEnumChatFormatting.DARK_RED
        TextFormatting.DARK_PURPLE -> WEnumChatFormatting.DARK_PURPLE
        TextFormatting.GOLD -> WEnumChatFormatting.GOLD
        TextFormatting.GRAY -> WEnumChatFormatting.GRAY
        TextFormatting.DARK_GRAY -> WEnumChatFormatting.DARK_GRAY
        TextFormatting.BLUE -> WEnumChatFormatting.BLUE
        TextFormatting.GREEN -> WEnumChatFormatting.GREEN
        TextFormatting.AQUA -> WEnumChatFormatting.AQUA
        TextFormatting.RED -> WEnumChatFormatting.RED
        TextFormatting.LIGHT_PURPLE -> WEnumChatFormatting.LIGHT_PURPLE
        TextFormatting.YELLOW -> WEnumChatFormatting.YELLOW
        TextFormatting.WHITE -> WEnumChatFormatting.WHITE
        TextFormatting.OBFUSCATED -> WEnumChatFormatting.OBFUSCATED
        TextFormatting.BOLD -> WEnumChatFormatting.BOLD
        TextFormatting.STRIKETHROUGH -> WEnumChatFormatting.STRIKETHROUGH
        TextFormatting.UNDERLINE -> WEnumChatFormatting.UNDERLINE
        TextFormatting.ITALIC -> WEnumChatFormatting.ITALIC
        TextFormatting.RESET -> WEnumChatFormatting.RESET
    }
}

inline fun WEnumChatFormatting.unwrap(): TextFormatting {
    return when (this) {
        WEnumChatFormatting.BLACK -> TextFormatting.BLACK
        WEnumChatFormatting.DARK_BLUE -> TextFormatting.DARK_BLUE
        WEnumChatFormatting.DARK_GREEN -> TextFormatting.DARK_GREEN
        WEnumChatFormatting.DARK_AQUA -> TextFormatting.DARK_AQUA
        WEnumChatFormatting.DARK_RED -> TextFormatting.DARK_RED
        WEnumChatFormatting.DARK_PURPLE -> TextFormatting.DARK_PURPLE
        WEnumChatFormatting.GOLD -> TextFormatting.GOLD
        WEnumChatFormatting.GRAY -> TextFormatting.GRAY
        WEnumChatFormatting.DARK_GRAY -> TextFormatting.DARK_GRAY
        WEnumChatFormatting.BLUE -> TextFormatting.BLUE
        WEnumChatFormatting.GREEN -> TextFormatting.GREEN
        WEnumChatFormatting.AQUA -> TextFormatting.AQUA
        WEnumChatFormatting.RED -> TextFormatting.RED
        WEnumChatFormatting.LIGHT_PURPLE -> TextFormatting.LIGHT_PURPLE
        WEnumChatFormatting.YELLOW -> TextFormatting.YELLOW
        WEnumChatFormatting.WHITE -> TextFormatting.WHITE
        WEnumChatFormatting.OBFUSCATED -> TextFormatting.OBFUSCATED
        WEnumChatFormatting.BOLD -> TextFormatting.BOLD
        WEnumChatFormatting.STRIKETHROUGH -> TextFormatting.STRIKETHROUGH
        WEnumChatFormatting.UNDERLINE -> TextFormatting.UNDERLINE
        WEnumChatFormatting.ITALIC -> TextFormatting.ITALIC
        WEnumChatFormatting.RESET -> TextFormatting.RESET
    }
}


inline fun IWorldSettings.WGameType.unwrap(): GameType {
    return when (this) {
        IWorldSettings.WGameType.NOT_SET -> GameType.NOT_SET
        IWorldSettings.WGameType.SURVIVAL -> GameType.SURVIVAL
        IWorldSettings.WGameType.CREATIVE -> GameType.CREATIVE
        IWorldSettings.WGameType.ADVENTUR -> GameType.ADVENTURE
        IWorldSettings.WGameType.SPECTATOR -> GameType.SPECTATOR
    }
}

inline fun GameType.wrap(): IWorldSettings.WGameType {
    return when (this) {
        GameType.NOT_SET -> IWorldSettings.WGameType.NOT_SET
        GameType.SURVIVAL -> IWorldSettings.WGameType.SURVIVAL
        GameType.CREATIVE -> IWorldSettings.WGameType.CREATIVE
        GameType.ADVENTURE -> IWorldSettings.WGameType.ADVENTUR
        GameType.SPECTATOR -> IWorldSettings.WGameType.SPECTATOR
    }
}

inline fun CPacketUseEntity.Action.wrap(): ICPacketUseEntity.WAction {
    return when (this) {
        CPacketUseEntity.Action.INTERACT -> ICPacketUseEntity.WAction.INTERACT
        CPacketUseEntity.Action.ATTACK -> ICPacketUseEntity.WAction.ATTACK
        CPacketUseEntity.Action.INTERACT_AT -> ICPacketUseEntity.WAction.INTERACT_AT
    }
}

inline fun ICPacketUseEntity.WAction.unwrap(): CPacketUseEntity.Action {
    return when (this) {
        ICPacketUseEntity.WAction.INTERACT -> CPacketUseEntity.Action.INTERACT
        ICPacketUseEntity.WAction.ATTACK -> CPacketUseEntity.Action.ATTACK
        ICPacketUseEntity.WAction.INTERACT_AT -> CPacketUseEntity.Action.INTERACT_AT
    }
}

inline fun IClickEvent.WAction.unwrap(): ClickEvent.Action {
    return when (this) {
        IClickEvent.WAction.OPEN_URL -> ClickEvent.Action.OPEN_URL
    }
}

inline fun ICPacketClientStatus.WEnumState.unwrap(): CPacketClientStatus.State {
    return when (this) {
        ICPacketClientStatus.WEnumState.PERFORM_RESPAWN -> CPacketClientStatus.State.PERFORM_RESPAWN
        ICPacketClientStatus.WEnumState.REQUEST_STATS -> CPacketClientStatus.State.REQUEST_STATS
        ICPacketClientStatus.WEnumState.OPEN_INVENTORY_ACHIEVEMENT -> BACKEND_UNSUPPORTED()
    }
}

inline fun ICPacketPlayerDigging.WAction.unwrap(): CPacketPlayerDigging.Action {
    return when (this) {
        ICPacketPlayerDigging.WAction.START_DESTROY_BLOCK -> CPacketPlayerDigging.Action.START_DESTROY_BLOCK
        ICPacketPlayerDigging.WAction.ABORT_DESTROY_BLOCK -> CPacketPlayerDigging.Action.ABORT_DESTROY_BLOCK
        ICPacketPlayerDigging.WAction.STOP_DESTROY_BLOCK -> CPacketPlayerDigging.Action.STOP_DESTROY_BLOCK
        ICPacketPlayerDigging.WAction.DROP_ALL_ITEMS -> CPacketPlayerDigging.Action.DROP_ALL_ITEMS
        ICPacketPlayerDigging.WAction.DROP_ITEM -> CPacketPlayerDigging.Action.DROP_ITEM
        ICPacketPlayerDigging.WAction.RELEASE_USE_ITEM -> CPacketPlayerDigging.Action.RELEASE_USE_ITEM
    }
}

inline fun ICPacketResourcePackStatus.WAction.unwrap(): CPacketResourcePackStatus.Action {
    return when (this) {
        ICPacketResourcePackStatus.WAction.SUCCESSFULLY_LOADED -> CPacketResourcePackStatus.Action.SUCCESSFULLY_LOADED
        ICPacketResourcePackStatus.WAction.DECLINED -> CPacketResourcePackStatus.Action.DECLINED
        ICPacketResourcePackStatus.WAction.FAILED_DOWNLOAD -> CPacketResourcePackStatus.Action.FAILED_DOWNLOAD
        ICPacketResourcePackStatus.WAction.ACCEPTED -> CPacketResourcePackStatus.Action.ACCEPTED
    }
}

inline fun ICPacketEntityAction.WAction.unwrap(): CPacketEntityAction.Action {
    return when (this) {
        ICPacketEntityAction.WAction.START_SNEAKING -> CPacketEntityAction.Action.START_SNEAKING
        ICPacketEntityAction.WAction.STOP_SNEAKING -> CPacketEntityAction.Action.STOP_SNEAKING
        ICPacketEntityAction.WAction.STOP_SLEEPING -> CPacketEntityAction.Action.STOP_SLEEPING
        ICPacketEntityAction.WAction.START_SPRINTING -> CPacketEntityAction.Action.START_SPRINTING
        ICPacketEntityAction.WAction.STOP_SPRINTING -> CPacketEntityAction.Action.STOP_SPRINTING
        ICPacketEntityAction.WAction.OPEN_INVENTORY -> CPacketEntityAction.Action.OPEN_INVENTORY
    }
}

inline fun Int.toEntityEquipmentSlot(): EntityEquipmentSlot {
    return when (this) {
        0 -> EntityEquipmentSlot.FEET
        1 -> EntityEquipmentSlot.LEGS
        2 -> EntityEquipmentSlot.CHEST
        3 -> EntityEquipmentSlot.HEAD
        4 -> EntityEquipmentSlot.MAINHAND
        5 -> EntityEquipmentSlot.OFFHAND
        else -> throw IllegalArgumentException("Invalid armorType $this")
    }
}

inline fun Int.toClickType(): ClickType {
    return when (this) {
        0 -> ClickType.PICKUP
        1 -> ClickType.QUICK_MOVE
        2 -> ClickType.SWAP
        3 -> ClickType.CLONE
        4 -> ClickType.THROW
        5 -> ClickType.QUICK_CRAFT
        6 -> ClickType.PICKUP_ALL
        else -> throw IllegalArgumentException("Invalid mode $this")
    }
}

inline fun ClickType.toInt(): Int {
    return when (this) {
        ClickType.PICKUP -> 0
        ClickType.QUICK_MOVE -> 1
        ClickType.SWAP -> 2
        ClickType.CLONE -> 3
        ClickType.THROW -> 4
        ClickType.QUICK_CRAFT -> 5
        ClickType.PICKUP_ALL -> 6
        else -> throw IllegalArgumentException("Invalid mode $this")
    }
}

inline fun WEnumHand.unwrap(): EnumHand {
    return when (this) {
        WEnumHand.MAIN_HAND -> EnumHand.MAIN_HAND
        WEnumHand.OFF_HAND -> EnumHand.OFF_HAND
    }
}