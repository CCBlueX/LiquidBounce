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
package net.ccbluex.liquidbounce.features.module.modules.player

import net.ccbluex.liquidbounce.config.types.group.Mode
import net.ccbluex.liquidbounce.config.types.group.ModeValueGroup
import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
import net.ccbluex.liquidbounce.event.tickConditional
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.event.tickUntil
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.features.module.modules.player.ModuleFastExp.NoWaste.maxDurabilityToContinueRepair
import net.ccbluex.liquidbounce.features.module.modules.player.ModuleFastExp.NoWaste.minDurabilityToStartRepair
import net.ccbluex.liquidbounce.injection.mixins.minecraft.entity.MixinExperienceOrbAccessor
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.RotationsValueGroup
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.combat.CombatManager
import net.ccbluex.liquidbounce.utils.input.InputBind
import net.ccbluex.liquidbounce.utils.inventory.HotbarItemSlot
import net.ccbluex.liquidbounce.utils.inventory.InventoryManager
import net.ccbluex.liquidbounce.utils.inventory.OffHandSlot
import net.ccbluex.liquidbounce.utils.inventory.Slots
import net.ccbluex.liquidbounce.utils.inventory.useHotbarSlotOrOffhand
import net.ccbluex.liquidbounce.utils.item.durability
import net.ccbluex.liquidbounce.utils.item.getEnchantment
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.ccbluex.liquidbounce.utils.kotlin.random
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.item.Items
import net.minecraft.world.item.enchantment.Enchantments

/**
 * FastExp module
 *
 * Automatically repairs your armor.
 */
