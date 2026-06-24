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

import com.mojang.blaze3d.vertex.PoseStack
import net.ccbluex.liquidbounce.config.types.group.Mode
import net.ccbluex.liquidbounce.config.types.group.ModeValueGroup
import net.ccbluex.liquidbounce.event.events.GameRenderEvent
import net.ccbluex.liquidbounce.event.events.WorldRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.render.drawBox
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.renderEnvironmentForWorld
import net.ccbluex.liquidbounce.render.withPositionRelativeToCamera
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.render.WireframePlayer
import net.ccbluex.liquidbounce.utils.render.isCustom
import net.ccbluex.liquidbounce.utils.render.scaleLightCoords
import net.ccbluex.liquidbounce.utils.render.setPosition
import net.ccbluex.liquidbounce.utils.render.setRotation
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityAttachment
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import java.util.function.Supplier

@JvmRecord
data class BlinkEspData(val entity: Entity, val pos: Vec3, val rotation: Rotation)

sealed class BlinkEspMode(
    name: String,
    protected val getEspData: Supplier<BlinkEspData?>,
) : Mode(name)

class BlinkEspBox(
    override val parent: ModeValueGroup<*>,
    getEspData: Supplier<BlinkEspData?>,
) : BlinkEspMode("Box", getEspData) {
    private val color by color("Color", Color4b(36, 32, 147, 87))
    private val outlineColor by color("OutlineColor", Color4b(36, 32, 147, 255))

    @Suppress("unused")
    private val renderHandler = handler<WorldRenderEvent> { event ->
        val (entity, pos, rotation) = this.getEspData.get() ?: return@handler

        val dimensions = entity.getDimensions(entity.pose)
        val d = dimensions.width.toDouble() / 2.0

        val box = AABB(-d, 0.0, -d, d, dimensions.height.toDouble(), d).inflate(0.05)

        renderEnvironmentForWorld(event.matrixStack) {
            withPositionRelativeToCamera(pos) {
                drawBox(box, color, outlineColor)
            }
        }
    }
}

class BlinkEspModel(
    override val parent: ModeValueGroup<*>,
    val nametagOverride: Supplier<Component?> = Supplier { null },
    getEspData: Supplier<BlinkEspData?>,
) : BlinkEspMode("Model", getEspData) {

    private val outlineColor by color("OutlineColor", Color4b(36, 32, 147, 0))
    private val lightPercent by int("LightPercent", 60, 0..100, "%")

    private val poseStack = PoseStack()

    /**
     * @see net.minecraft.client.renderer.entity.EntityRenderer
     * @see net.minecraft.client.renderer.entity.LivingEntityRenderer
     */
    @Suppress("unused")
    private val renderHandler = handler<GameRenderEvent> { event ->
        val (entity, pos, rotation) = this.getEspData.get() ?: return@handler
        val partialTicks = 0F

        val entityRenderer = mc.entityRenderDispatcher.getRenderer(entity)

        val rs = entityRenderer.createRenderState(entity, partialTicks)

        if (!outlineColor.isTransparent) {
            rs.outlineColor = outlineColor.argb
        }

        rs.isCustom = true
        this.nametagOverride.get()?.let {
            rs.nameTag = it
            rs.nameTagAttachment = entity.attachments[EntityAttachment.NAME_TAG, 0, entity.getYRot(partialTicks)]
        }
        rs.scaleLightCoords(lightPercent * 0.01f)
        rs.setPosition(pos)
        if (rs is LivingEntityRenderState) {
            rs.setRotation(rotation)
        }

        val cameraState = mc.gameRenderer.gameRenderState().levelRenderState.cameraRenderState
        mc.entityRenderDispatcher.submit(
            rs,
            cameraState,
            rs.x - cameraState.pos.x,
            rs.y - cameraState.pos.y,
            rs.z - cameraState.pos.z,
            poseStack,
            mc.levelRenderer.submitNodeStorage,
        )
    }
}

class BlinkEspWireframe(
    override val parent: ModeValueGroup<*>,
    getEspData: Supplier<BlinkEspData?>,
) : BlinkEspMode("Wireframe", getEspData) {
    private val color by color("Color", Color4b(36, 32, 147, 87))
    private val outlineColor by color("OutlineColor", Color4b(36, 32, 147, 255))

    private val wireframePlayer = WireframePlayer()

    @Suppress("unused")
    private val renderHandler = handler<WorldRenderEvent> {
        val (entity, pos, rotation) = this.getEspData.get() ?: return@handler

        wireframePlayer.pos = pos
        wireframePlayer.pose = entity.pose
        wireframePlayer.swimAmount = (entity as? LivingEntity)?.getSwimAmount(it.partialTicks) ?: 0f
        wireframePlayer.setRotation(rotation)
        wireframePlayer.render(it, color, outlineColor)
    }
}

class BlinkEspNone(override val parent: ModeValueGroup<*>) : BlinkEspMode("None", { null })
