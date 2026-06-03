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

import it.unimi.dsi.fastutil.ints.Int2BooleanOpenHashMap
import net.ccbluex.fastutil.enumSetOf
import net.ccbluex.liquidbounce.config.types.list.Tagged
import net.ccbluex.liquidbounce.event.events.AttackEntityEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.features.module.modules.combat.ModuleAutoWeapon.autoMace
import net.ccbluex.liquidbounce.features.module.modules.combat.ModuleAutoWeapon.autoShieldBreak
import net.ccbluex.liquidbounce.features.module.modules.combat.ModuleAutoWeapon.onTarget
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.features.KillAuraAutoBlock
import net.ccbluex.liquidbounce.features.module.modules.player.autobuff.ModuleAutoBuff
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.ItemCategorization
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.items.WeaponItemFacet
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug.debugParameter
import net.ccbluex.liquidbounce.utils.block.collisionShape
import net.ccbluex.liquidbounce.utils.client.SilentHotbar
import net.ccbluex.liquidbounce.utils.client.isBlocksAttacksExisting
import net.ccbluex.liquidbounce.utils.client.isOlderThanOrEqual1_8
import net.ccbluex.liquidbounce.utils.entity.hasCooldown
import net.ccbluex.liquidbounce.utils.entity.wouldBlockHit
import net.ccbluex.liquidbounce.utils.inventory.HotbarItemSlot
import net.ccbluex.liquidbounce.utils.inventory.Slots
import net.ccbluex.liquidbounce.utils.item.WeaponType
import net.ccbluex.liquidbounce.utils.item.attackSpeed
import net.ccbluex.liquidbounce.utils.item.getEnchantment
import net.ccbluex.liquidbounce.utils.item.isAxe
import net.ccbluex.liquidbounce.utils.item.isConsumable
import net.ccbluex.liquidbounce.utils.kotlin.matchesAny
import net.minecraft.core.BlockPos
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.MaceItem
import net.minecraft.world.item.enchantment.Enchantments
import net.minecraft.world.phys.Vec3
import kotlin.math.floor

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

    /**
     * Favor a sword when KillAura AutoBlock only blocks on danger and we are currently in danger,
     * so there is a blockable item in hand. Applies wherever item blocking exists (1.8 and 1.21.5+).
     */
    private val preferBlockingSword by boolean("PreferBlockingSword", true)

    /**
     * When the target stands next to the void, prefer a weapon enchanted with Knockback to push
     * them off the edge.
     */
    private val prioritizeVoidKnockback by boolean("PrioritizeVoidKnockback", true)

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

    private val voidCheckCache = Int2BooleanOpenHashMap()
    private var voidCheckCacheTick = -1

    private val voidCheckDistances = doubleArrayOf(0.5, 1.0, 1.5, 2.0, 2.5)

    private const val VOID_HITS_THRESHOLD = 3
    private const val VOID_MIN_Y = -64
    private const val DIRECTION_EPSILON = 1.0E-4

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

    private fun shouldPrioritizeKnockback(target: LivingEntity): Boolean {
        val direction = Vec3(target.x - player.x, 0.0, target.z - player.z)
        if (direction.lengthSqr() < DIRECTION_EPSILON) {
            return false
        }

        return isNearVoid(target, direction.normalize())
    }

    private fun isNearVoid(target: LivingEntity, direction: Vec3): Boolean {
        val tick = player.tickCount
        if (voidCheckCacheTick != tick) {
            voidCheckCache.clear()
            voidCheckCacheTick = tick
        }

        val targetId = target.id
        if (voidCheckCache.containsKey(targetId)) {
            return voidCheckCache.get(targetId)
        }

        val result = isNearVoidInDirection(target, direction)
        voidCheckCache.put(targetId, result)
        return result
    }

    private fun isNearVoidInDirection(target: LivingEntity, direction: Vec3): Boolean {
        val targetY = floor(target.boundingBox.minY).toInt()
        var voidBlocksCount = 0

        for (distance in voidCheckDistances) {
            val checkX = target.x + direction.x * distance
            val checkZ = target.z + direction.z * distance

            if (isNearVoidAt(targetY, checkX, checkZ)) {
                voidBlocksCount++
                if (voidBlocksCount >= VOID_HITS_THRESHOLD) {
                    return true
                }
            }
        }

        return false
    }

    private fun isNearVoidAt(
        targetY: Int,
        checkX: Double,
        checkZ: Double,
    ): Boolean {
        val blockX = floor(checkX).toInt()
        val blockZ = floor(checkZ).toInt()
        val pos = BlockPos.MutableBlockPos()

        for (y in targetY downTo VOID_MIN_Y) {
            // A column counts as void only when no block below has a collision shape to stand on.
            if (!pos.set(blockX, y, blockZ).collisionShape.isEmpty) {
                return false
            }
        }

        return true
    }

    private fun findKnockbackSlot(): HotbarItemSlot? {
        var bestSlot: HotbarItemSlot? = null
        var bestLevel = 0

        for (slot in Slots.Hotbar) {
            val knockbackLevel = slot.itemStack.getEnchantment(Enchantments.KNOCKBACK)
            if (knockbackLevel <= 0) {
                continue
            }

            if (knockbackLevel > bestLevel) {
                bestLevel = knockbackLevel
                bestSlot = slot
                continue
            }

            if (knockbackLevel == bestLevel && bestSlot != null &&
                HotbarItemSlot.PREFER_NEARBY.compare(slot, bestSlot) < 0
            ) {
                bestSlot = slot
            }
        }

        return bestSlot
    }

    private fun List<WeaponItemFacet>.firstBestMatching(
        predicate: (WeaponItemFacet) -> Boolean,
    ): WeaponItemFacet? = filter(predicate).maxOrNull()

    private fun determineWeaponSlot(target: LivingEntity?, enforceShield: Boolean = false): HotbarItemSlot? {
        val itemCategorization = ItemCategorization(Slots.Hotbar)
        val requiresShield = autoShieldBreak && (enforceShield || target?.wouldBlockHit == true)
        val requiresMace = autoMace && canMaceSmash
        // When AutoBlock only blocks on danger and we are in danger, favor a sword so we can block with it.
        // Sword blocking is not a 1.8-only feature: it also applies on 1.21.5+ where any item can block.
        val requiresBlockingSword = preferBlockingSword && isBlocksAttacksExisting &&
            KillAuraAutoBlock.enabled && KillAuraAutoBlock.onlyWhenInDanger && KillAuraAutoBlock.isInDanger
        val prioritizeKnockback = prioritizeVoidKnockback && target?.let(::shouldPrioritizeKnockback) == true

        val weaponFacets = Slots.Hotbar
            .flatMap { slot -> itemCategorization.getItemFacets(slot).filterIsInstance<WeaponItemFacet>() }

        val specialSlot = when {
            // A mace's smash attack cannot be blocked by a shield
            requiresMace -> weaponFacets.firstBestMatching { WeaponType.MACE.test(it.itemStack) }
            // An axe will stun the target if it is blocking with a shield
            requiresShield -> weaponFacets.firstBestMatching { WeaponType.AXE.test(it.itemStack) }
            // Favor a sword so AutoBlock can block with it when only blocking on danger
            requiresBlockingSword -> weaponFacets.firstBestMatching { WeaponType.SWORD.test(it.itemStack) }
            else -> null
        }

        if (specialSlot != null) {
            return specialSlot.itemSlot as HotbarItemSlot?
        }

        if (prioritizeKnockback) {
            val knockbackSlot = findKnockbackSlot()
            if (knockbackSlot != null) {
                return knockbackSlot
            }
        }

        val preferredSlot = weaponFacets
            .firstBestMatching { preferredWeapon.matchesAny(it.itemStack) }

        return preferredSlot?.itemSlot as HotbarItemSlot?
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
