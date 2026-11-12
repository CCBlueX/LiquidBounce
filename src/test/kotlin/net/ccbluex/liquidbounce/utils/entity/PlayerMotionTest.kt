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

package net.ccbluex.liquidbounce.utils.entity

import net.ccbluex.liquidbounce.test.MinecraftBootstrap
import net.ccbluex.liquidbounce.utils.navigation.TestWorld
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.Vec3
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * [PlayerMotion] on slime, held against Entity.move, Entity.restituteMovementAfterCollisions and SlimeBlock worked
 * through by hand.
 */
class PlayerMotionTest {

    companion object {
        init {
            MinecraftBootstrap.ensureInitialized()
        }

        private val SLIME = Blocks.SLIME_BLOCK.defaultBlockState()
        private const val BOUNCE_TICKS = 60
        private const val WALK_TICKS = 40
    }

    /** A slime floor at y 0, so the feet rest at y 1. */
    private fun slime() = TestWorld().fill(-20, 0, -20, 20, 0, 20, SLIME)

    /** Drops the player from rest at [height] and returns the highest the feet get after the first landing. */
    private fun bounceHeight(world: TestWorld, height: Double, sneak: Boolean = false): Double {
        val motion = PlayerMotion(world.terrain())
        motion.reset(Vec3(0.5, height, 0.5), Vec3.ZERO, onGround = false)
        var landed = false
        var peak = 0.0
        repeat(BOUNCE_TICKS) {
            motion.tick(0f, 0, 0, jump = false, sprint = false, sneak = sneak)
            landed = landed || motion.onGround
            if (landed) {
                peak = maxOf(peak, motion.y)
            }
        }
        return peak
    }

    @Test
    fun `a fall onto slime bounces back up`() {
        // 17 ticks of falling end 0.5 into the 18th; the rebound gets 0.98 drag for the part of it moved that tick.
        assertEquals(7.5065, bounceHeight(slime(), 11.0), 1.0E-3)
        assertEquals(5.2417, bounceHeight(slime(), 7.0), 1.0E-3)
    }

    @Test
    fun `sneaking onto slime does not bounce`() {
        assertEquals(1.0, bounceHeight(slime(), 11.0, sneak = true), 1.0E-9)
    }

    @Test
    fun `a fall onto slime does not hurt`() {
        val onSlime = PlayerMotion(slime().terrain())
        onSlime.reset(Vec3(0.5, 11.0, 0.5), Vec3.ZERO, onGround = false)
        val onStone = PlayerMotion(TestWorld.flat().terrain())
        onStone.reset(Vec3(0.5, 11.0, 0.5), Vec3.ZERO, onGround = false)
        while (!onSlime.onGround || !onStone.onGround) {
            if (!onSlime.onGround) onSlime.tick(0f, 0, 0, jump = false, sprint = false)
            if (!onStone.onGround) onStone.tick(0f, 0, 0, jump = false, sprint = false)
        }

        assertEquals(0.0, onSlime.landingFall)
        assertEquals(10.0, onStone.landingFall, 1.0E-9)
    }

    @Test
    fun `walking on slime is slowed after gravity pulls the feet down`() {
        // SlimeBlock.stepOn scales by 0.4 + |motionY| * 0.2, and motionY is the -0.0784 of standing after the move.
        val motion = PlayerMotion(slime().terrain())
        motion.reset(Vec3(0.5, 1.0, 0.5), PlayerMotion.RESTING, onGround = true)
        repeat(WALK_TICKS) {
            motion.tick(0f, 1, 0, jump = false, sprint = false)
        }
        val from = motion.z
        motion.tick(0f, 1, 0, jump = false, sprint = false)

        val accelerate = 0.98 * 0.1 * 0.21600002f / (0.8f * 0.8f * 0.8f)
        val keep = 0.8f * 0.91f * (0.4 + 0.0784 * 0.2)
        assertEquals(accelerate / (1 - keep), motion.z - from, 1.0E-5)
        assertTrue(motion.onGround)
    }
}
