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

package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.features.module.modules.combat.criticals.ModuleCriticals.CriticalsSelectionMode
import net.ccbluex.liquidbounce.utils.clicking.Clicker
import net.ccbluex.liquidbounce.utils.combat.shouldBeAttacked
import net.minecraft.client.KeyMapping
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.phys.EntityHitResult

object ModuleAutoTrigger : ClientModule(
    "AutoTrigger",
    ModuleCategories.COMBAT,
    aliases = listOf("TriggerBot")
) {

    private val clicker =
        Clicker(this, mc.options.keyAttack)

    private object Critical : ToggleableValueGroup(
        this,
        "Critical",
        true
    ) {

        val selectionMode by enumChoice(
            "Mode",
            CriticalsSelectionMode.SMART
        )

    }

    private object PostTrigger :
        ToggleableValueGroup(
            this,
            "PostTrigger",
            true
        ) {

        val trackingTicks by int(
            "TrackingTicks",
            2,
            1..10,
            "ticks"
        )

        val triggerWindowTicks by int(
            "TriggerWindowTicks",
            3,
            1..10,
            "ticks"
        )
    }

    private val hitRate by int(
        "HitRate",
        70,
        0..100,
        "%"
    )

    init {
        treeAll(Critical)
        treeAll(PostTrigger)
    }

    private var lastCooldownProgress = 1.0f
    private var postTriggerConsumed = false
    private var consecutiveTargetTicks = 0
    private var trackedTargetInWindow = false

    private val tickHandler = tickHandler {

        val player = mc.player ?: return@tickHandler

        if (mc.screen is AbstractContainerScreen<*>) {
            return@tickHandler
        }

        if (interaction.isDestroying || player.isUsingItem) {
            return@tickHandler
        }

        val hit = mc.hitResult as? EntityHitResult

        val entity = hit?.entity as? LivingEntity

        val hasValidTarget = entity?.shouldBeAttacked() == true

        consecutiveTargetTicks =
            if (hasValidTarget)
                consecutiveTargetTicks + 1
            else
                0

        val cooldownProgress =
            player.attackStrengthTicker
                .toFloat() /
                player.currentItemAttackStrengthDelay

        if (cooldownProgress < lastCooldownProgress) {
            postTriggerConsumed = false
            trackedTargetInWindow = false
        }

        lastCooldownProgress = cooldownProgress

        val precisionTicks = 1 + ((hitRate - 50)
                    .coerceAtLeast(0) / 10)

        val canDirectTrigger =
            hasValidTarget &&
                consecutiveTargetTicks >=
                precisionTicks

        val cooldownRemainingTicks = player.attackStrengthTicker - player.currentItemAttackStrengthDelay

        val inPostTriggerWindow =
            cooldownRemainingTicks >= 0 &&
                cooldownRemainingTicks <=
                PostTrigger.triggerWindowTicks

        val wasTrackingTarget =
            consecutiveTargetTicks >=
                PostTrigger.trackingTicks

        if (inPostTriggerWindow && wasTrackingTarget) {
            trackedTargetInWindow = true
        }

        val cooldownJustReady =
            cooldownRemainingTicks <= 0

        val canPostTrigger =
            PostTrigger.enabled &&
                cooldownJustReady &&
                !hasValidTarget &&
                trackedTargetInWindow &&
                !postTriggerConsumed

        clicker.click {

            if (!canDirectTrigger && !canPostTrigger) {
                return@click false
            }

            if (Critical.enabled && entity != null) {
                val isCritical =
                    Critical.selectionMode
                        .isCriticalHit(entity)

                if (!isCritical) {
                    return@click false
                }

            }

            KeyMapping.click(
                mc.options.keyAttack.key
            )

            if (canPostTrigger) {
                postTriggerConsumed = true
            }
            true
        }
    }
}
