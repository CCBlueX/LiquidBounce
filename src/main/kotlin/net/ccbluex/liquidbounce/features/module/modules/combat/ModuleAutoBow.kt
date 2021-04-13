/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2016 - 2021 CCBlueX
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

import net.ccbluex.liquidbounce.event.GameTickEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ListenableConfigurable
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.repeatable
import net.ccbluex.liquidbounce.render.engine.Vec3
import net.ccbluex.liquidbounce.utils.extensions.*
import net.minecraft.item.BowItem
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.util.math.MathHelper
import net.minecraft.util.math.Vec3d
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.sqrt


/**
 * AutoBow module
 *
 * Automatically shoots with your bow when it's fully charged
 *  + and make it possible to shoot faster
 */
object ModuleAutoBow : Module("AutoBow", Category.COMBAT) {

    /**
     * Automatically shoots with your bow when you aim correctly at an enemy or when the bow is fully charged.
     */
    private object AutoShootOptions : ListenableConfigurable(this, "AutoShoot", true) {

        val charged by int("Charged", 20, 3..20)

        val tickRepeatable = handler<GameTickEvent> {
            val currentItem = player.activeItem

            // Should check if player is using bow
            if (currentItem?.item is BowItem) {
                // Wait until bow is fully charged
                if (player.itemUseTime < charged)
                    return@handler

                // Send stop using item to server
                network.sendPacket(
                    PlayerActionC2SPacket(
                        PlayerActionC2SPacket.Action.RELEASE_USE_ITEM, BlockPos.ORIGIN,
                        Direction.DOWN
                    )
                )
                // Stop using item client-side
                player.stopUsingItem()
            }
        }
    }

    /**
     * Bow aimbot automatically aims at enemy targets
     */
    private object BowAimbotOptions : ListenableConfigurable(this, "BowAimbot", false) {

        // Target
        val targetTracker = TargetTracker()

        // Rotation
        val rotationConfigurable = RotationsConfigurable()

        val tickRepeatable = handler<GameTickEvent> {
            targetTracker.update()

            val world = world
            val player = player

            val eyePos = player.eyesPos

            val target = targetTracker.firstOrNull() ?: return@handler

            val rotation = faceBow(target.boundingBox.center.subtract(eyePos), FastChargeOptions.enabled)

            RotationManager.aimAt(rotation, configurable = rotationConfigurable)
        }

    }

    override fun disable() {
        BowAimbotOptions.targetTracker.cleanup()
    }

    fun faceBow(target: Vec3d, assumeElongated: Boolean): Rotation {
        val player = player

        val posSqrt = sqrt(target.x * target.x + target.z * target.z)

        var velocity = if (assumeElongated) 1f else player.itemUseTime / 20f

        velocity = (velocity * velocity + velocity * 2) / 3

        if (velocity > 1)
            velocity = 1f

        return Rotation(
            (atan2(target.z, target.x) * 180.0f / Math.PI).toFloat() - 90.0f,
            (-Math.toDegrees(atan((velocity * velocity - sqrt(velocity * velocity * velocity * velocity - 0.006f * (0.006f * (posSqrt * posSqrt) + 2 * target.y * (velocity * velocity)))) / (0.006f * posSqrt)).toDouble())).toFloat()
        )
    }

    fun getRotationVector(pitch: Float, yaw: Float): Vec3 {
        val f = pitch * 0.017453292f
        val g = -yaw * 0.017453292f

        val h = MathHelper.cos(g)
        val i = MathHelper.sin(g)
        val j = MathHelper.cos(f)
        val k = MathHelper.sin(f)

        return Vec3(i * j, -k, h * j)
    }

    /**
     * @desc Fast charge options (like FastBow) can be used to charge the bow faster.
     * @warning Should only be used on vanilla minecraft. Most anti cheats patch these kinds of exploits
     *
     * TODO: Add version specific options
     */
    private object FastChargeOptions : ListenableConfigurable(this, "FastCharge", true) {

        val packets by int("Packets", 20, 3..20)

        val tickRepeatable = repeatable {
            val currentItem = player.activeItem

            // Should accelerated game ticks when using bow
            if (currentItem?.item is BowItem) {
                repeat(packets) {
                    // Send movement packet to simulate ticks (has been patched in 1.19)
                    network.sendPacket(PlayerMoveC2SPacket(true))
                    // Just show visual effect (not required to work - but looks better)
                    player.tickActiveItemStack()
                }

                // Shoot with bow (auto shoot has to be enabled)
                // TODO: Depend on Auto Shoot
            }
        }

    }

    init {
        tree(AutoShootOptions)
        tree(BowAimbotOptions)
        tree(FastChargeOptions)
    }

}

