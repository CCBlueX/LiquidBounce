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
 * [PlayerMotion] on slime and beds, held against Entity.move, Entity.restituteMovementAfterCollisions, Block.fallOn and
 * SlimeBlock worked through by hand.
 */
class PlayerMotionTest {

    companion object {
        init {
            MinecraftBootstrap.ensureInitialized()
        }

        private val SLIME = Blocks.SLIME_BLOCK.defaultBlockState()
        private val BED = Blocks.BED.red().defaultBlockState()
        private const val BOUNCE_TICKS = 60
        private const val WALK_TICKS = 40
    }

    /** A slime floor at y 0, so the feet rest at y 1. */
    private fun slime() = TestWorld().fill(-20, 0, -20, 20, 0, 20, SLIME)

    /** A stone floor at y 0 with a bed on it, so the feet rest at y 1.5625 on the bed. */
    private fun bed() = TestWorld.flat().set(0, 1, 0, BED)

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
    fun `a fall onto a bed bounces back three quarters of the speed`() {
        // Entity.getOnPosLegacy, 0.2 below the feet at 1.5625, is the bed itself and not the floor under it.
        assertEquals(5.1062, bounceHeight(bed(), 11.0), 1.0E-3)
        assertEquals(3.7540, bounceHeight(bed(), 7.0), 1.0E-3)
    }

    @Test
    fun `sneaking onto a bed does not bounce`() {
        assertEquals(1.5625, bounceHeight(bed(), 11.0, sneak = true), 1.0E-9)
    }

    @Test
    fun `a fall onto carpet on slime bounces as onto the slime`() {
        val world = slime().fill(-20, 1, -20, 20, 1, 20, Blocks.CARPET.red().defaultBlockState())
        assertEquals(7.5364, bounceHeight(world, 11.0), 1.0E-3)
    }

    @Test
    fun `a fall onto a bed hurts half`() {
        val motion = PlayerMotion(bed().terrain())
        motion.reset(Vec3(0.5, 11.0, 0.5), Vec3.ZERO, onGround = false)
        while (!motion.onGround) {
            motion.tick(0f, 0, 0, jump = false, sprint = false)
        }

        assertEquals((11.0 - 1.5625) / 2, motion.landingFall, 1.0E-9)
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
