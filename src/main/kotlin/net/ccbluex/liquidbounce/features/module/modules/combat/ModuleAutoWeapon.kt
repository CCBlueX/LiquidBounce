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

import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.event.events.AttackEntityEvent
import net.ccbluex.liquidbounce.event.sequenceHandler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.modules.combat.ModuleAutoWeapon.againstShield
import net.ccbluex.liquidbounce.features.module.modules.combat.ModuleAutoWeapon.prepare
import net.ccbluex.liquidbounce.features.module.modules.player.autobuff.ModuleAutoBuff
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.ItemCategorization
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.items.WeaponItemFacet
import net.ccbluex.liquidbounce.utils.client.SilentHotbar
import net.ccbluex.liquidbounce.utils.client.isOlderThanOrEqual1_8
import net.ccbluex.liquidbounce.utils.inventory.HotbarItemSlot
import net.ccbluex.liquidbounce.utils.inventory.Slots
import net.ccbluex.liquidbounce.utils.item.getEnchantment
import net.ccbluex.liquidbounce.utils.item.isConsumable
import net.minecraft.enchantment.Enchantments
import net.minecraft.entity.Entity
import net.minecraft.entity.LivingEntity
import net.minecraft.item.AxeItem
import net.minecraft.item.MaceItem
import net.minecraft.item.SwordItem
import net.minecraft.util.Hand

/**
 * AutoWeapon module
 *
 * Automatically selects the best weapon in your hotbar
 */
object ModuleAutoWeapon : ClientModule("AutoWeapon", Category.COMBAT) {

    /**
     * The weapon type to prefer, which is on 1.8 and 1.9+ versions usually a sword,
     * due to the attack speed.
     *
     * On 1.9+ we are likely to prefer an axe when the target is blocking with a shield,
     * which is covered by the [againstShield] weapon type.
     */
    private val preferredWeapon by enumChoice("Preferred", WeaponType.SWORD)
    private val againstShield by enumChoice("BlockedByShield", WeaponType.AXE)

    @Suppress("unused")
    private enum class WeaponType(
        override val choiceName: String,
        val filter: (WeaponItemFacet) -> Boolean
    ): NamedChoice {
        ANY("Any", { true }),
        SWORD("Sword", { it.itemStack.item is SwordItem }),
        AXE("Axe", { it.itemStack.item is AxeItem }),
        MACE("Mace", { it.itemStack.item is MaceItem }),

        /**
         * Do not prefer any weapon type, this is useful to only
         * use the [againstShield] weapon type.
         */
        NONE("None", { false });
    }

    private val prepare by boolean("Prepare", true)

    /**
     * In Minecraft, even when we send a packet to switch the slot,
     * before we attack, the server will still calculate damage
     * based on the slot we were on before the switch.
     *
     * This is why we have to wait at least 1 tick before attacking
     * after switching the slot.
     *
     * This is not necessary when we are already on the correct slot,
     * which should be the case when using [prepare].
     */
    private val switchOn by int("SwitchOn", 1, 0..2, "ticks")
    private val switchBack by int("SwitchBack", 20, 1..300, "ticks")

    /**
     * Prioritize Auto Buff or consuming an item over Auto Weapon
     */
    private val isBusy: Boolean
        get() = SilentHotbar.isSlotModifiedBy(ModuleAutoBuff) || player.isUsingItem && player.activeHand ==
            Hand.MAIN_HAND && player.activeItem.isConsumable

    /**
     * Check if the attack will break the shield
     */
    fun willBreakShield(): Boolean {
        if (!this.running || isOlderThanOrEqual1_8) {
            return false
        }

        // If we have an axe in our main hand, we will break the shield
        if (player.mainHandStack.item is AxeItem) {
            return true
        }

        // If we are not going to switch to an axe, we will not break the shield
        return determineWeaponSlot(null, enforceShield = true)?.itemStack?.item is AxeItem
    }

    @Suppress("unused")
    private val attackHandler = sequenceHandler<AttackEntityEvent> { event ->
        val entity = event.entity as? LivingEntity ?: return@sequenceHandler
        val weaponSlot = determineWeaponSlot(entity)?.hotbarSlot ?: return@sequenceHandler
        val isOnSwitch = SilentHotbar.serversideSlot != weaponSlot

        if (isBusy) {
            return@sequenceHandler
        }

        SilentHotbar.selectSlotSilently(
            this,
            weaponSlot,
            switchBack
        )

        // Sync selected slot right now,
        // we will not sync on this tick otherwise
        interaction.syncSelectedSlot()

        if (isOnSwitch && switchOn > 0) {
            event.cancelEvent()

            // Re-attack after switch
            // This should not end up in a recursive loop,
            // because we switched slot by now
            waitTicks(switchOn)
            event.caller()
        }
    }

    /**
     * Prepare AutoWeapon for given [entity] if [prepare] is enabled
     */
    fun prepare(entity: Entity?) {
        if (!running || !prepare || entity !is LivingEntity || isBusy) {
            return
        }

        determineWeaponSlot(entity)?.let { slot ->
            SilentHotbar.selectSlotSilently(
                this,
                slot.hotbarSlot,
                switchBack
            )
        }
    }

    private fun determineWeaponSlot(target: LivingEntity?, enforceShield: Boolean = false): HotbarItemSlot? {
        val itemCategorization = ItemCategorization(Slots.Hotbar)
        val blockedByShield = enforceShield || !isOlderThanOrEqual1_8 &&
            target?.blockedByShield(world.damageSources.playerAttack(player)) == true

        val bestSlot = Slots.Hotbar
            .flatMap { slot -> itemCategorization.getItemFacets(slot).filterIsInstance<WeaponItemFacet>() }
            .filter(
                when {
                    blockedByShield -> againstShield.filter
                    else -> preferredWeapon.filter
                }
            )
            .maxOrNull()

        return bestSlot?.itemSlot as HotbarItemSlot?
    }

}
