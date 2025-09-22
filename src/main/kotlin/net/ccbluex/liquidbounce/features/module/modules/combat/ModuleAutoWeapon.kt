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
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.modules.combat.ModuleAutoWeapon.autoMace
import net.ccbluex.liquidbounce.features.module.modules.combat.ModuleAutoWeapon.autoShieldBreak
import net.ccbluex.liquidbounce.features.module.modules.combat.ModuleAutoWeapon.onTarget
import net.ccbluex.liquidbounce.features.module.modules.player.autobuff.ModuleAutoBuff
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.ItemCategorization
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.items.WeaponItemFacet
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug.debugParameter
import net.ccbluex.liquidbounce.utils.client.SilentHotbar
import net.ccbluex.liquidbounce.utils.client.convertToString
import net.ccbluex.liquidbounce.utils.client.isOlderThanOrEqual1_8
import net.ccbluex.liquidbounce.utils.entity.hasCooldown
import net.ccbluex.liquidbounce.utils.entity.wouldBlockHit
import net.ccbluex.liquidbounce.utils.inventory.HotbarItemSlot
import net.ccbluex.liquidbounce.utils.inventory.Slots
import net.ccbluex.liquidbounce.utils.item.attackSpeed
import net.ccbluex.liquidbounce.utils.item.isAxe
import net.ccbluex.liquidbounce.utils.item.isConsumable
import net.ccbluex.liquidbounce.utils.item.isSword
import net.minecraft.entity.Entity
import net.minecraft.entity.LivingEntity
import net.minecraft.item.MaceItem
import net.minecraft.util.Hand
import java.util.*

/**
 * AutoWeapon module
 *
 * Automatically selects the best weapon in your hotbar
 */
object ModuleAutoWeapon : ClientModule("AutoWeapon", Category.COMBAT) {

    /**
     * The weapon type to prefer, which on 1.8 and 1.9+ versions is usually a sword,
     * due to the attack speed.
     */
    private val preferredWeapon by enumChoice("Preferred", WeaponType.SWORD)

    private val autoShieldBreak by boolean("AutoShieldBreak", true)
    private val autoMace by boolean("AutoMace", true)

    @Suppress("unused")
    private enum class WeaponType(
        override val choiceName: String,
        val filter: (WeaponItemFacet) -> Boolean
    ): NamedChoice {
        ANY("Any", { true }),
        SWORD("Sword", { it.itemStack.isSword }),
        AXE("Axe", { it.itemStack.isAxe }),
        MACE("Mace", { it.itemStack.item is MaceItem }),

        /**
         * Do not prefer any weapon type. This is useful if you only
         * want to make use of either [autoShieldBreak] or [autoMace].
         */
        NONE("None", { false });
    }

    private val switchBack by int("SwitchBack", 20, 1..300, "ticks")

    private val changeOnActions by multiEnumChoice<ChangeOnAction>(
        "ChangeOn",
        EnumSet.of(ChangeOnAction.ON_ATTACK)
    )

    @Suppress("unused")
    private enum class ChangeOnAction(
        override val choiceName: String
    ): NamedChoice {
        ON_ATTACK("OnAttack"),
        ON_TARGET("OnTarget")
    }

    /**
     * Prioritize Auto Buff or consuming an item over Auto Weapon
     */
    private val isBusy: Boolean
        get() = SilentHotbar.isSlotModifiedBy(ModuleAutoBuff) || player.isUsingItem && player.activeHand ==
            Hand.MAIN_HAND && player.activeItem.isConsumable

    /**
     * Check if the attack will break the shield
     */
    val willShieldBreak: Boolean
        get() {
            if (isOlderThanOrEqual1_8) {
                return false
            }

            // If we have an axe in our main hand, we will break the shield
            if (player.mainHandStack.isAxe) {
                return true
            }

            // If we are not going to switch to an axe, we will not break the shield
            return determineWeaponSlot(null, enforceShield = true)?.itemStack?.isAxe == true
        }

    /**
     * Check if the attack will mace smash
     */
    val willMaceSmash: Boolean
        get() {
            if (!canMaceSmash) {
                return false
            }

            if (player.mainHandStack.item is MaceItem) {
                return true
            }

            return determineWeaponSlot(null)?.itemStack?.item is MaceItem
        }

    // https://minecraft.wiki/w/Mace#Falling
    private val canMaceSmash
        get() = !isOlderThanOrEqual1_8 && MaceItem.shouldDealAdditionalDamage(player)

    @Suppress("unused")
    private val attackHandler = handler<AttackEntityEvent> { event ->
        val entity = event.entity as? LivingEntity ?: return@handler
        val weaponSlot = determineWeaponSlot(entity)?.hotbarSlot ?: return@handler

        if (isBusy || ChangeOnAction.ON_ATTACK !in changeOnActions) {
            return@handler
        }

        SilentHotbar.selectSlotSilently(
            this,
            weaponSlot,
            switchBack
        )

        // [ClientPlayerInteractionManager.attackEntity] will sync the selected slot,
        // so we can do that here already. This is legitimate, but unfortunately, the server seems
        // to not care about the sync when it occurs in the same tick as the attack.
        interaction.syncSelectedSlot()
    }

    /**
     * Prepare AutoWeapon for given [entity] if [onTarget] is enabled
     */
    fun onTarget(entity: Entity?) {
        if (!running || entity !is LivingEntity || isBusy || ChangeOnAction.ON_TARGET !in changeOnActions) {
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
        val requiresShield = autoShieldBreak && (enforceShield || target?.wouldBlockHit == true)
        val requiresMace = autoMace && canMaceSmash

        val bestSlot = Slots.Hotbar
            .flatMap { slot -> itemCategorization.getItemFacets(slot).filterIsInstance<WeaponItemFacet>() }
            .filter { itemFacet ->
                when {
                    // A mace's smash attack cannot be blocked by a shield
                    requiresMace -> itemFacet.itemStack.item is MaceItem
                    // An axe will stun the target if it is blocking with a shield
                    requiresShield -> itemFacet.itemStack.isAxe
                    // Fall back to a preferred weapon when no special case applies
                    else -> preferredWeapon.filter(itemFacet)
                }
            }
            .maxOrNull()

        return bestSlot?.itemSlot as HotbarItemSlot?
    }

    /**
     * Get the attack speed of the determined weapon, or
     * return [original] if no weapon is selected
     * or if [ChangeOnAction] does not contain [ChangeOnAction.ON_ATTACK].
     *
     * When we switch our item on the same tick as we attack,
     * the cooldown progress is not updated.
     */
    fun getAttackSpeed(original: Double): Double {
        debugParameter("Original Attack Speed") { original }

        if (!running || ChangeOnAction.ON_ATTACK !in changeOnActions || !player.hasCooldown) {
            return original
        }

        val itemStack = determineWeaponSlot(null)?.itemStack ?: return original
        val itemAttackSpeed = itemStack.attackSpeed
        debugParameter("Item") { itemStack.itemName.convertToString() }
        debugParameter("Attack Speed") { itemAttackSpeed }

        return itemAttackSpeed
    }

}
