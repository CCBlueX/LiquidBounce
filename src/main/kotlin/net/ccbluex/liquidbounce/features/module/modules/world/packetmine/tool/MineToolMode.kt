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
package net.ccbluex.liquidbounce.features.module.modules.world.packetmine.tool

import it.unimi.dsi.fastutil.ints.IntObjectImmutablePair
import net.ccbluex.liquidbounce.config.types.group.Mode
import net.ccbluex.liquidbounce.config.types.group.ModeValueGroup
import net.ccbluex.liquidbounce.features.module.MinecraftShortcuts
import net.ccbluex.liquidbounce.features.module.modules.world.ModuleAutoTool
import net.ccbluex.liquidbounce.features.module.modules.world.packetmine.MineTarget
import net.ccbluex.liquidbounce.features.module.modules.world.packetmine.ModulePacketMine
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.client.world
import net.ccbluex.liquidbounce.utils.item.getEnchantment
import net.ccbluex.liquidbounce.utils.math.sq
import net.minecraft.core.BlockPos
import net.minecraft.tags.FluidTags
import net.minecraft.world.effect.MobEffectUtil
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.enchantment.Enchantments
import net.minecraft.world.level.block.state.BlockState

/**
 * Determines when to switch to a tool and calculates the breaking process delta.
 */
@Suppress("unused")
abstract class MineToolMode(
    choiceName: String,
    val syncOnStart: Boolean = false,
    private val switchesNever: Boolean = false
) : Mode(choiceName), MinecraftShortcuts {

    abstract fun shouldSwitch(mineTarget: MineTarget): Boolean

    open fun getSwitchingMethod() = SwitchMethod.NORMAL

    fun getBlockBreakingDelta(pos: BlockPos, state: BlockState, itemStack: ItemStack?): Float {
        if (switchesNever || itemStack == null) {
            return state.getDestroyProgress(player, world, pos)
        }

        return calcBlockBreakingDelta(pos, state, itemStack)
    }

    fun getSlot(state: BlockState): IntObjectImmutablePair<ItemStack>? {
        if (switchesNever) {
            return null
        }

        return ModuleAutoTool.toolSelector.activeMode.getTool(state)?.let {
            IntObjectImmutablePair(it.hotbarSlot, it.itemStack)
        }
    }

    override val parent: ModeValueGroup<*>
        get() = ModulePacketMine.switchMode

}

/* tweaked minecraft code start */

/**
 * @see net.minecraft.world.level.block.state.BlockBehaviour.getDestroyProgress
 */
private fun calcBlockBreakingDelta(pos: BlockPos, state: BlockState, stack: ItemStack): Float {
    val hardness = state.getDestroySpeed(world, pos)
    if (hardness == -1f) {
        return 0f
    }

    val suitableMultiplier = if (!state.requiresCorrectToolForDrops() || stack.isCorrectToolForDrops(state)) 30 else 100
    return getBlockBreakingSpeed(state, stack) / hardness / suitableMultiplier
}

private fun getBlockBreakingSpeed(state: BlockState, stack: ItemStack): Float {
    var speed = stack.getDestroySpeed(state)

    val enchantmentLevel = stack.getEnchantment(Enchantments.EFFICIENCY)
    if (speed > 1f && enchantmentLevel != 0) {
        /**
         * See: [Attributes.MINING_EFFICIENCY]
         */
        val enchantmentAddition = enchantmentLevel.sq() + 1f
        speed += enchantmentAddition.coerceIn(0f..1024f)
    }

    if (MobEffectUtil.hasDigSpeed(player)) {
        speed *= 1f + (MobEffectUtil.getDigSpeedAmplification(player) + 1).toFloat() * 0.2f
    }

    if (player.hasEffect(MobEffects.MINING_FATIGUE)) {
        val miningFatigueMultiplier = when (player.getEffect(MobEffects.MINING_FATIGUE)!!.amplifier) {
            0 -> 0.3f
            1 -> 0.09f
            2 -> 0.0027f
            3 -> 8.1E-4f
            else -> 8.1E-4f
        }

        speed *= miningFatigueMultiplier
    }

    speed *= player.getAttributeValue(Attributes.BLOCK_BREAK_SPEED).toFloat()
    if (player.isEyeInFluid(FluidTags.WATER)) {
        speed *= player.getAttribute(Attributes.SUBMERGED_MINING_SPEED)!!.value.toFloat()
    }

    if (!player.onGround()) {
        speed /= 5f
    }

    return speed
}

/* tweaked minecraft code end */
