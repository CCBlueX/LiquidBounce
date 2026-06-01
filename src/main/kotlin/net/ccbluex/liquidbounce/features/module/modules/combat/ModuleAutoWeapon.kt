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

import net.ccbluex.fastutil.enumSetOf
import net.ccbluex.liquidbounce.config.types.list.Tagged
import net.ccbluex.liquidbounce.event.events.AttackEntityEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.features.module.modules.combat.ModuleAutoWeapon.autoMace
import net.ccbluex.liquidbounce.features.module.modules.combat.ModuleAutoWeapon.autoShieldBreak
import net.ccbluex.liquidbounce.features.module.modules.combat.ModuleAutoWeapon.onTarget
import net.ccbluex.liquidbounce.features.module.modules.player.autobuff.ModuleAutoBuff
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.ItemCategorization
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.items.WeaponItemFacet
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug.debugParameter
import net.ccbluex.liquidbounce.utils.client.SilentHotbar
import net.ccbluex.liquidbounce.utils.client.isOlderThanOrEqual1_8
import net.ccbluex.liquidbounce.utils.entity.hasCooldown
import net.ccbluex.liquidbounce.utils.entity.wouldBlockHit
import net.ccbluex.liquidbounce.utils.inventory.HotbarItemSlot
import net.ccbluex.liquidbounce.utils.inventory.Slots
import net.ccbluex.liquidbounce.utils.item.WeaponType
import net.ccbluex.liquidbounce.utils.item.attackSpeed
import net.ccbluex.liquidbounce.utils.item.isAxe
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.ModuleKillAura
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.features.KillAuraAutoBlock
import net.ccbluex.liquidbounce.utils.item.isSword
import net.ccbluex.liquidbounce.utils.item.getEnchantment
import net.ccbluex.liquidbounce.utils.kotlin.matchesAny
import net.minecraft.core.BlockPos
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.MaceItem
import net.minecraft.world.item.enchantment.Enchantments

/**
 * AutoWeapon module
 *
 * Automatically selects the best weapon in your hotbar
 */
object ModuleAutoWeapon : ClientModule("AutoWeapon", ModuleCategories.COMBAT) {

    /**
     * The weapon type to prefer, which on 1.8 and 1.9+ versions is usually a sword,
     * due to the attack speed.
     *
     * If this [Set] is empty, do not prefer any weapon type.
     * This is useful if you only want to make use of either [autoShieldBreak] or [autoMace].
     */
    private val preferredWeapon by multiEnumChoice("Preferred", WeaponType.SWORD)

    private val autoShieldBreak by boolean("AutoShieldBreak", true)
    private val autoMace by boolean("AutoMace", true)
    private val block1_8Priority by boolean("1.8BlockPriority", true)
    private val voidKnockbackPriority by boolean("VoidKnockbackPriority", true)

    private val switchBack by int("SwitchBack", 20, 1..300, "ticks")

    private val changeOnActions by multiEnumChoice<ChangeOnAction>(
        "ChangeOn",
        enumSetOf(ChangeOnAction.ON_ATTACK)
    )

    @Suppress("unused")
    private enum class ChangeOnAction(
        override val tag: String
    ): Tagged {
        ON_ATTACK("OnAttack"),
        ON_TARGET("OnTarget")
    }

    /**
     * Prioritize Auto Buff or consuming an item over Auto Weapon
     */
    private val isBusy: Boolean
        get() = SilentHotbar.isSlotModifiedBy(ModuleAutoBuff) || player.isUsingItem && player.usingItemHand ==
            InteractionHand.MAIN_HAND && player.useItem.isConsumable

