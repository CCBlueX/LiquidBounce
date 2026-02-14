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

package net.ccbluex.liquidbounce.utils.render

import net.ccbluex.liquidbounce.interfaces.EntityRenderStateAddition
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.client.mc
import net.minecraft.client.renderer.LightTexture
import net.minecraft.client.renderer.entity.state.EntityRenderState
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState
import net.minecraft.core.Position
import net.minecraft.util.Mth
import net.minecraft.world.entity.Entity
import kotlin.math.roundToInt

inline val EntityRenderState.entity: Entity?
    get() = (this as EntityRenderStateAddition).`liquid_bounce$getEntity`()

inline var EntityRenderState.isCustom: Boolean
    get() = (this as EntityRenderStateAddition).`liquid_bounce$isCustom`()
    set(value) = (this as EntityRenderStateAddition).`liquid_bounce$setCustom`(value)

fun EntityRenderState.scaleLightCoords(scale: Float) {
    val block = (LightTexture.block(this.lightCoords) * scale).roundToInt().coerceIn(0, 15)
    val sky = (LightTexture.sky(this.lightCoords) * scale).roundToInt().coerceIn(0, 15)
    this.lightCoords = LightTexture.pack(block, sky)
}

fun EntityRenderState.setPosition(position: Position) {
    this.x = position.x()
    this.y = position.y()
    this.z = position.z()
    val cameraPos = mc.gameRenderer.levelRenderState.cameraRenderState.pos
    this.distanceToCameraSq = cameraPos.distanceToSqr(this.x, this.y, this.z)
}

fun LivingEntityRenderState.setRotation(rotation: Rotation) =
    setRotation(rotation.xRot, rotation.yRot)

fun LivingEntityRenderState.setRotation(xRot: Float, yRot: Float) {
    this.bodyRot = yRot
    this.yRot = Mth.wrapDegrees(yRot - this.bodyRot)
    this.xRot = xRot
}
