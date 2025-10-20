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
package net.ccbluex.liquidbounce.features.module.modules.player

import net.ccbluex.liquidbounce.config.types.nesting.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.event.tickUntil
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.modules.player.ModuleFastExp.NoWaste.durabilityThreshold
import net.ccbluex.liquidbounce.injection.mixins.minecraft.entity.MixinExperienceOrbEntityAccessor
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.RotationsConfigurable
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.combat.CombatManager
import net.ccbluex.liquidbounce.utils.input.InputBind
import net.ccbluex.liquidbounce.utils.inventory.*
import net.ccbluex.liquidbounce.utils.item.durability
import net.ccbluex.liquidbounce.utils.item.getEnchantment
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.ccbluex.liquidbounce.utils.kotlin.random
import net.minecraft.enchantment.Enchantments
import net.minecraft.entity.ExperienceOrbEntity
import net.minecraft.item.Items

/**
 * FastExp module
 *
 * Automatically repairs your armor.
 */
object ModuleFastExp : ClientModule(
    "FastExp",
    Category.PLAYER,
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

    private object Rotate : ToggleableConfigurable(this, "Rotate", true) {
        val rotations = tree(RotationsConfigurable(this))
    }

    private object NoWaste : ToggleableConfigurable(this, "NoWaste", true) {
        /**
         * If at least one of items that can be repaired has less durability than [durabilityThreshold],
         * the module will start throwing experience bottles.
         * */
        val durabilityThreshold by int("DurabilityThreshold", 256, 0..2048)
    }

    init {
        tree(Rotate)
        tree(NoWaste)
    }

    private val itemsPerTick by floatRange("ItemsPerTick", 0.5f..2f, 0.1f..32f)

    private val combatPauseTime by int("CombatPauseTime", 0, 0..40, "ticks")
    private val slotResetDelay by intRange("SlotResetDelay", 0..0, 0..40, "ticks")

    private var bottlesRequired = 0
    private var bottlesUsed = 0
    private var itemsToThrow = 0f

    override fun onDisabled() {
        bottlesUsed = 0
        bottlesRequired = 0
        itemsToThrow = 0f
        super.onDisabled()
    }

    @Suppress("unused")
    private val repeatable = tickHandler {
        val slot = Slots.OffhandWithHotbar.findSlot(Items.EXPERIENCE_BOTTLE)
        if (slot == null || player.isDead || InventoryManager.isInventoryOpen) {
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

        bottlesRequired = bottlesRequired.coerceAtLeast(bottlesRequiredCurrently)

        action(slot)

        // after all experience bottles have been thrown
        if (bottlesUsed > 0 && bottlesUsed >= bottlesRequired) {
            waitForExperienceOrbs()
            bottlesUsed = 0
            bottlesRequired = 0
        }
    }

    private suspend fun waitForExperienceOrbs() {
        // waits for experience orbs to appear
        tickUntil {
            world.entities
                .filterIsInstance<ExperienceOrbEntity>()
                .any { (it as MixinExperienceOrbEntityAccessor).target == player
                    && it.velocity.length() > player.velocity.length() }

                // the orbs might get absorbed instantly if they come only from a very few bottles
                || it > 10
        }

        // waits for the orbs to get absorbed
        tickUntil {
            world.entities
                .filterIsInstance<ExperienceOrbEntity>()
                .none { (it as MixinExperienceOrbEntityAccessor).target == player
                    && it.velocity.length() > player.velocity.length()}
        }
    }

    private suspend fun action(slot: HotbarItemSlot) {
        CombatManager.pauseCombatForAtLeast(combatPauseTime)

        if (Rotate.enabled) {
            tickUntil {
                val rotation = Rotation(player.yaw, 90f)
                RotationManager.setRotationTarget(
                    Rotate.rotations.toRotationTarget(rotation),
                    Priority.IMPORTANT_FOR_USAGE_3,
                    this@ModuleFastExp
                )
                RotationManager.serverRotation.pitch > 85f
            }
        }

        itemsToThrow += itemsPerTick.random()
        val times = itemsToThrow.toInt()
        itemsToThrow -= times

        val pitch = if (Rotate.enabled) RotationManager.serverRotation.pitch else 90f
        repeat(times) {
            useHotbarSlotOrOffhand(
                slot,
                slotResetDelay.random(),
                pitch = pitch
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

        // an item in the other hand, not holding the exp bottle could also get repaired
        val otherHandSlot = if (slot == OffHandSlot) {
            player.mainHandStack
        } else {
            player.offHandStack
        }

        val itemsToRepair = (player.inventory.armor + otherHandSlot)
            .filter { it.getEnchantment(Enchantments.MENDING) != 0 }

        // doesn't let the module start repairing items if the durability threshold hasn't been reached
        if (bottlesUsed == 0
            && itemsToRepair.none { it.durability <= durabilityThreshold }) {
            return 0
        }

        val totalDamage = itemsToRepair.sumOf { it.damage }
        val experienceRequired = totalDamage / REPAIR_RATE
        val bottlesRequired = experienceRequired / EXPERIENCE_PER_BOTTLE

        return bottlesRequired
    }
}
