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
package net.ccbluex.liquidbounce.features.module.modules.combat.killaura.features

import net.ccbluex.liquidbounce.config.types.NoneChoice
import net.ccbluex.liquidbounce.config.types.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.Sequence
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.ModuleKillAura
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.ModuleKillAura.KillAuraClicker.considerMissCooldown
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.ModuleKillAura.prepareAttackEnvironment
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.features.KillAuraNotifyWhenFail.Box
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.features.KillAuraNotifyWhenFail.Sound
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.clicking.Clicker
import net.ccbluex.liquidbounce.utils.combat.findEnemy
import net.ccbluex.liquidbounce.utils.entity.boxedDistanceTo
import net.ccbluex.liquidbounce.utils.inventory.InventoryManager
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen
import net.minecraft.entity.Entity
import net.minecraft.util.Hand
import net.minecraft.util.hit.HitResult

internal object KillAuraFailSwing : ToggleableConfigurable(ModuleKillAura, "FailSwing", false) {

    /**
     * Additional range for fail swing to work
     */
    private val additionalRange by float("AdditionalRange", 2f, 0f..10f)
    val clicker = tree(Clicker(this, false))
    val mode = choices(this, "NotifyWhenFail", activeIndex = 1) {
        arrayOf(NoneChoice(it), Box, Sound)
    }.apply {
        doNotIncludeAlways()
    }

    suspend fun Sequence.dealWithFakeSwing(target: Entity?) {
        if (!enabled) {
            return
        }

        if (considerMissCooldown && mc.attackCooldown > 0) {
            return
        }

        val isInInventoryScreen =
            InventoryManager.isInventoryOpen || mc.currentScreen is GenericContainerScreen

        if (isInInventoryScreen && !ModuleKillAura.ignoreOpenInventory && !ModuleKillAura.simulateInventoryClosing) {
            return
        }

        val raycastType = mc.crosshairTarget?.type

        val range = ModuleKillAura.range + additionalRange
        val entity = target ?: world.findEnemy(0f..range) ?: return

        if (entity.isRemoved || entity.boxedDistanceTo(player) > range || raycastType != HitResult.Type.MISS) {
            return
        }

        // Make it seem like we are blocking
        KillAuraAutoBlock.makeSeemBlock()

        if (clicker.isGoingToClick) {
            prepareAttackEnvironment {
                clicker.clicks {
                    if (considerMissCooldown && mc.attackCooldown > 0) {
                        return@clicks false
                    }

                    player.swingHand(Hand.MAIN_HAND)

                    // Notify the user about the failed hit
                    KillAuraNotifyWhenFail.notifyForFailedHit(entity, RotationManager.serverRotation)
                    true
                }
            }
        }
    }

}
