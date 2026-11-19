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

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AnchorAuraLogicTest {

    private fun damage(
        enemyDamage: Float = 10f,
        selfDamage: Float = 2f,
        friendDamage: Float = 1f,
        enemyHealth: Float = 20f,
        selfHealth: Float = 10f,
    ) = AnchorDamageInput(enemyDamage, selfDamage, friendDamage, enemyHealth, selfHealth)

    private fun limits(
        minEnemyDamage: Float = 5f,
        maxSelfDamage: Float = 4f,
        maxFriendDamage: Float = 3f,
        antiSuicide: Boolean = true,
        lethalOverride: Boolean = false,
        efficient: Boolean = true,
    ) = AnchorDamageLimits(
        minEnemyDamage,
        maxSelfDamage,
        maxFriendDamage,
        antiSuicide,
        lethalOverride,
        efficient,
    )

    @Test
    fun `missing anchor starts the place transition`() {
        val action = assertNotNull(AnchorAuraLogic.actionFor(anchorPresent = false, charges = 0))

        assertEquals(AnchorAuraAction.PLACE, action)
        assertEquals(AnchorAuraPhase.PLACING, AnchorAuraLogic.phaseFor(action))
    }

    @Test
    fun `nonfull anchors charge until the maximum`() {
        for (charges in 0 until ANCHOR_MAX_CHARGE) {
            assertEquals(AnchorAuraAction.CHARGE, AnchorAuraLogic.actionFor(true, charges))
            assertEquals(
                AnchorAuraPhase.CHARGING,
                AnchorAuraLogic.phaseFor(AnchorAuraLogic.actionFor(true, charges)!!),
            )
        }
    }

    @Test
    fun `full anchor detonates instead of consuming more glowstone`() {
        val action = assertNotNull(AnchorAuraLogic.actionFor(anchorPresent = true, charges = ANCHOR_MAX_CHARGE))

        assertEquals(AnchorAuraAction.DETONATE, action)
        assertEquals(AnchorAuraPhase.DETONATING, AnchorAuraLogic.phaseFor(action))
    }

    @Test
    fun `invalid charge states have no action`() {
        assertNull(AnchorAuraLogic.actionFor(true, charges = -1))
        assertNull(AnchorAuraLogic.actionFor(true, charges = ANCHOR_MAX_CHARGE + 1))
        assertNull(AnchorAuraLogic.actionFor(false, charges = 0, maxCharge = 0))
    }

    @Test
    fun `custom positive full-charge boundary is respected`() {
        assertEquals(AnchorAuraAction.CHARGE, AnchorAuraLogic.actionFor(true, charges = 0, maxCharge = 1))
        assertEquals(AnchorAuraAction.DETONATE, AnchorAuraLogic.actionFor(true, charges = 1, maxCharge = 1))
    }

    @Test
    fun `valid damage inside every limit is accepted`() {
        assertNull(AnchorAuraLogic.damageRejection(damage(), limits()))
    }

    @Test
    fun `minimum enemy damage has an inclusive lower boundary`() {
        assertEquals(
            AnchorDamageRejection.MIN_ENEMY_DAMAGE,
            AnchorAuraLogic.damageRejection(damage(enemyDamage = 4.9f), limits()),
        )
        assertNull(AnchorAuraLogic.damageRejection(damage(enemyDamage = 5f), limits()))
    }

    @Test
    fun `self and friend limits reject only damage above their caps`() {
        assertNull(AnchorAuraLogic.damageRejection(damage(selfDamage = 4f, friendDamage = 3f), limits()))
        assertEquals(
            AnchorDamageRejection.SELF_DAMAGE,
            AnchorAuraLogic.damageRejection(damage(selfDamage = 4.1f), limits()),
        )
        assertEquals(
            AnchorDamageRejection.FRIEND_DAMAGE,
            AnchorAuraLogic.damageRejection(damage(friendDamage = 3.1f), limits()),
        )
    }

    @Test
    fun `anti suicide rejects damage equal to remaining health`() {
        assertEquals(
            AnchorDamageRejection.ANTI_SUICIDE,
            AnchorAuraLogic.damageRejection(damage(selfDamage = 10f), limits(maxSelfDamage = 20f)),
        )
        assertEquals(
            AnchorDamageRejection.ANTI_SUICIDE,
            AnchorAuraLogic.damageRejection(damage(selfDamage = 11f), limits(maxSelfDamage = 20f)),
        )
    }

    @Test
    fun `anti suicide can be disabled independently of the self cap`() {
        assertNull(
            AnchorAuraLogic.damageRejection(
                damage(enemyDamage = 20f, selfDamage = 12f),
                limits(maxSelfDamage = 20f, antiSuicide = false),
            ),
        )
    }

    @Test
    fun `efficiency rejects equal damage but accepts a greater enemy value`() {
        assertEquals(
            AnchorDamageRejection.INEFFICIENT,
            AnchorAuraLogic.damageRejection(damage(enemyDamage = 6f, selfDamage = 6f), limits(maxSelfDamage = 8f)),
        )
        assertNull(
            AnchorAuraLogic.damageRejection(damage(enemyDamage = 7f, selfDamage = 6f), limits(maxSelfDamage = 8f))
        )
        assertNull(
            AnchorAuraLogic.damageRejection(
                damage(enemyDamage = 6f, selfDamage = 6f),
                limits(maxSelfDamage = 8f, efficient = false),
            ),
        )
    }

    @Test
    fun `lethal override relaxes minimum enemy damage and efficiency`() {
        val input = damage(
            enemyDamage = 10f,
            selfDamage = 10f,
            friendDamage = 0f,
            enemyHealth = 10f,
            selfHealth = 20f,
        )
        val limits = limits(
            minEnemyDamage = 20f,
            maxSelfDamage = 20f,
            lethalOverride = true,
            efficient = true,
        )

        assertNull(AnchorAuraLogic.damageRejection(input, limits))
    }

    @Test
    fun `lethal override never relaxes lethal self damage`() {
        val input = damage(enemyDamage = 10f, selfDamage = 10f, enemyHealth = 10f, selfHealth = 10f)

        assertEquals(
            AnchorDamageRejection.ANTI_SUICIDE,
            AnchorAuraLogic.damageRejection(input, limits(maxSelfDamage = 20f, lethalOverride = true)),
        )
    }

    @Test
    fun `lethal override never relaxes self or friend caps`() {
        assertEquals(
            AnchorDamageRejection.SELF_DAMAGE,
            AnchorAuraLogic.damageRejection(
                damage(enemyDamage = 20f, selfDamage = 11f, enemyHealth = 10f, selfHealth = 20f),
                limits(maxSelfDamage = 10f, lethalOverride = true),
            ),
        )
        assertEquals(
            AnchorDamageRejection.FRIEND_DAMAGE,
            AnchorAuraLogic.damageRejection(
                damage(enemyDamage = 20f, friendDamage = 4f, enemyHealth = 10f),
                limits(lethalOverride = true),
            ),
        )
    }

    @Test
    fun `nonlethal damage cannot use lethal override to bypass efficiency`() {
        assertEquals(
            AnchorDamageRejection.INEFFICIENT,
            AnchorAuraLogic.damageRejection(
                damage(enemyDamage = 9f, selfDamage = 9f, enemyHealth = 10f),
                limits(maxSelfDamage = 10f, lethalOverride = true),
            ),
        )
    }

    @Test
    fun `attempt gate respects both not-before and action delay`() {
        assertFalse(AnchorAuraLogic.canAttempt(nowMs = 99L, lastActionMs = 0L, notBeforeMs = 100L, delayMs = 0L))
        assertTrue(AnchorAuraLogic.canAttempt(nowMs = 100L, lastActionMs = 0L, notBeforeMs = 100L, delayMs = 500L))
        assertFalse(AnchorAuraLogic.canAttempt(nowMs = 149L, lastActionMs = 100L, notBeforeMs = 0L, delayMs = 50L))
        assertTrue(AnchorAuraLogic.canAttempt(nowMs = 150L, lastActionMs = 100L, notBeforeMs = 0L, delayMs = 50L))
    }

    @Test
    fun `attempt gate handles negative delay and out-of-order timestamps`() {
        assertTrue(AnchorAuraLogic.canAttempt(nowMs = 100L, lastActionMs = 100L, notBeforeMs = 0L, delayMs = -1L))
        assertFalse(AnchorAuraLogic.canAttempt(nowMs = 99L, lastActionMs = 100L, notBeforeMs = 0L, delayMs = 0L))
    }

    @Test
    fun `retry backoff doubles only for positive failure counts`() {
        assertEquals(0L, AnchorAuraLogic.retryDelayMs(0, baseDelayMs = 25L, maxDelayMs = 1000L))
        assertEquals(0L, AnchorAuraLogic.retryDelayMs(-1, baseDelayMs = 25L, maxDelayMs = 1000L))
        assertEquals(25L, AnchorAuraLogic.retryDelayMs(1, baseDelayMs = 25L, maxDelayMs = 1000L))
        assertEquals(50L, AnchorAuraLogic.retryDelayMs(2, baseDelayMs = 25L, maxDelayMs = 1000L))
        assertEquals(100L, AnchorAuraLogic.retryDelayMs(3, baseDelayMs = 25L, maxDelayMs = 1000L))
    }

    @Test
    fun `retry backoff caps growth and avoids overflow`() {
        assertEquals(100L, AnchorAuraLogic.retryDelayMs(4, baseDelayMs = 25L, maxDelayMs = 100L))
        assertEquals(400L, AnchorAuraLogic.retryDelayMs(5, baseDelayMs = 25L, maxDelayMs = 1000L))
        assertEquals(400L, AnchorAuraLogic.retryDelayMs(100, baseDelayMs = 25L, maxDelayMs = 1000L))
        assertEquals(
            Long.MAX_VALUE,
            AnchorAuraLogic.retryDelayMs(2, baseDelayMs = Long.MAX_VALUE, maxDelayMs = Long.MAX_VALUE),
        )
    }

    @Test
    fun `candidate score combines lethality completion visibility and distance`() {
        val score = AnchorAuraLogic.candidateScore(
            damage = damage(enemyDamage = 12f, selfDamage = 2f, enemyHealth = 12f),
            distance = 4.0,
            existingAnchor = true,
            visible = true,
        )

        assertEquals(110.8f, score, 0.0001f)
    }

    @Test
    fun `nonlethal candidate receives no kill or completion bonuses`() {
        val score = AnchorAuraLogic.candidateScore(
            damage = damage(enemyDamage = 9f, selfDamage = 2f, enemyHealth = 10f),
            distance = 0.0,
            existingAnchor = false,
            visible = false,
        )

        assertEquals(7f, score)
    }
}
