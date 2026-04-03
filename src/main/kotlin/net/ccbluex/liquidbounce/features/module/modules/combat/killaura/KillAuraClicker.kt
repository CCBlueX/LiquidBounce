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
package net.ccbluex.liquidbounce.features.module.modules.combat.killaura

import net.ccbluex.liquidbounce.features.module.modules.combat.ModuleAutoWeapon
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.KillAuraRotationsValueGroup.rotationTiming
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.ModuleKillAura.simulateInventoryClosing
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.features.KillAuraAutoBlock
import net.ccbluex.liquidbounce.features.module.modules.combat.velocity.mode.VelocityReduce
import net.ccbluex.liquidbounce.features.module.modules.exploit.ModuleMultiActions
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug.debugGeometry
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug.debugParameter
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.aiming.utils.canSeeBox
import net.ccbluex.liquidbounce.utils.aiming.utils.withFixedYaw
import net.ccbluex.liquidbounce.utils.clicking.Clicker
import net.ccbluex.liquidbounce.utils.clicking.ItemCooldown
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.client.network
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.client.send1_11_1OpenInventory
import net.ccbluex.liquidbounce.utils.client.sendCloseInventory
import net.ccbluex.liquidbounce.utils.entity.PositionExtrapolation
import net.ccbluex.liquidbounce.utils.entity.getBoundingBoxAt
import net.ccbluex.liquidbounce.utils.entity.wouldBlockHit
import net.ccbluex.liquidbounce.utils.inventory.InventoryManager
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.PosRot
import kotlin.math.round

object KillAuraClicker : Clicker<ModuleKillAura>(
    ModuleKillAura,
    mc.options.keyAttack,
    KillAuraClickerItemCooldown()
) {

    override val isClickTick: Boolean
        get() = super.isClickTick && (!VelocityReduce.running || VelocityReduce.remainingAttackCount == 0)

    private class KillAuraClickerItemCooldown : ItemCooldown() {

        private val ignoreOnShieldBreak by boolean("IgnoreOnShieldBreak", true)
        private val ignoreOnMaceSmash by boolean("IgnoreOnMaceSmash", true)
        private val ignoreWhenExitingRange by boolean("IgnoreWhenExitingRange", true)

        override fun isCooldownPassed(ticks: Int) = when {
            super.isCooldownPassed(ticks) -> true
            ignoreOnShieldBreak && ModuleKillAura.targetTracker.target?.wouldBlockHit == true
                && ModuleAutoWeapon.willShieldBreak -> true
            ignoreOnMaceSmash && ModuleAutoWeapon.willMaceSmash -> true
            ignoreWhenExitingRange && predictExitingRange(1.0 + ticks.toDouble()) -> true
            else -> false
        }

        /**
         * Predicts if we are going to move out of attack range.
         */
        fun predictExitingRange(ticks: Double): Boolean {
            require(ticks > 0) { "ticks must be positive" }

            val target = KillAuraTargetTracker.target ?: return false
            if (target.hurtTime > 7) {
                return false
            }

            val futurePos = PositionExtrapolation.getBestForEntity(player)
                .getPositionInTicks(ticks)
            val futureTargetPos = PositionExtrapolation.getBestForEntity(target)
                .getPositionInTicks(ticks)

            val ownEyePos = futurePos.add(0.0, player.getEyeHeight(player.pose).toDouble(), 0.0)
            val targetBox = target.getBoundingBoxAt(futureTargetPos)

            val isExitingRange = !canSeeBox(
                eyes = ownEyePos,
                box = targetBox,
                range = ModuleKillAura.range.interactionRange.toDouble(),
                wallsRange = ModuleKillAura.range.interactionThroughWallsRange.toDouble()
            )
            debugParameter("Is Exiting Range On ${round(ticks)}") { isExitingRange }
            if (isExitingRange) {
                debugGeometry("Exiting") { ModuleDebug.DebuggedPoint(futurePos, Color4b.RED, 0.4) }
            }

            return isExitingRange
        }

    }

    /**
     * Will prepare us for attacking using the [attack] function.
     *
     * This includes:
     * - Closing the inventory if we are simulating inventory closing
     * - Unblocking if we are blocking and the tick on is 0
     */
    @Suppress("CognitiveComplexMethod")
    fun prepareForAttack(rotation: Rotation? = null, attack: () -> Boolean) {
        if (!canExecuteClickNow()) {
            // If we are not going to click, we don't need to prepare the environment
            return
        }

        // 1. Stop blocking
        if (player.isBlocking) {
            if (!KillAuraAutoBlock.enabled && !ModuleMultiActions.mayAttackWhileUsing()) {
                return
            }

            if (KillAuraAutoBlock.enabled && KillAuraAutoBlock.shouldUnblockToHit) {
                if (KillAuraAutoBlock.stopBlocking(pauses = true) && KillAuraAutoBlock.pauseOnUnblockTicks > 0) {
                    ModuleKillAura.waitTicks = KillAuraAutoBlock.pauseOnUnblockTicks
                    return
                }
            }
        } else if (player.isUsingItem && !ModuleMultiActions.mayAttackWhileUsing()) {
            // Since we are not allowed to attack while the player is using another item,
            // we will return here.
            return
        }

        val wasSimulatedInventoryClose = simulateInventoryClosing && InventoryManager.isInventoryOpen

        // 2. Close Inventory
        if (wasSimulatedInventoryClose) {
            network.sendCloseInventory()
        }

        // 3. Rotate to target (if we have on-tick enabled)
        if (rotationTiming == KillAuraRotationsValueGroup.KillAuraRotationTiming.ON_TICK && rotation != null) {
            network.send(
                PosRot(
                    player.x,
                    player.y,
                    player.z,
                    rotation.yaw,
                    rotation.pitch,
                    player.onGround(),
                    player.horizontalCollision
                )
            )
        }

        // Run the attack
        click(attack)

        // 1. Rotate back
        if (rotationTiming == KillAuraRotationsValueGroup.KillAuraRotationTiming.ON_TICK && rotation != null) {
            network.send(
                PosRot(
                    player.x,
                    player.y,
                    player.z,
                    player.withFixedYaw(rotation),
                    player.xRot,
                    player.onGround(),
                    player.horizontalCollision
                )
            )
        }

        // 2. Start blocking again
        if (KillAuraAutoBlock.blockImmediate) {
            KillAuraAutoBlock.startBlocking()
        }

        // 3. Open inventory again
        if (wasSimulatedInventoryClose) {
            network.send1_11_1OpenInventory()
        }
    }

}
