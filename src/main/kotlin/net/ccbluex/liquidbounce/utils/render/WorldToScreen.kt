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

import com.mojang.blaze3d.systems.RenderSystem
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.events.WorldRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.MinecraftShortcuts
import net.ccbluex.liquidbounce.features.module.modules.combat.aimbot.ModuleProjectileAimbot
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug.debugParameter
import net.ccbluex.liquidbounce.render.engine.type.Vec3f
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention.READ_FINAL_STATE
import net.ccbluex.liquidbounce.utils.math.geometry.Line
import net.ccbluex.liquidbounce.utils.math.set
import net.ccbluex.liquidbounce.utils.math.sub
import net.ccbluex.liquidbounce.utils.math.toVec3d
import net.minecraft.world.phys.Vec2
import net.minecraft.world.phys.Vec3
import org.joml.Matrix4f
import org.joml.Vector3f
import java.text.NumberFormat

/**
 * This util should only be called from main thread
 */
object WorldToScreen : MinecraftShortcuts, EventListener {

    private val mvpMatrix = Matrix4f()
    private val projectionMatrix = Matrix4f()

    private val cacheMatrix = Matrix4f()
    private val cacheVec3f = Vector3f()

    @Suppress("unused")
    private val renderHandler = handler<WorldRenderEvent>(priority = READ_FINAL_STATE) { event ->
        val matrixStack = event.matrixStack

        this.mvpMatrix.set(matrixStack.last().pose())

        // Important: here we need this buffer to be USAGE_MAP_READ, so add mixins at all sources.
        // Usages (2025/11/09, 1.21.6):
        // - PostEffectPass
        // - CubeMapRenderer
        // - GuiRenderer
        // - SpecialGuiElementRenderer
        // - GameRenderer -> renderWorld (we need this within event callback) (@see MixinRawProjectionMatrix)
        val projMat = RenderSystem.getProjectionMatrixBuffer() ?: return@handler

        projMat.mapBuffer(read = true, write = false).use {
            this.projectionMatrix.set(it.data())
        }
    }

    @JvmStatic
    @JvmOverloads
    fun calculateScreenPos(
        pos: Vec3,
        cameraPos: Vec3 = mc.gameRenderer.mainCamera.position(),
    ): Vec3f? {
        val transformedPos = cacheVec3f.set(pos).sub(cameraPos)
            .mulProject(cacheMatrix.set(projectionMatrix).mul(mvpMatrix))

        val scaleFactor = mc.window.guiScale
        val guiScaleMul = 0.5f / scaleFactor.toFloat()

        val screenPos = transformedPos.mul(1.0F, -1.0F, 1.0F).add(1.0F, 1.0F, 0.0F)
            .mul(guiScaleMul * mc.mainRenderTarget.width, guiScaleMul * mc.mainRenderTarget.height, 1.0F)

        return if (transformedPos.z < 1.0F) Vec3f(screenPos.x, screenPos.y, transformedPos.z) else null
    }

    @JvmStatic
    @JvmOverloads
    fun calculateMouseRay(posOnScreen: Vec2, cameraPos: Vec3 = mc.gameRenderer.mainCamera.position()): Line {
        val screenVec = cacheVec3f.set(posOnScreen.x, posOnScreen.y, 1.0F)

        val scaleFactor = mc.window.guiScale
        val guiScaleMul = 0.5f / scaleFactor.toFloat()

        val transformedPos = screenVec.mul(
            1.0F / (guiScaleMul * mc.mainRenderTarget.width),
            1.0F / (guiScaleMul * mc.mainRenderTarget.height),
            1.0F
        ).sub(1.0F, 1.0F, 0.0F).mul(1.0F, -1.0F, 1.0F)

        val relativePos = cacheVec3f.set(transformedPos)
            .mulProject(cacheMatrix.set(projectionMatrix).mul(mvpMatrix).invert())

        ModuleProjectileAimbot.debugParameter("s2w") {
            relativePos.toString(NumberFormat.getInstance())
        }

        return Line(cameraPos, relativePos.toVec3d())
    }

}
