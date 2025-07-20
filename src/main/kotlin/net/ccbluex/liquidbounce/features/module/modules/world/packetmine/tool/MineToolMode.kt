/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2025 CCBlueX
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
package net.ccbluex.liquidbounce.features.module.modules.world.packetmine.tool

import it.unimi.dsi.fastutil.ints.IntObjectImmutablePair
import net.ccbluex.liquidbounce.config.types.Choice
import net.ccbluex.liquidbounce.config.types.ChoiceConfigurable
import net.ccbluex.liquidbounce.features.module.MinecraftShortcuts
import net.ccbluex.liquidbounce.features.module.modules.world.ModuleAutoTool
import net.ccbluex.liquidbounce.features.module.modules.world.packetmine.MineTarget
import net.ccbluex.liquidbounce.features.module.modules.world.packetmine.ModulePacketMine
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.client.world
import net.ccbluex.liquidbounce.utils.item.getEnchantment
import net.ccbluex.liquidbounce.utils.math.sq
import net.minecraft.block.BlockState
import net.minecraft.enchantment.Enchantments
import net.minecraft.entity.attribute.EntityAttributes
import net.minecraft.entity.effect.StatusEffectUtil
import net.minecraft.entity.effect.StatusEffects
import net.minecraft.item.ItemStack
import net.minecraft.registry.tag.FluidTags
import net.minecraft.util.math.BlockPos

/**
 * Determines when to switch to a tool and calculates the breaking process delta.
 */
@Suppress("unused")
abstract class MineToolMode(
    override val choiceName: String,
    val syncOnStart: Boolean = false,
    private val switchesNever: Boolean = false
) : Choice(choiceName), MinecraftShortcuts {

    abstract fun shouldSwitch(mineTarget: MineTarget): Boolean

    open fun getSwitchingMethod() = SwitchMethod.NORMAL

    fun getBlockBreakingDelta(pos: BlockPos, state: BlockState, itemStack: ItemStack?): Float {
        if (switchesNever || itemStack == null) {
            return state.calcBlockBreakingDelta(player, world, pos)
        }

        return calcBlockBreakingDelta(pos, state, itemStack)
    }

    fun getSlot(state: BlockState): IntObjectImmutablePair<ItemStack>? {
        if (switchesNever) {
            return null
        }

        return ModuleAutoTool.toolSelector.activeChoice.getTool(state)?.let {
            IntObjectImmutablePair(it.hotbarSlot, it.itemStack)
        }
    }

    override val parent: ChoiceConfigurable<*>
        get() = ModulePacketMine.switchMode

}

/* tweaked minecraft code start */

/**
 * See [BlockState.calcBlockBreakingDelta]
 */
private fun calcBlockBreakingDelta(pos: BlockPos, state: BlockState, stack: ItemStack): Float {
    val hardness = state.getHardness(world, pos)
    if (hardness == -1f) {
        return 0f
    }

    val suitableMultiplier = if (!state.isToolRequired || stack.isSuitableFor(state)) 30 else 100
    return getBlockBreakingSpeed(state, stack) / hardness / suitableMultiplier
}

private fun getBlockBreakingSpeed(state: BlockState, stack: ItemStack): Float {
    var speed = stack.getMiningSpeedMultiplier(state)

    val enchantmentLevel = stack.getEnchantment(Enchantments.EFFICIENCY)
    if (speed > 1f && enchantmentLevel != 0) {
        /**
         * See: [EntityAttributes.MINING_EFFICIENCY]
         */
        val enchantmentAddition = enchantmentLevel.sq() + 1f
        speed += enchantmentAddition.coerceIn(0f..1024f)
    }

    if (StatusEffectUtil.hasHaste(player)) {
        speed *= 1f + (StatusEffectUtil.getHasteAmplifier(player) + 1).toFloat() * 0.2f
    }

    if (player.hasStatusEffect(StatusEffects.MINING_FATIGUE)) {
        val miningFatigueMultiplier = when (player.getStatusEffect(StatusEffects.MINING_FATIGUE)!!.amplifier) {
            0 -> 0.3f
            1 -> 0.09f
            2 -> 0.0027f
            3 -> 8.1E-4f
            else -> 8.1E-4f
        }

        speed *= miningFatigueMultiplier
    }

    speed *= player.getAttributeValue(EntityAttributes.BLOCK_BREAK_SPEED).toFloat()
    if (player.isSubmergedIn(FluidTags.WATER)) {
        speed *= player.getAttributeInstance(EntityAttributes.SUBMERGED_MINING_SPEED)!!.value.toFloat()
    }

    if (!player.isOnGround) {
        speed /= 5f
    }

    return speed
}

/* tweaked minecraft code end */
