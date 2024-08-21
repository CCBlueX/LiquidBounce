/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2024 CCBlueX
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

import net.ccbluex.liquidbounce.config.Choice
import net.ccbluex.liquidbounce.config.ChoiceConfigurable
import net.ccbluex.liquidbounce.config.NamedChoice
import net.ccbluex.liquidbounce.config.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.events.AttackEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.event.sequenceHandler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.HotbarItemSlot
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.ItemCategorization
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.items.WeaponItemFacet
import net.ccbluex.liquidbounce.utils.client.SilentHotbar
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.combat.findEnemy
import net.ccbluex.liquidbounce.utils.inventory.HOTBAR_SLOTS
import net.minecraft.entity.Entity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.AxeItem
import net.minecraft.item.SwordItem

/**
 * AutoWeapon module
 *
 * Automatically selects the best weapon in your hotbar
 */
object ModuleAutoWeapon : Module("AutoWeapon", Category.COMBAT) {

    private val slotMode = choices("SlotMode", BestSlotMode, arrayOf(
        BestSlotMode,
        ConstantSlotMode
    ))

    private val resetDelay by int("TicksUntilReset", 20, 1..100)
    private val breakShield by boolean("BreakShield", true)

    private object Prepare : ToggleableConfigurable(this, "Prepare", false) {

        private val range by float("Range", 4.2f, 1f..8f)

        @Suppress("unused")
        private val prepareHandler = repeatable {
            val enemy = world.findEnemy(0f..range) ?: return@repeatable

            SilentHotbar.selectSlotSilently(
                this,
                determineWeaponSlot(enemy) ?: return@repeatable,
                resetDelay
            )
        }


    }

    init {
        tree(Prepare)
    }

    private object BestSlotMode : Choice("Best") {

        override val parent: ChoiceConfigurable<*>
            get() = slotMode

        val weaponType by enumChoice("WeaponType", WeaponType.ANY)

        enum class WeaponType(
            override val choiceName: String,
            val filter: (WeaponItemFacet) -> Boolean
        ): NamedChoice {
            ANY("Any", { true }),
            SWORD("Sword", { it.itemStack.item is SwordItem }),
            AXE("Axe", { it.itemStack.item is AxeItem })
        }

    }

    private object ConstantSlotMode : Choice("Constant") {

        override val parent: ChoiceConfigurable<*>
            get() = slotMode

        val slot by int("Slot", 1, 1..9)

    }

    @Suppress("unused")
    val attackHandler = sequenceHandler<AttackEvent> { event ->
        SilentHotbar.selectSlotSilently(
            this,
            determineWeaponSlot(event.enemy) ?: return@sequenceHandler,
            resetDelay
        )
    }

    private fun determineWeaponSlot(target: Entity?): Int? {
        val itemCategorization = ItemCategorization(HOTBAR_SLOTS)
        val itemMap = HOTBAR_SLOTS
            .flatMap { itemCategorization.getItemFacets(it).filterIsInstance<WeaponItemFacet>().toList() }

        // Shield Breaker
        if (breakShield && target is PlayerEntity
            && target.blockedByShield(world.damageSources.playerAttack(player))) {
            val bestAxeSlot = itemMap
                .filter(BestSlotMode.WeaponType.AXE.filter)
                .maxOrNull()

            if (bestAxeSlot != null) {
                return (bestAxeSlot.itemSlot as HotbarItemSlot).hotbarSlot
            }
        }

        return if (BestSlotMode.isActive) {
            val bestSlot = itemMap
                .filter(BestSlotMode.weaponType.filter)
                .maxOrNull()

            (bestSlot?.itemSlot as HotbarItemSlot?)?.hotbarSlot
        } else {
            ConstantSlotMode.slot - 1
        }
    }

}
