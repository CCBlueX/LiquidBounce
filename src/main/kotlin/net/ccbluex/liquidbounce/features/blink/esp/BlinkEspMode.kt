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

package net.ccbluex.liquidbounce.features.blink.esp

import net.ccbluex.liquidbounce.config.types.group.Mode
import net.ccbluex.liquidbounce.config.types.group.ModeValueGroup
import net.ccbluex.liquidbounce.event.events.WorldRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.render.drawBox
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.renderEnvironmentForWorld
import net.ccbluex.liquidbounce.render.withPositionRelativeToCamera
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.client.floorToInt
import net.ccbluex.liquidbounce.utils.render.WireframePlayer
import net.minecraft.client.renderer.LightTexture
import net.minecraft.world.entity.Entity
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3

@JvmRecord
data class BlinkEspData(val entity: Entity, val pos: Vec3, val rotation: Rotation)

sealed class BlinkEspMode(
    name: String,
    protected val getEspData: () -> BlinkEspData?
) : Mode(name)

class BlinkEspBox(
    override val parent: ModeValueGroup<*>,
    getEspData: () -> BlinkEspData?,
) : BlinkEspMode("Box", getEspData) {
    private val color by color("Color", Color4b(36, 32, 147, 87))

    @Suppress("unused")
    private val renderHandler = handler<WorldRenderEvent> { event ->
        val (entity, pos, rotation) = this.getEspData() ?: return@handler

        val dimensions = entity.getDimensions(entity.pose)
        val d = dimensions.width.toDouble() / 2.0

        val box = AABB(-d, 0.0, -d, d, dimensions.height.toDouble(), d).inflate(0.05)

        renderEnvironmentForWorld(event.matrixStack) {
            withPositionRelativeToCamera(pos) {
                drawBox(box, color)
            }
        }
    }
}

class BlinkEspModel(
    override val parent: ModeValueGroup<*>,
    getEspData: () -> BlinkEspData?,
) : BlinkEspMode("Model", getEspData) {
    private val lightAmount by float("LightAmount", 0.3f, 0.01f..1f)

    @Suppress("unused")
    private val renderHandler = handler<WorldRenderEvent> { event ->
        val (entity, pos, rotation) = this.getEspData() ?: return@handler

        val entityRenderer = mc.entityRenderDispatcher.getRenderer(entity)

        val rs = entityRenderer.createRenderState(entity, event.partialTicks)

        val originalBlockLight = LightTexture.block(rs.lightCoords)
        val originalSkyLight = LightTexture.sky(rs.lightCoords)
        rs.lightCoords = LightTexture.pack(
            (originalBlockLight * lightAmount).floorToInt(),
            (originalSkyLight * lightAmount).floorToInt(),
        )
        rs.x = pos.x
        rs.y = pos.y
        rs.z = pos.z
        val cameraState = mc.gameRenderer.levelRenderState.cameraRenderState
        rs.distanceToCameraSq = pos.distanceToSqr(cameraState.pos)

        // TODO(1.21.10-port): position & light incorrect
        mc.entityRenderDispatcher.submit(
            rs,
            cameraState,
            rs.x - cameraState.pos.x,
            rs.y - cameraState.pos.y,
            rs.z - cameraState.pos.z,
            event.matrixStack,
            mc.gameRenderer.submitNodeStorage,
        )
    }
}

class BlinkEspWireframe(
    override val parent: ModeValueGroup<*>,
    getEspData: () -> BlinkEspData?,
) : BlinkEspMode("Wireframe", getEspData) {
    private val color by color("Color", Color4b(36, 32, 147, 87))
    private val outlineColor by color("OutlineColor", Color4b(36, 32, 147, 255))

    @Suppress("unused")
    private val renderHandler = handler<WorldRenderEvent> {
        val (entity, pos, rotation) = this.getEspData() ?: return@handler

        val wireframePlayer = WireframePlayer(pos, rotation.yaw, rotation.pitch)
        wireframePlayer.render(it, color, outlineColor)
    }
}

class BlinkEspNone(override val parent: ModeValueGroup<*>) : BlinkEspMode("None", { null })
