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
package net.ccbluex.liquidbounce.features.module.modules.combat

import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet
import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.config.types.nesting.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.SprintEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.event.tickUntil
import net.ccbluex.liquidbounce.event.waitTicks
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.modules.combat.criticals.ModuleCriticals.CriticalsSelectionMode
import net.ccbluex.liquidbounce.utils.clicking.Clicker
import net.ccbluex.liquidbounce.utils.combat.shouldBeAttacked
import net.ccbluex.liquidbounce.utils.input.InputTracker.isPressedOnAny
import net.ccbluex.liquidbounce.utils.item.isAxe
import net.ccbluex.liquidbounce.utils.item.isSword
import net.minecraft.block.DoorBlock
import net.minecraft.block.FenceGateBlock
import net.minecraft.block.TrapdoorBlock
import net.minecraft.client.option.KeyBinding
import net.minecraft.entity.Entity
import net.minecraft.item.BlockItem
import net.minecraft.item.Items
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket
import net.minecraft.registry.Registries
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.hit.EntityHitResult

/**
 * AutoClicker module
 *
 * Clicks automatically when holding down a mouse button.
 */

object ModuleAutoClicker : ClientModule("AutoClicker", Category.COMBAT, aliases = listOf("TriggerBot")) {

    object AttackButton : ToggleableConfigurable(this, "Attack", true) {

        val clicker = tree(Clicker(this, mc.options.attackKey))

        internal val requiresNoInput by boolean("RequiresNoInput", false)
        internal val delayOnBroken by boolean("DelayOnBroken", true)
        private val objectiveType by enumChoice("Objective", ObjectiveType.ANY)
        private val onItemUse by enumChoice("OnItemUse", Use.WAIT)
        private val weapon by enumChoice("Weapon", Weapon.ANY)
        private val criticalsSelectionMode by enumChoice("Criticals", CriticalsSelectionMode.SMART)
        private val delayPostStopUse by int("DelayPostStopUse", 0, 0..20, "ticks")

        private enum class ObjectiveType(override val choiceName: String) : NamedChoice {
            ENEMY("Enemy"),
            ENTITY("Entity"),
            BLOCK("Block"),
            ANY("Any")
        }

        private enum class Weapon(override val choiceName: String) : NamedChoice {
            SWORD("Sword"),
            AXE("Axe"),
            BOTH("Both"),
            ANY("Any")
        }

        private enum class Use(override val choiceName: String) : NamedChoice {
            WAIT("Wait"),
            STOP("Stop"),
            IGNORE("Ignore")
        }

        fun isOnObjective(): Boolean {
            val crosshair = mc.crosshairTarget

            return when (objectiveType) {
                ObjectiveType.ENEMY -> crosshair is EntityHitResult && crosshair.entity.shouldBeAttacked()
                ObjectiveType.ENTITY -> crosshair is EntityHitResult
                ObjectiveType.BLOCK -> crosshair is BlockHitResult
                ObjectiveType.ANY -> true
            }
        }

        fun isWeaponSelected(): Boolean {
            val stack = player.mainHandStack

            return when (weapon) {
                Weapon.SWORD -> stack.isSword
                Weapon.AXE -> stack.isAxe
                Weapon.BOTH -> stack.isSword || stack.isAxe
                Weapon.ANY -> true
            }
        }

        fun isCriticalHit(entity: Entity): Boolean {
            return criticalsSelectionMode.isCriticalHit(entity)
        }

        suspend fun encounterItemUse(): Boolean {
            return when (onItemUse) {
                Use.WAIT -> {
                    tickUntil { !player.isUsingItem }
                    waitTicks(delayPostStopUse)
                    true
                }

                Use.STOP -> {
                    interaction.stopUsingItem(player)
                    waitTicks(delayPostStopUse)
                    true
                }

                Use.IGNORE -> false
            }
        }

        @Suppress("unused")
        private val sprintHandler = handler<SprintEvent> { event ->
            if (event.source == SprintEvent.Source.MOVEMENT_TICK || event.source == SprintEvent.Source.INPUT) {
                if (!attack || !isOnObjective() || !isWeaponSelected()) {
                    return@handler
                }

                val target = mc.crosshairTarget as? EntityHitResult ?: return@handler
                if (criticalsSelectionMode.shouldStopSprinting(clicker, target.entity)) {
                    event.sprint = false
                }
            }
        }

    }

