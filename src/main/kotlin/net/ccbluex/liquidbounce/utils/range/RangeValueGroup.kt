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
package net.ccbluex.liquidbounce.utils.range

import net.ccbluex.liquidbounce.config.types.group.ValueGroup
import net.ccbluex.liquidbounce.features.module.MinecraftShortcuts
import net.minecraft.core.component.DataComponents
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.component.AttackRange
import net.minecraft.world.phys.Vec3
import kotlin.math.max
import kotlin.math.min

/**
 * Allows adjusting your attack range and scan range.
 */
open class RangeValueGroup(
    name: String,
    maxRangeIncrease: Float,
    throughWallsRange: Float
) : ValueGroup(name), MinecraftShortcuts {

    /**
     * @see net.minecraft.world.entity.player.Player.entityInteractionRange
     */
    internal val interactionRange: Float
        get() = (mc.player?.getAttributeValue(Attributes.ENTITY_INTERACTION_RANGE)?.toFloat()
            ?: 3.0F) + maxRangeIncrease

    internal val interactionThroughWallsRange
        get() = throughWallsRange

    /**
     * Increases the attack max-range.
     *
     * When min-range is introduced, rename from "RangeIncrease" to "MaxRangeIncrease"
     * and add "RangeIncrease" as an alias.
     */
    protected var maxRangeIncrease by float(
        "RangeIncrease",
        maxRangeIncrease,
        0.0f..5f,
        "blocks"
    )

    /**
     * Decreases the attack min-range.
     *
     * This is a placeholder until required.
     * There is no vanilla item that is making use of this for now.
     *
     * The spear is executed on the server-side and only uses min range as a visual indicator.
     * @see net.minecraft.client.Minecraft.startAttack
     * @see net.minecraft.client.multiplayer.MultiPlayerGameMode.piercingAttack
     */
    // private val minRangeDecrease by float("MinRangeDecrease", 0f, 0f..2f, "blocks")

    /**
     * This will use only this value for non-visible entities. Originally, we could never attack through walls,
     * so this makes sense to keep starting from 0.0.
     */
    protected var throughWallsRange by float(
        "ThroughWallsRange",
        throughWallsRange,
        0f..8f,
        "blocks"
    ).onChange {
        min(interactionRange, it)
    }

    fun adjustAttackRange(attackRange: AttackRange = AttackRange.defaultFor(player)) =
        AttackRange(
            max(0f, attackRange.minRange/* - minRangeDecrease*/),
            attackRange.maxRange + this@RangeValueGroup.maxRangeIncrease,
            max(0f, attackRange.minCreativeRange/* - minRangeDecrease*/),
            attackRange.maxCreativeRange + this@RangeValueGroup.maxRangeIncrease,
            attackRange.hitboxMargin,
            attackRange.mobFactor
        )

    fun getAttackRange(itemStack: ItemStack = player.getItemInHand(InteractionHand.MAIN_HAND)) = adjustAttackRange(
        itemStack.get(DataComponents.ATTACK_RANGE) ?: AttackRange.defaultFor(player)
    )

    fun isInRange(itemStack: ItemStack = player.getItemInHand(InteractionHand.MAIN_HAND), pos: Vec3) =
        getAttackRange(itemStack).isInRange(player, pos)

}
