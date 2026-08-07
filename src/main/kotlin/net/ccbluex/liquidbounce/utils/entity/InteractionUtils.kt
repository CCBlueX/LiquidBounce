/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2026 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LiquidBounce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 */

package net.ccbluex.liquidbounce.utils.entity

import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.block.SwingMode
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.network.useItem
import net.minecraft.client.multiplayer.MultiPlayerGameMode
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.InteractionResult.SwingSource
import net.minecraft.world.entity.Entity
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.EntityHitResult

fun InteractionResult.shouldSwingHand() =
    this is InteractionResult.Success && this.swingSource === SwingSource.CLIENT

private inline val gameMode: MultiPlayerGameMode
    get() = mc.gameMode!!

/**
 * ## Vanilla use item packet sequence
 *
 * ### On Entity
 *
 * - InteractAt (>=1.8)
 * - Interact (<=1.21.11)
 * - UseItem
 *
 * ### On block
 *
 * - UseItemOn
 * - UseItem
 *
 * If the effective hand (item) is offhand, the packets are doubled (main hand -> offhand).
 */
enum class StrictInteractionSource {
    INTERACT,
    USE_ITEM_ON,
    USE_ITEM,
}

/**
 * *Strict* means to 1:1 simulate [net.minecraft.client.Minecraft.startUseItem] logic:
 * Try `interact`/`useItemOn` then `useItem` with each hand (main hand -> offhand).
 */
@JvmRecord
data class StrictInteractionResult(
    val hand: InteractionHand,
    val source: StrictInteractionSource,
    val result: InteractionResult,
) {
    val isUseItemSuccess: Boolean
        get() = source == StrictInteractionSource.USE_ITEM && result is InteractionResult.Success
}

@JvmOverloads
fun useItem(
    hand: InteractionHand,
    yRot: Float = RotationManager.currentRotation?.yRot ?: player.yRot,
    xRot: Float = RotationManager.currentRotation?.xRot ?: player.xRot,
    swingMode: SwingMode = SwingMode.DO_NOT_HIDE,
): InteractionResult {
    val useItemResult = gameMode.useItem(player, hand, yRot, xRot)

    if (useItemResult is InteractionResult.Success) {
        if (useItemResult.swingSource === SwingSource.CLIENT) {
            swingMode.accept(hand)
        }

        mc.gameRenderer.itemInHandRenderer.itemUsed(hand)
    }

    return useItemResult
}

fun useItemStrict(
    yRot: Float = RotationManager.currentRotation?.yRot ?: player.yRot,
    xRot: Float = RotationManager.currentRotation?.xRot ?: player.xRot,
    swingMode: SwingMode = SwingMode.DO_NOT_HIDE,
): StrictInteractionResult? {
    return InteractionHand.entries.firstNotNullOfOrNull { hand ->
        val useItemResult = useItem(hand, yRot, xRot, swingMode)
        if (useItemResult is InteractionResult.Success) {
            StrictInteractionResult(
                hand = hand,
                source = StrictInteractionSource.USE_ITEM,
                result = useItemResult,
            )
        } else {
            null
        }
    }
}

/**
 * Simulated [net.minecraft.world.phys.HitResult.Type.ENTITY] branch in vanilla
 * No fallback [MultiPlayerGameMode.useItem] call
 *
 * @see net.minecraft.client.Minecraft.startUseItem
 * @return Cannot interact -> null; else -> [MultiPlayerGameMode.interact] result
 */
fun interactEntity(
    entity: Entity,
    hitResult: EntityHitResult = EntityHitResult(entity),
    hand: InteractionHand = InteractionHand.MAIN_HAND,
    swingMode: SwingMode = SwingMode.DO_NOT_HIDE,
): InteractionResult? {
    val level = entity.level()
    if (!level.worldBorder.isWithinBounds(entity.blockPosition())) {
        return null
    }
    // Skipped check:
    // player.isWithinEntityInteractionRange(entity, 0.0)

    // ViaVersion handles 1.7.6/1.21.11/current protocol of this packet
    val result = gameMode.interact(player, entity, hitResult, hand)

    if (result.shouldSwingHand()) {
        swingMode.swing(hand)
    }

    return result
}

