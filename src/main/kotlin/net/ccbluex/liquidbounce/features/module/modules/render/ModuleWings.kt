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

package net.ccbluex.liquidbounce.features.module.modules.render

import com.mojang.math.Axis
import net.ccbluex.liquidbounce.config.types.group.ValueGroup
import net.ccbluex.liquidbounce.event.events.WorldRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.misc.FriendManager
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.render.ClientRenderPipelines
import net.ccbluex.liquidbounce.render.drawCustomMesh
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.renderEnvironment
import net.ccbluex.liquidbounce.render.setColor
import net.ccbluex.liquidbounce.render.withPositionRelativeToCamera
import net.ccbluex.liquidbounce.render.withPush
import net.ccbluex.liquidbounce.utils.entity.interpolateCurrentRotation
import net.minecraft.util.Mth
import net.minecraft.world.phys.Vec3
import org.joml.Matrix4f

object ModuleWings : ClientModule("Wings", ModuleCategories.RENDER) {

    private val color by color("color", Color4b.LIQUID_BOUNCE)
    private object WingsOptions : ValueGroup("wingsVisual") {
        val wingsLength by float("wingsLength", 1f, 0.1f..2f)
        val wingsHeight by float("wingsHeight", 0.3f, 0.1f..1f)
        val fadeStartRatio by float("fadeStartRatio", 0.5f, 0f..1f)
    }
    private object WingsPosition : ValueGroup("wingsPosition") {
        val wingsHeight by float("wingsHeight", 0.2f, -1f..1f)
        val behindScale by float("behindScale", 0.25f, 0f..1f)
    }

    val showDamage by boolean("ShowDamage", true)
    val showInFirstPerson by boolean("ShowInFirstPerson", false)

    private class FriendsOptions : ValueGroup("FriendsOptions") {
        val friendView by boolean("ViewOnFriend", true)
        val distance by int("Distance", 64, 8..512, "blocks")
    }
    init {
        tree(WingsOptions)
        tree(WingsPosition)
        tree(FriendsOptions())
    }

    @Suppress("unused")
    private val renderHandler = handler<WorldRenderEvent> { event ->
        val player = mc.player ?: return@handler

        event.renderEnvironment {
            for (entity in world.players()) {
                val renderFriend =
                    FriendsOptions().friendView
                        && FriendManager.isFriend(entity)
                        && player.distanceTo(entity) <= FriendsOptions().distance
                if (entity != player && !renderFriend) continue
                if (entity == player && mc.options.cameraType.isFirstPerson && showInFirstPerson) continue
                val currentColor = when(showDamage && entity.hurtTime != 0) {
                    true -> Color4b.RED.alpha(color.a)
                    false -> color
                }

                val bodyRot = Mth.rotLerp(event.partialTicks, entity.yBodyRotO, entity.yBodyRot)
                val rot = entity.interpolateCurrentRotation(event.partialTicks)
                val look = Vec3.directionFromRotation(0f, rot.yRot)
                val pos = entity.getPosition(event.partialTicks).subtract(look.scale(WingsPosition.behindScale.toDouble()))

                withPositionRelativeToCamera(pos.add(0.0, entity.bbHeight / 2.0 + 0.2 + WingsPosition.wingsHeight, 0.0)) {
                    poseStack.withPush {

                        fun drawQuadSegment(
                            m: Matrix4f,
                            x1: Float,
                            x2: Float,
                            height: Float,
                            c1: Color4b,
                            c2: Color4b
                        ) {
                            drawCustomMesh(ClientRenderPipelines.triangles(false)) { pose ->
                                val h2 = height / 2f
                                addVertex(m, x1, 0f, -h2).setColor(c1)
                                addVertex(m, x2, 0f, -h2).setColor(c2)
                                addVertex(m, x2, 0f, h2).setColor(c2)

                                addVertex(m, x1, 0f, -h2).setColor(c1)
                                addVertex(m, x2, 0f, h2).setColor(c2)
                                addVertex(m, x1, 0f, h2).setColor(c1)
                            }
                        }

                        fun drawWingPair(
                            firstX: Float,
                            height: Float,
                            secondX: Float = firstX,
                        ) {
                            drawCustomMesh(ClientRenderPipelines.triangles(false)) { pose ->
                                val m = pose.pose()
                                val transparent = currentColor.alpha(0)

                                val rSolid = firstX * WingsOptions.fadeStartRatio
                                drawQuadSegment(m, 0f, rSolid, height, currentColor, currentColor)
                                drawQuadSegment(m, rSolid, firstX, height, currentColor, transparent)

                                val lSolid = -secondX * WingsOptions.fadeStartRatio
                                drawQuadSegment(m, 0f, lSolid, height, currentColor, currentColor)
                                drawQuadSegment(m, lSolid, -secondX, height, currentColor, transparent)
                            }
                        }

                        mulPose(Axis.XP.rotationDegrees(-90f))
                        mulPose(Axis.ZP.rotationDegrees(-bodyRot))

                        drawWingPair(WingsOptions.wingsLength, WingsOptions.wingsHeight)

                        poseStack.withPush {
                            translate(0.1, 0.0, -0.1)
                            mulPose(Axis.YP.rotationDegrees(45f))
                            drawWingPair(
                                WingsOptions.wingsLength - 0.25f,
                                WingsOptions.wingsHeight,
                                secondX = WingsOptions.wingsLength
                            )
                        }

                        poseStack.withPush {
                            translate(-0.1, 0.0, -0.1)
                            mulPose(Axis.YP.rotationDegrees(-45f))
                            drawWingPair(
                                WingsOptions.wingsLength,
                                WingsOptions.wingsHeight,
                                secondX = WingsOptions.wingsLength - 0.25f
                            )
                        }
                    }
                }
            }
        }
    }
}