    /**
     * Check if the attack will break the shield
     */
    val willShieldBreak: Boolean
        get() {
            if (isOlderThanOrEqual1_8) {
                return false
            }

            // If we have an axe in our main hand, we will break the shield
            if (player.mainHandItem.isAxe) {
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

            if (player.mainHandItem.item is MaceItem) {
                return true
            }

            return determineWeaponSlot(null)?.itemStack?.item is MaceItem
        }

    // https://minecraft.wiki/w/Mace#Falling
    private val canMaceSmash
        get() = (!isOlderThanOrEqual1_8 && MaceItem.canSmashAttack(player)) || ModuleMaceKill.enabled

    @Suppress("unused")
    private val attackHandler = handler<AttackEntityEvent> { event ->
        val entity = event.entity as? LivingEntity ?: return@handler
        val weaponSlot = determineWeaponSlot(entity)?.inventorySlot ?: return@handler

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
        interaction.ensureHasSentCarriedItem()
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
                slot.inventorySlot,
                switchBack
            )
        }
    }

    override fun onDisabled() {
        SilentHotbar.resetSlot(this)
    }

    private fun determineWeaponSlot(target: LivingEntity?, enforceShield: Boolean = false): HotbarItemSlot? {
        val itemCategorization = ItemCategorization(Slots.Hotbar)
        val requiresShield = autoShieldBreak && (enforceShield || target?.wouldBlockHit == true)
        val requiresMace = autoMace && canMaceSmash

        val requiresLegacySword = block1_8Priority && KillAuraAutoBlock.running &&
                (!KillAuraAutoBlock.onlyWhenInDanger || KillAuraAutoBlock.isInDanger) && target != null

        val prioritizeKnockback = voidKnockbackPriority && target != null && isNearVoid(target)

        val bestSlot = Slots.Hotbar
            .flatMap { slot -> itemCategorization.getItemFacets(slot).filterIsInstance<WeaponItemFacet>() }
            .filter { itemFacet ->
                val itemStack = itemFacet.itemStack
                when {
                    // A mace's smash attack cannot be blocked by a shield
                    requiresMace -> WeaponType.MACE.test(itemStack)
                    // An axe will stun the target if it is blocking with a shield
                    requiresShield -> WeaponType.AXE.test(itemStack)
                    // If legacy block priority is active, always allow swords
                    requiresLegacySword && itemStack.isSword -> true
                    // If void knockback is active, always allow knockback items
                    prioritizeKnockback && itemStack.getEnchantment(Enchantments.KNOCKBACK) > 0 -> true
                    // Fall back to a preferred weapon when no special case applies
                    else -> preferredWeapon.matchesAny(itemStack)
                }
            }
            .maxWithOrNull(Comparator { o1, o2 ->
                if (prioritizeKnockback) {
                    val kb1 = o1.itemStack.getEnchantment(Enchantments.KNOCKBACK)
                    val kb2 = o2.itemStack.getEnchantment(Enchantments.KNOCKBACK)
                    if (kb1 != kb2) {
                        return@Comparator kb1.compareTo(kb2)
                    }
                }
                if (requiresLegacySword) {
                    val o1Sword = o1.itemStack.isSword
                    val o2Sword = o2.itemStack.isSword
                    if (o1Sword != o2Sword) {
                        return@Comparator if (o1Sword) 1 else -1
                    }
                }
                o1.compareTo(o2)
            })

        return bestSlot?.itemSlot as HotbarItemSlot?
    }

    private fun isNearVoid(target: LivingEntity): Boolean {
        val level = mc.level ?: return false

        val dx = target.x - player.x
        val dz = target.z - player.z
        val len = Math.sqrt(dx * dx + dz * dz)
        if (len < 0.1) return false

        val nx = dx / len
        val nz = dz / len

        val targetY = target.blockPosition().y
        var voidBlocksCount = 0
        val checkDistances = listOf(1.5, 3.0, 4.5, 6.0)

        for (dist in checkDistances) {
            val checkX = target.x + nx * dist
            val checkZ = target.z + nz * dist

            val blockPos = BlockPos(checkX.toInt(), targetY, checkZ.toInt())
            var hasBlockBelow = false

            for (dy in 0..16) {
                val p = blockPos.below(dy)
                if (p.y < level.minBuildHeight) {
                    break
                }
                val state = level.getBlockState(p)
                if (!state.isAir && !state.getCollisionShape(level, p).isEmpty) {
                    hasBlockBelow = true
                    break
                }
            }

            if (!hasBlockBelow) {
                voidBlocksCount++
            }
        }

        return voidBlocksCount >= 2
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
        debugParameter("Item") { itemStack.itemName.string }
        debugParameter("Attack Speed") { itemAttackSpeed }

        return itemAttackSpeed
    }

}