/**
 * @return Cannot interact -> null; else -> [MultiPlayerGameMode.interact] or [MultiPlayerGameMode.useItem] result
 */
fun interactEntityLikeVanilla(
    entity: Entity,
    hitResult: EntityHitResult = EntityHitResult(entity),
    swingMode: SwingMode = SwingMode.DO_NOT_HIDE,
    rotation: Rotation = RotationManager.currentRotation ?: player.rotation,
): StrictInteractionResult? {
    fun interactEntityOrUseItem(
        hand: InteractionHand,
    ): StrictInteractionResult? {
        val interactResult = interactEntity(entity, hitResult, hand, swingMode) ?: return null
        if (interactResult is InteractionResult.Success) {
            return StrictInteractionResult(
                hand = hand,
                source = StrictInteractionSource.INTERACT,
                result = interactResult,
            )
        }
        val useItemResult = useItem(
            hand,
            rotation.yRot,
            rotation.xRot,
            swingMode,
        )
        if (useItemResult is InteractionResult.Success) {
            return StrictInteractionResult(
                hand = hand,
                source = StrictInteractionSource.USE_ITEM,
                result = useItemResult,
            )
        }

        return null
    }

    return InteractionHand.entries.firstNotNullOfOrNull { hand ->
        interactEntityOrUseItem(hand)
    }
}

/**
 * Simulated [net.minecraft.world.phys.HitResult.Type.BLOCK] branch in vanilla
 * No fallback [MultiPlayerGameMode.useItem] call
 *
 * @see net.minecraft.client.Minecraft.startUseItem
 * @return [MultiPlayerGameMode.useItemOn] result
 */
fun interactBlock(
    hitResult: BlockHitResult,
    hand: InteractionHand = InteractionHand.MAIN_HAND,
    swingMode: SwingMode = SwingMode.DO_NOT_HIDE,
): InteractionResult {
    val itemStack = player.getItemInHand(hand)
    val oldCount = itemStack.count
    val useResult = gameMode.useItemOn(player, hand, hitResult)
    if (useResult is InteractionResult.Success) {
        if (useResult.swingSource === SwingSource.CLIENT) {
            swingMode.swing(hand)
            if (!itemStack.isEmpty && (itemStack.count != oldCount || player.hasInfiniteMaterials())) {
                mc.gameRenderer.itemInHandRenderer.itemUsed(hand)
            }
        }
    }

    return useResult
}

/**
 * @return [MultiPlayerGameMode.useItemOn] or [MultiPlayerGameMode.useItem] result
 */
fun interactBlockLikeVanilla(
    hitResult: BlockHitResult,
    swingMode: SwingMode = SwingMode.DO_NOT_HIDE,
    rotation: Rotation = RotationManager.currentRotation ?: player.rotation,
): StrictInteractionResult? {
    fun interactBlockOrUseItem(
        hand: InteractionHand,
    ): StrictInteractionResult? {
        val interactResult = interactBlock(hitResult, hand, swingMode)
        if (interactResult is InteractionResult.Success || interactResult is InteractionResult.Fail) {
            return StrictInteractionResult(
                hand = hand,
                source = StrictInteractionSource.USE_ITEM_ON,
                result = interactResult,
            )
        }
        val useItemResult = useItem(
            hand,
            rotation.yRot,
            rotation.xRot,
            swingMode,
        )
        if (useItemResult is InteractionResult.Success) {
            return StrictInteractionResult(
                hand = hand,
                source = StrictInteractionSource.USE_ITEM,
                result = useItemResult,
            )
        }

        return null
    }

    return InteractionHand.entries.firstNotNullOfOrNull { hand ->
        interactBlockOrUseItem(hand)
    }
}
