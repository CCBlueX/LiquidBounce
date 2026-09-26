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

import net.ccbluex.liquidbounce.config.types.group.Mode
import net.ccbluex.liquidbounce.config.types.group.ModeValueGroup
import net.ccbluex.liquidbounce.features.module.MinecraftShortcuts
import net.ccbluex.liquidbounce.features.module.modules.world.ModuleAutoTool
import net.ccbluex.liquidbounce.features.module.modules.world.packetmine.MineTarget
import net.ccbluex.liquidbounce.features.module.modules.world.packetmine.ModulePacketMine
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.client.world
import net.ccbluex.liquidbounce.utils.inventory.HotbarItemSlot
import net.minecraft.core.BlockPos
import net.minecraft.tags.FluidTags
import net.minecraft.world.effect.MobEffectUtil
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.state.BlockState

/**
 * Determines when to switch to a tool and calculates the breaking process delta.
 */
sealed class MineToolMode(
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

        return getDestroyProgress(pos, state, itemStack)
    }

    fun getSlot(state: BlockState): HotbarItemSlot? {
        if (switchesNever) {
            return null
        }

        return ModuleAutoTool.toolSelector.activeMode.getTool(state)
    }

    final override val parent: ModeValueGroup<*>
        get() = ModulePacketMine.switchMode

}

/* tweaked minecraft code start */

/**
 * @see net.minecraft.world.level.block.state.BlockBehaviour.getDestroyProgress
 */
private fun getDestroyProgress(pos: BlockPos, state: BlockState, stack: ItemStack): Float {
    val hardness = state.getDestroySpeed(world, pos)
    if (hardness == -1f) {
        return 0f
    }

    val suitableMultiplier = if (!state.requiresCorrectToolForDrops() || stack.isCorrectToolForDrops(state)) 30 else 100
    return getDestroySpeed(player, state, stack) / hardness / suitableMultiplier
}

/**
 * @see net.minecraft.world.entity.player.Player.getDestroySpeed
 */
private fun getDestroySpeed(player: Player, state: BlockState, stack: ItemStack): Float {
    var speed = stack.getDestroySpeed(state)

    if (speed > 1f) {
        speed += player.getAttributeValue(Attributes.MINING_EFFICIENCY).toFloat()
    }

    if (MobEffectUtil.hasDigSpeed(player)) {
        speed *= 1f + (MobEffectUtil.getDigSpeedAmplification(player) + 1).toFloat() * 0.2f
    }

    if (player.hasEffect(MobEffects.MINING_FATIGUE)) {
        val miningFatigueMultiplier = when (player.getEffect(MobEffects.MINING_FATIGUE)!!.amplifier) {
            0 -> 0.3f
            1 -> 0.09f
            2 -> 0.0027f
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
