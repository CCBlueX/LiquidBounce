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

package net.ccbluex.liquidbounce.features.module.modules.render.hats

import net.ccbluex.liquidbounce.config.types.group.Mode
import net.ccbluex.liquidbounce.config.types.group.ModeValueGroup
import net.ccbluex.liquidbounce.config.types.group.ValueGroup
import net.ccbluex.liquidbounce.event.events.WorldRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.misc.FriendManager
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleFreeLook
import net.ccbluex.liquidbounce.features.module.modules.render.hats.ModuleHats.modes
import net.ccbluex.liquidbounce.render.WorldRenderEnvironment
import net.ccbluex.liquidbounce.render.renderEnvironmentForWorld
import net.ccbluex.liquidbounce.render.withPositionRelativeToCamera
import net.ccbluex.liquidbounce.render.withPush
import net.ccbluex.liquidbounce.utils.entity.interpolateCurrentPosition
import net.ccbluex.liquidbounce.utils.entity.interpolateCurrentRotation
import net.minecraft.util.Mth
import net.minecraft.world.entity.EquipmentSlot
import org.joml.Quaternionf
import org.joml.Vector2f
import org.joml.Vector3f
import kotlin.math.cos
import kotlin.math.sin

private val ROTATION = Quaternionf()

/**
 * @author minecrrrr
 */
abstract class HatsMode(name: String) : Mode(name) {
    final override val parent: ModeValueGroup<*>
        get() = modes

    // --- Settings ---
    private val followRotation by boolean("FollowRotation", false)

    private class EquipOffset : ValueGroup("EquipmentOffset") {
        val equipmentOffset by float("ArmorOffset", 0.1f, 0f..1f)
    }

    private val equipOffset = tree(EquipOffset())

    private val hurtMarked by boolean("ShowDamage", true)

    private class FriendsOptions : ValueGroup("FriendsOptions") {
        val friendView by boolean("ViewOnFriend", true)
        val distance by int("Distance", 64, 8..512, "blocks")
    }

    private val friendsOptions = tree(FriendsOptions())

    protected val showInFirstPerson by boolean("FirstPersonView", true)

    // --- Render ---
    protected abstract fun WorldRenderEnvironment.drawHat(isHurt: Boolean)

    @Suppress("unused")
    private val renderHandler = handler<WorldRenderEvent> {
        val player = mc.player ?: return@handler

        for (entity in world.players()) {
            val isMe = entity == player
            val isFriend = FriendManager.isFriend(entity)
            val inDistance = player.distanceTo(entity) <= friendsOptions.distance

            val shouldRender = if (isMe) {
                !mc.options.cameraType.isFirstPerson || showInFirstPerson || ModuleFreeLook.enabled
            } else {
                inDistance && (isFriend && friendsOptions.friendView)
            }

            if (shouldRender) {
                val hurtMarked = entity.hurtTime > 0 && hurtMarked
                val pos = entity.interpolateCurrentPosition(it.partialTicks)
                val rotation = entity.interpolateCurrentRotation(it.partialTicks)

                val height = ModuleHats.HeightOffset.current()
                val equipOffset = if (!entity.getItemBySlot(EquipmentSlot.HEAD).isEmpty) {
                    equipOffset.equipmentOffset
                } else {
                    0.0F
                }

                renderEnvironmentForWorld(it.matrixStack) {
                    withPositionRelativeToCamera(pos.add(0.0, entity.eyeHeight.toDouble(), 0.0)) {
                        matrixStack.withPush {
                            if (followRotation) mulPose(rotation.toQuaternion(ROTATION))
                            translate(0F, entity.bbHeight - entity.eyeHeight + height + equipOffset, 0F)
                            drawHat(hurtMarked)
                        }
                    }
                }
            }
        }
    }

    protected fun innerI(
        innerSegments: Int,
        angles: Angles,
        radiuses: Radiuses,
        innerI: Int
    ): TorusQuad {
        val innerCurAngle = getAngle(innerI, innerSegments)
        val innerNextAngle = getNextAngle(innerI, innerSegments)

        val radii = Vector2f(radiuses.outerCurRadius, radiuses.outerNextRadius)

        val angles = TorusAngles(
            angles.outerCurAngle,
            angles.outerNextAngle,
            innerCurAngle,
            innerNextAngle,
            angles.rotationAngle,
        )
        val pos = getToroidalMeshCords(
            angles,
            radii,
            radiuses.innerRadius,
        )
        return pos
    }

    private fun getTorusPoints(
        mainAngle: Float,
        tubeAngle: Float,
        radius: Float,
        tubeRadius: Float
    ): Vector3f {
        val x = ((radius + tubeRadius * cos(tubeAngle)) * sin(mainAngle))
        val y = (tubeRadius * sin(tubeAngle))
        val z = ((radius + tubeRadius * cos(tubeAngle)) * cos(mainAngle))

        return Vector3f(x, y, z)
    }

    private fun getToroidalMeshCords(
        angles: TorusAngles,
        radii: Vector2f,
        innerRadius: Float
    ): TorusQuad {
        val currentRadius = radii.x
        val nextRadius = radii.y
        return TorusQuad(
            getTorusPoints(
                angles.outerCurrentAngle + angles.rotationAngle,
                angles.innerCurrentAngle, currentRadius, innerRadius
            ),
            getTorusPoints(
                angles.outerCurrentAngle + angles.rotationAngle,
                angles.innerNextAngle, currentRadius, innerRadius
            ),
            getTorusPoints(
                angles.outerNextAngle + angles.rotationAngle,
                angles.innerCurrentAngle, nextRadius, innerRadius
            ),
            getTorusPoints(
                angles.outerNextAngle + angles.rotationAngle,
                angles.innerNextAngle, nextRadius, innerRadius
            ),
        )
    }

    // --- Data Classes ---
    protected data class TorusQuad(
        val p1: Vector3f,
        val p2: Vector3f,
        val p3: Vector3f,
        val p4: Vector3f,
    )

    protected data class TorusAngles(
        val outerCurrentAngle: Float,
        val outerNextAngle: Float,
        val innerCurrentAngle: Float,
        val innerNextAngle: Float,
        val rotationAngle: Float,
    )

    protected data class Angles(
        val outerCurAngle: Float,
        val outerNextAngle: Float,
        val rotationAngle: Float,
    )

    protected data class Radiuses(
        val outerCurRadius: Float,
        val outerNextRadius: Float,
        val innerRadius: Float,
    )

    // Math functions

    protected fun getAngle(i: Int, segments: Int) = i * Mth.TWO_PI / segments
    protected fun getNextAngle(i: Int, segments: Int) = (i + 1) * Mth.TWO_PI / segments

    protected fun getRotationAngle(speed: Float): Float {
        return if (Mth.equal(speed, 0f)) {
            0f
        } else {
            (System.currentTimeMillis() % 360000) * 0.001F * speed
        }
    }


}