object ModuleFastExp : ClientModule(
    "FastExp",
    ModuleCategories.PLAYER,
    bindAction = InputBind.BindAction.HOLD,
    disableOnQuit = true
) {
    /**
     * Experience in the orb is deducted by one point for every two durability points repaired.
     * For example, one durability point repaired doesn't take any experience away from the orb.
     *
     * @see <a href=https://minecraft.fandom.com/wiki/Mending#Usage>Mending - Usage</a>
     */
    private const val REPAIR_RATE = 2

    /**
     * A bottle o' enchanting drops experience orbs worth 3–11 points (average 7.0).
     *
     * @see <a href=https://minecraft.fandom.com/wiki/Bottle_o%27_Enchanting#Usage>Bottle o' Enchanting - Usage</a>
     */
    private const val EXPERIENCE_PER_BOTTLE = 7

    private object Rotate : ToggleableValueGroup(this, "Rotate", true) {
        val rotations = tree(RotationsValueGroup(this))
    }

    private object NoWaste : ToggleableValueGroup(this, "NoWaste", true) {
        /**
         * If at least one of the items to repair has durability lower than or equal to [minDurabilityToStartRepair],
         * the module will start throwing experience bottles.
         */
        val minDurabilityToStartRepair by int("MinDurabilityToStartRepair", 64, 0..2048)

        /**
         * If some experience orbs have been destroyed by lava/fire,
         * or the player has switched to items with greater total damage,
         * or some experience orbs haven't reached the player due to movement, etc.,
         *
         * then the module can start throwing experience bottles again to complete the repair process,
         * but only if there is still at least one item
         * whose durability is lower than or equal to [maxDurabilityToContinueRepair]%.
         *
         * This should prevent the module from repairing armor after every 2 or 3 received hits.
         */
        val maxDurabilityToContinueRepair by int("MaxDurabilityToContinueRepair", 85, 1..100, "%")
    }

    init {
        tree(Rotate)
        tree(NoWaste)
    }

    private val throwMode = modes(this,
        "ThrowMode",
        Normal ,
        arrayOf(Normal, Fast)
    )

    private sealed class ThrowMode(name: String) : Mode(name) {
        final override val parent: ModeValueGroup<ThrowMode>
            get() = throwMode

        abstract fun nextTickItems(): Float
    }

    private object Normal : ThrowMode("Normal") {
        private val ticksPerItem by floatRange("TicksPerItem", 2f..3f, 0.5f..10f, "ticks")
        override fun nextTickItems(): Float = 1f / ticksPerItem.random()
    }

    private object Fast : ThrowMode("Fast") {
        private val itemsPerTick by floatRange("ItemsPerTick", 3f..5f, 0.5f..16f)
        override fun nextTickItems(): Float = itemsPerTick.random()
    }

    private val combatPauseTime by int("CombatPauseTime", 0, 0..40, "ticks")
    private val slotResetDelay by intRange("SlotResetDelay", 0..0, 0..40, "ticks")

    private var bottlesRequired = 0
    private var bottlesUsed = 0
    private var itemsToThrow = 0f
    private var repairing = false

    override fun onDisabled() {
        bottlesUsed = 0
        bottlesRequired = 0
        itemsToThrow = 0f
        repairing = false
        super.onDisabled()
    }

    @Suppress("unused")
    private val repeatable = tickHandler {
        if (InventoryManager.isHandledScreenOpen) {
            // doesn't throw exp bottles when an inventory is open
            // yet doesn't stop the repairing process either
            // allowing to refill the hotbar with exp bottles, if needed.
            return@tickHandler
        }

        val slot = Slots.OffhandWithHotbar.findSlot(Items.EXPERIENCE_BOTTLE)
        if (slot == null || player.isDeadOrDying) {
            bottlesUsed = 0
            bottlesRequired = 0
            repairing = false
            return@tickHandler
        }

        if (!NoWaste.enabled) {
            action(slot)
            return@tickHandler
        }

        val bottlesRequiredCurrently = getRequiredExperienceBottleCount(slot)

        if (bottlesRequiredCurrently < 1) {
            bottlesUsed = 0
            bottlesRequired = 0
            return@tickHandler
        }

        // waits for the orbs, in case there are any, to get absorbed before repairing items
        tickUntil {
            repairing || !anyExpOrbMovingToPlayer()
        }

        repairing = true
        bottlesRequired = bottlesRequired.coerceAtLeast(bottlesRequiredCurrently)

        action(slot)

        // after all experience bottles have been thrown
        if (bottlesUsed > 0 && bottlesUsed >= bottlesRequired) {
            waitForExperienceOrbs()
            bottlesUsed = 0
            bottlesRequired = 0
        }
    }

    private fun anyExpOrbMovingToPlayer(): Boolean =
        world.entitiesForRendering().any {
            (it is MixinExperienceOrbAccessor) && it.followingPlayer === player
                && it.deltaMovement.lengthSqr() > player.deltaMovement.lengthSqr()
        }

    private suspend fun waitForExperienceOrbs() {
        // waits for experience orbs to appear
        // waits up to 10 ticks because the orbs might get absorbed instantly if they come only from a very few bottles
        tickConditional(10, ::anyExpOrbMovingToPlayer)

        // waits for the orbs to get absorbed
        tickUntil {
            !anyExpOrbMovingToPlayer()
        }
    }

    private suspend fun action(slot: HotbarItemSlot) {
        CombatManager.pauseCombatForAtLeast(combatPauseTime)

        if (Rotate.enabled) {
            tickUntil {
                val rotation = Rotation(player.yRot, 90f)
                RotationManager.setRotationTarget(
                    Rotate.rotations.toRotationTarget(rotation),
                    Priority.IMPORTANT_FOR_USAGE_3,
                    this@ModuleFastExp
                )
                RotationManager.serverRotation.pitch > 85f
            }
        }

        itemsToThrow += throwMode.activeMode.nextTickItems()
        val times = itemsToThrow.toInt()
        itemsToThrow -= times

        val pitch = if (Rotate.enabled) RotationManager.serverRotation.pitch else 90f
        repeat(times) {
            useHotbarSlotOrOffhand(
                slot,
                slotResetDelay.random(),
                xRot = pitch
            )
        }

        if (NoWaste.enabled) {
            bottlesUsed += times
        }
    }

    /**
     * Assuming how much one experience bottle can repair,
     * returns the experience bottle count needed to repair
     * all the player's armor and the item in their other hand.
     */
    private fun getRequiredExperienceBottleCount(slot: HotbarItemSlot): Int {
        if (!NoWaste.enabled) {
            return Int.MAX_VALUE
        }

        val itemsToRepair = arrayOf(
            player.getItemBySlot(EquipmentSlot.HEAD),
            player.getItemBySlot(EquipmentSlot.CHEST),
            player.getItemBySlot(EquipmentSlot.LEGS),
            player.getItemBySlot(EquipmentSlot.FEET),
            // an item in the other hand, not holding the exp bottle could also get repaired
            if (slot == OffHandSlot) {
                player.getItemBySlot(EquipmentSlot.MAINHAND)
            } else {
                player.getItemBySlot(EquipmentSlot.OFFHAND)
            },
        ).filter { it.getEnchantment(Enchantments.MENDING) != 0 }

        // doesn't let the module start repairing the items again
        // when the items have been repaired but not fully and are missing a just few more exp bottle
        if (bottlesUsed == 0 && repairing
            && itemsToRepair.none { 100f * it.durability / it.maxDamage <= maxDurabilityToContinueRepair } ) {
            repairing = false
            return 0
        }

        // doesn't let the module start repairing items if the durability threshold hasn't been reached
        if (bottlesUsed == 0 && !repairing
            && itemsToRepair.none { it.durability <= minDurabilityToStartRepair }) {
            return 0
        }

        val totalDamage = itemsToRepair.sumOf { it.damageValue }
        val experienceRequired = totalDamage / REPAIR_RATE
        val bottlesRequired = experienceRequired / EXPERIENCE_PER_BOTTLE

        return bottlesRequired.coerceAtMost(slot.itemStack.count)
    }
}