    object UseButton : ToggleableConfigurable(this, "Use", false) {
        val clicker = tree(Clicker(this, mc.options.useKey, null))
        internal val holdingItemsForIgnore by items(
            "HoldingItemsForIgnore",
            default = ReferenceOpenHashSet.of(
                Items.WATER_BUCKET,
                Items.LAVA_BUCKET,
                Items.ENDER_PEARL,
                Items.ENDER_EYE,
                Items.PLAYER_HEAD,
            ),
        )
        internal val blocksForIgnore by blocks(
            "BlocksForIgnore",
            default = Registries.BLOCK.filterTo(ReferenceOpenHashSet()) {
                it is DoorBlock || it is FenceGateBlock || it is TrapdoorBlock
            },
        )
        internal val delayStart by boolean("DelayStart", false)
        internal val onlyBlock by boolean("OnlyBlock", false)
        internal val requiresNoInput by boolean("RequiresNoInput", false)

        internal var needToWait = true
    }

    private val SPECIAL_ITEMS_FOR_IGNORE = ReferenceOpenHashSet.of(
        Items.RED_BED,
        Items.PLAYER_HEAD,
        Items.COMPASS,
        Items.EMERALD,
        Items.LAPIS_LAZULI,
        Items.GREEN_DYE,
        Items.GRAY_DYE,
        Items.PINK_DYE,
        Items.SLIME_BALL,
    )

    init {
        tree(AttackButton)
        tree(UseButton)
    }

    val attack: Boolean
        get() = mc.options.attackKey.isPressedOnAny || AttackButton.requiresNoInput

    val use: Boolean
        get() = mc.options.useKey.isPressedOnAny || UseButton.requiresNoInput

    @Volatile
    private var lastFinishBreak = 0L

    @Suppress("unused")
    private val packetHandler = handler<PacketEvent> { event ->
        val packet = event.packet
        if (packet is PlayerActionC2SPacket && packet.action == PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK) {
            lastFinishBreak = System.currentTimeMillis()
        }
    }

    @Suppress("unused")
    private val tickHandler = tickHandler {
        AttackButton.run {
            if (!enabled || !attack || !isWeaponSelected() || !isOnObjective()) {
                return@run
            }

            // Check if the player is breaking a block, if so, return
            if (interaction.isBreakingBlock) {
                return@run
            }

            if ((System.currentTimeMillis() - lastFinishBreak < 300L) && delayOnBroken) {
                return@run
            }

            val crosshairTarget = mc.crosshairTarget
            if (crosshairTarget is EntityHitResult) {
                ModuleAutoWeapon.onTarget(crosshairTarget.entity)

                if (!isCriticalHit(crosshairTarget.entity)) {
                    return@run
                }
            }

            if (player.usingItem) {
                val encounterItemUse = encounterItemUse()

                if (encounterItemUse) {
                    return@tickHandler
                }
            }

            clicker.click {
                KeyBinding.onKeyPressed(mc.options.attackKey.boundKey)
                true
            }
        }

        UseButton.run {
            if (!enabled) return@run

            if (!use) {
                needToWait = true
                return@run
            }

            val mainHandStack = player.mainHandStack
            val offHandStack = player.offHandStack
            if (mainHandStack.item in SPECIAL_ITEMS_FOR_IGNORE && mainHandStack.customName != null) {
                return@run
            }

            if (mainHandStack.item in holdingItemsForIgnore || offHandStack.item in holdingItemsForIgnore) {
                return@run
            }

            if (onlyBlock && mainHandStack.item !is BlockItem && offHandStack.item !is BlockItem) {
                return@run
            }

            val crosshairTarget = mc.crosshairTarget
            if (crosshairTarget is BlockHitResult) {
                val blockState = mc.world?.getBlockState(crosshairTarget.blockPos)
                if (blockState?.block in blocksForIgnore) {
                    return@run
                }
            }

            if (delayStart && needToWait) {
                needToWait = false
                waitTicks(2)
                return@run
            }

            clicker.click {
                KeyBinding.onKeyPressed(mc.options.useKey.boundKey)
                true
            }
        }
    }
}
