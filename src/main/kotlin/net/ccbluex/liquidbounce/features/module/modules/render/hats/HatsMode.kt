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

import net.ccbluex.liquidbounce.config.types.nesting.Choice
import net.ccbluex.liquidbounce.config.types.nesting.ChoiceConfigurable
import net.ccbluex.liquidbounce.config.types.nesting.Configurable
import net.ccbluex.liquidbounce.event.events.WorldRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.misc.FriendManager
import net.ccbluex.liquidbounce.features.module.modules.render.hats.ModuleHats.modes
import net.ccbluex.liquidbounce.features.module.modules.render.hats.utils.Angles
import net.ccbluex.liquidbounce.features.module.modules.render.hats.utils.Radiuses
import net.ccbluex.liquidbounce.features.module.modules.render.hats.utils.TorusAngles
import net.ccbluex.liquidbounce.features.module.modules.render.hats.utils.TorusQuad
import net.ccbluex.liquidbounce.features.module.modules.render.hats.utils.getAngle
import net.ccbluex.liquidbounce.features.module.modules.render.hats.utils.getNextAngle
import net.ccbluex.liquidbounce.features.module.modules.render.hats.utils.getToroidalMeshCords
import net.ccbluex.liquidbounce.render.WorldRenderEnvironment
import net.ccbluex.liquidbounce.render.renderEnvironmentForWorld
import net.ccbluex.liquidbounce.render.withPositionRelativeToCamera
import net.ccbluex.liquidbounce.utils.entity.interpolateCurrentPosition
import net.minecraft.world.entity.EquipmentSlot
import org.joml.Vector2f
import com.mojang.blaze3d.shaders.UniformType

/**
 * @author minecrrrr
 */
abstract class HatsMode(name: String) : Choice(name) {
    // --- Settings ---
    protected val height by float("HeightOffset", 0.1f, 0f..2f)
    protected object EquipOffset : Configurable("EquipmentOffset") {
        val equipmentOffset by float("ArmorOffset", 0.1f, 0f..1f)
    }
    val hurtMarked by boolean("ShowDamage", true)
    protected object FriendsOptions : Configurable("FriendsOptions") {
        val friendView by boolean("ViewOnFriend", true)
        val distance by int("Distance", 64, 8..512, "blocks")
    }
    protected val showInFirstPerson by boolean("FirstPersonView", true)

    final override val parent: ChoiceConfigurable<*>
        get() = modes

    init {
        tree(FriendsOptions)
    }

    // --- Render ---
    protected abstract fun WorldRenderEnvironment.drawHat(isHurt: Boolean)

    @Suppress("unused")
    private val renderHandler = handler<WorldRenderEvent> {
        val world = net.ccbluex.liquidbounce.utils.client.world
        val player = mc.player ?: return@handler

        for (entity in world.players()) {
            val isMe = entity == player
            val isFriend = FriendManager.isFriend(entity)
            val inDistance = player.distanceTo(entity) <= FriendsOptions.distance

            val shouldRender = if (isMe) {
                !mc.options.cameraType.isFirstPerson || showInFirstPerson
            } else {
                inDistance && (isFriend && FriendsOptions.friendView)
            }

            if (shouldRender) {

                val hurtMarked = entity.hurtTime > 0 && hurtMarked
                val pos = entity.interpolateCurrentPosition(it.partialTicks)

                val equipOffset = if (!entity.getItemBySlot(EquipmentSlot.HEAD).isEmpty) {
                    EquipOffset.equipmentOffset.toDouble()
                } else {
                    0.0
                }

                renderEnvironmentForWorld(it.matrixStack) {
                    withPositionRelativeToCamera(pos.add(0.0, entity.bbHeight + height.toDouble() + equipOffset, 0.0)) {
                        drawHat(hurtMarked)
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

}
