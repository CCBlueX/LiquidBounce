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
package net.ccbluex.liquidbounce.features.module.modules.combat.killaura

import net.ccbluex.liquidbounce.event.Sequence
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.KillAuraRotationsConfigurable.rotationTiming
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.ModuleKillAura.simulateInventoryClosing
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.features.KillAuraAutoBlock
import net.ccbluex.liquidbounce.features.module.modules.exploit.ModuleMultiActions
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.aiming.utils.withFixedYaw
import net.ccbluex.liquidbounce.utils.clicking.Clicker
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.client.network
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.entity.isBlockAction
import net.ccbluex.liquidbounce.utils.inventory.InventoryManager
import net.ccbluex.liquidbounce.utils.inventory.openInventorySilently
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.Full

object KillAuraClicker : Clicker<ModuleKillAura>(ModuleKillAura, mc.options.attackKey, true) {

    /**
     * Prepare the environment for attacking an entity
     *
     * This means, we make sure we are not blocking, we are not using another item,
     * and we are not in an inventory screen depending on the configuration.
     */
    suspend fun attack(sequence: Sequence, rotation: Rotation? = null, attack: () -> Boolean) {
        if (!isClickTick) {
            // If we are not going to click, we don't need to prepare the environment
            return
        }

        val interactiveScene = InteractiveScene(sequence = sequence, rotation = rotation)
        if (interactiveScene.prepare()) {
            return
        }

        click(attack)

        interactiveScene.unprepare()
    }

    /**
     * Prepare the scene for e.g. attacking an entity.
     */
    private data class InteractiveScene(
        val sequence: Sequence,
        val rotation: Rotation?,
        val isInInventoryScreen: Boolean = InventoryManager.isInventoryOpen,
    ) {

        @Suppress("CognitiveComplexMethod")
        suspend fun prepare(): Boolean {
            if (simulateInventoryClosing && isInInventoryScreen) {
                network.sendPacket(CloseHandledScreenC2SPacket(0))
            }

            if (player.isBlockAction) {
                if (!KillAuraAutoBlock.enabled && !ModuleMultiActions.mayAttackWhileUsing()) {
                    return true
                }

                if (KillAuraAutoBlock.enabled && KillAuraAutoBlock.shouldUnblockToHit) {
                    // Wait for the tick off time to be over, if it's not 0
                    // Ideally this should not happen.
                    if (KillAuraAutoBlock.stopBlocking(pauses = true) && KillAuraAutoBlock.currentTickOff > 0) {
                        sequence.waitTicks(KillAuraAutoBlock.currentTickOff)
                    }
                }
            } else if (player.isUsingItem && !ModuleMultiActions.mayAttackWhileUsing()) {
                // return if it's not allowed to attack while the player is using another item that's not a shield
                return true
            }

            if (rotationTiming == KillAuraRotationsConfigurable.KillAuraRotationTiming.ON_TICK && rotation != null) {
                network.sendPacket(
                    Full(
                        player.x, player.y, player.z, rotation.yaw, rotation.pitch, player.isOnGround,
                        player.horizontalCollision
                    )
                )
            }
            return false
        }

        fun unprepare() {
            if (rotationTiming == KillAuraRotationsConfigurable.KillAuraRotationTiming.ON_TICK && rotation != null) {
                network.sendPacket(
                    Full(
                        player.x, player.y, player.z, player.withFixedYaw(rotation), player.pitch, player.isOnGround,
                        player.horizontalCollision
                    )
                )
            }

            if (simulateInventoryClosing && isInInventoryScreen) {
                openInventorySilently()
            }

            // If the player was blocking before, we start blocking again after the attack if the tick on is 0
            if (KillAuraAutoBlock.blockImmediate) {
                KillAuraAutoBlock.startBlocking()
            }
        }

    }

}
