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
package net.ccbluex.liquidbounce.features.module.modules.combat.anchoraura

const val ANCHOR_MAX_CHARGE: Int = 4

enum class AnchorAuraAction {
    PLACE,
    CHARGE,
    DETONATE,
}

enum class AnchorAuraPhase {
    SEARCHING,
    PLACING,
    CHARGING,
    PROTECTING,
    DETONATING,
}

data class AnchorDamageLimits(
    val minEnemyDamage: Float,
    val maxSelfDamage: Float,
    val maxFriendDamage: Float,
    val antiSuicide: Boolean,
    val lethalOverride: Boolean,
    val efficient: Boolean,
)

data class AnchorDamageInput(
    val enemyDamage: Float,
    val selfDamage: Float,
    val friendDamage: Float,
    val enemyHealth: Float,
    val selfHealth: Float,
) {
    val isLethal: Boolean
        get() = enemyDamage >= enemyHealth
}

enum class AnchorDamageRejection {
    MIN_ENEMY_DAMAGE,
    SELF_DAMAGE,
    FRIEND_DAMAGE,
    ANTI_SUICIDE,
    INEFFICIENT,
}

/**
 * Minecraft-independent decisions kept separate so state, retry, and safety behavior can be tested deterministically.
 */
object AnchorAuraLogic {

    /**
     * Returns null for an invalid charge value. Only an uncharged anchor needs glowstone. Every charged state has the
     * same blast power and can be detonated immediately, so charging more than once wastes both time and glowstone.
     */
    @JvmStatic
    @JvmOverloads
    fun actionFor(
        anchorPresent: Boolean,
        charges: Int,
        maxCharge: Int = ANCHOR_MAX_CHARGE,
    ): AnchorAuraAction? {
        if (maxCharge <= 0) {
            return null
        }

        if (!anchorPresent) {
            return AnchorAuraAction.PLACE
        }

        if (charges !in 0..maxCharge) {
            return null
        }

        return if (charges == 0) {
            AnchorAuraAction.CHARGE
        } else {
            AnchorAuraAction.DETONATE
        }
    }

    @JvmStatic
    fun phaseFor(action: AnchorAuraAction): AnchorAuraPhase = when (action) {
        AnchorAuraAction.PLACE -> AnchorAuraPhase.PLACING
        AnchorAuraAction.CHARGE -> AnchorAuraPhase.CHARGING
        AnchorAuraAction.DETONATE -> AnchorAuraPhase.DETONATING
    }

    /**
     * Lethal override relaxes minimum enemy damage and efficiency only. It never relaxes self/friend limits or
     * anti-suicide, since those checks protect entities other than the selected enemy.
     */
    @JvmStatic
    fun damageRejection(input: AnchorDamageInput, limits: AnchorDamageLimits): AnchorDamageRejection? {
        val overrideLethal = limits.lethalOverride && input.isLethal

        if (limits.antiSuicide && input.selfDamage >= input.selfHealth) {
            return AnchorDamageRejection.ANTI_SUICIDE
        }

        if (input.selfDamage > limits.maxSelfDamage) {
            return AnchorDamageRejection.SELF_DAMAGE
        }

        if (input.friendDamage > limits.maxFriendDamage) {
            return AnchorDamageRejection.FRIEND_DAMAGE
        }

        if (!overrideLethal && input.enemyDamage < limits.minEnemyDamage) {
            return AnchorDamageRejection.MIN_ENEMY_DAMAGE
        }

        if (!overrideLethal && limits.efficient && input.enemyDamage <= input.selfDamage) {
            return AnchorDamageRejection.INEFFICIENT
        }

        return null
    }

    @JvmStatic
    fun canAttempt(nowMs: Long, lastActionMs: Long, notBeforeMs: Long, delayMs: Long): Boolean {
        if (nowMs < notBeforeMs) {
            return false
        }

        return lastActionMs <= 0L || nowMs >= lastActionMs && nowMs - lastActionMs >= delayMs.coerceAtLeast(0L)
    }

    /** Exponential backoff capped at four doublings and [maxDelayMs]. */
    @JvmStatic
    fun retryDelayMs(failures: Int, baseDelayMs: Long, maxDelayMs: Long): Long {
        if (failures <= 0) {
            return 0L
        }

        val base = baseDelayMs.coerceAtLeast(0L)
        val cap = maxDelayMs.coerceAtLeast(base)
        val multiplier = 1L shl (failures - 1).coerceIn(0, 4)
        return if (base > cap / multiplier) cap else (base * multiplier).coerceAtMost(cap)
    }

    @JvmStatic
    @JvmOverloads
    fun candidateScore(
        damage: AnchorDamageInput,
        distance: Double,
        existingAnchor: Boolean,
        visible: Boolean,
        lethalBonus: Float = 100f,
    ): Float {
        val completionBonus = if (existingAnchor) 0.75f else 0f
        val visibilityBonus = if (visible) 0.25f else 0f
        val killBonus = if (damage.isLethal) lethalBonus else 0f
        return damage.enemyDamage - damage.selfDamage + completionBonus + visibilityBonus + killBonus -
            (distance * 0.05).toFloat()
    }

}
