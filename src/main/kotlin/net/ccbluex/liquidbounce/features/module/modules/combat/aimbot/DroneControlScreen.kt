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

package net.ccbluex.liquidbounce.features.module.modules.combat.aimbot

import net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.client.asPlainText
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.client.toDegrees
import net.ccbluex.liquidbounce.utils.entity.box
import net.ccbluex.liquidbounce.utils.input.InputTracker.isPressedOnAny
import net.ccbluex.liquidbounce.utils.math.geometry.NormalizedPlane
import net.ccbluex.liquidbounce.utils.math.plus
import net.ccbluex.liquidbounce.utils.math.withLength
import net.ccbluex.liquidbounce.utils.render.WorldToScreen
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.util.Mth
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.projectile.ProjectileUtil
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec2
import net.minecraft.world.phys.Vec3
import org.joml.Vector2d
import org.lwjgl.glfw.GLFW
import kotlin.math.hypot
import kotlin.math.pow

private const val DRAG_BUTTON = 0

/**
 * Zoom by another 25% every mouse tick.
 */
private const val ZOOM_STEP_BASE = 1.25

@Suppress("detekt.TooManyFunctions")
class DroneControlScreen : Screen("BowAimbot Control Panel".asPlainText()) {

    var cameraPos = player.eyePosition.add(0.0, 10.0, 0.0)
    var cameraRotation = Vec2(Mth.wrapDegrees(player.yRot), player.xRot.coerceIn(-90.0F, 90.0F))

    private var focusedEntity: EntityFocusData? = null

    private var dragStartPos: Vector2d? = null
    private var dragStartRotation: Vec2 = Vec2.ZERO

    private var zoomSteps = 0.0

    fun getZoomFactor(): Float {
        return ZOOM_STEP_BASE.pow(zoomSteps).toFloat()
    }

    override fun mouseDragged(click: MouseButtonEvent, offsetX: Double, offsetY: Double): Boolean {
        val mX = click.x
        val mY = click.y
        val dragStart = this.dragStartPos

        if (click.button() != DRAG_BUTTON || dragStart == null) {
            return false
        }

        val prevWorldRay = WorldToScreen.calculateMouseRay(Vec2(mX.toFloat(), mY.toFloat()))
        val newWorldRay = WorldToScreen.calculateMouseRay(Vec2(dragStart.x.toFloat(), dragStart.y.toFloat()))

        val yawDelta = Vector2d(newWorldRay.direction.x, newWorldRay.direction.z).angle(
            Vector2d(
                prevWorldRay.direction.x,
                prevWorldRay.direction.z
            )
        ).toFloat().toDegrees()

        val pitchDelta =
            Vector2d(newWorldRay.direction.y, hypot(newWorldRay.direction.x, newWorldRay.direction.z)).angle(
                Vector2d(
                    prevWorldRay.direction.y,
                    hypot(prevWorldRay.direction.x, prevWorldRay.direction.z)
                )
            ).toFloat().toDegrees()

        this.cameraRotation = this.dragStartRotation.add(Vec2(-yawDelta, -pitchDelta))

        return true
    }

    override fun keyPressed(input: KeyEvent): Boolean {
        if (input.key == GLFW.GLFW_KEY_SPACE) {
            ModuleDroneControl.mayShoot = true
        }

        return super.keyPressed(input)
    }

    override fun keyReleased(input: KeyEvent): Boolean {
        return super.keyReleased(input)
    }

    override fun mouseClicked(click: MouseButtonEvent, doubled: Boolean): Boolean {
        if (click.button() == DRAG_BUTTON) {
            this.dragStartPos = Vector2d(click.x, click.y)
            this.dragStartRotation = this.cameraRotation
        }

        return true
    }

    override fun mouseScrolled(
        mouseX: Double,
        mouseY: Double,
        horizontalAmount: Double,
        verticalAmount: Double
    ): Boolean {
        this.zoomSteps += verticalAmount

        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)
    }

    override fun mouseMoved(mouseX: Double, mouseY: Double) {
        val focusedEntity = this.focusedEntity

        if (mc.options.keyShift.isPressedOnAny && focusedEntity != null) {
            val rot = Rotation.lookingAt(point = focusedEntity.entity.box.center, from = this.cameraPos)

            this.cameraRotation = Vec2(rot.yaw, rot.pitch)
        }
    }

    override fun mouseReleased(click: MouseButtonEvent): Boolean {
        val button = click.button()

        if (button == DRAG_BUTTON) {
            this.dragStartPos = null
        }

        if (button != 1) {
            return true
        }

        val mouseX = click.x
        val mouseY = click.y

        val line = WorldToScreen.calculateMouseRay(Vec2(mouseX.toFloat(), mouseY.toFloat()))

        val startPos = line.position
        val endPos = startPos + line.direction.withLength(10000.0)

        val target = ProjectileUtil.getEntityHitResult(
            player,
            startPos,
            endPos,
            AABB.ofSize(startPos, 0.1, 0.1, 0.1).expandTowards(line.direction.withLength(10000.0)),
            { true },
            10000.0
        )

        this.focusedEntity = target?.let {
            EntityFocusData(it.entity, it.location.y, it.location.y - it.entity.position().y)
        }

        return super.mouseReleased(click)
    }

    override fun render(context: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        ModuleDroneControl.currentTarget = null

        this.focusedEntity?.let {
            ModuleDebug.debugGeometry(
                ModuleProjectileAimbot,
                "focusEntity",
                ModuleDebug.DebuggedBox(it.entity.box, Color4b.RED.with(a = 127))
            )

            val plane = NormalizedPlane(Vec3(0.0, it.baseY, 0.0), Vec3(0.0, 1.0, 0.0))
            val intersect = plane.intersection(
                WorldToScreen.calculateMouseRay(
                    Vec2(mouseX.toFloat(), mouseY.toFloat()),
                    cameraPos = this.cameraPos
                )
            )!!

            val entityPos = intersect.subtract(0.0, it.hitBoxOffsetY, 0.0)

            ModuleDroneControl.currentTarget = it.entity to entityPos

            ModuleDebug.debugGeometry(
                ModuleProjectileAimbot,
                "focusEntity",
                ModuleDebug.DebuggedBox(
                    it.entity.dimensions.makeBoundingBox(entityPos),
                    Color4b.RED.with(a = 127)
                )
            )
        }
    }

    @Suppress("detekt:EmptyFunctionBlock")
    override fun renderBackground(context: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {

    }

    override fun onClose() {
        ModuleDroneControl.enabled = false
    }

    override fun shouldCloseOnEsc(): Boolean {
        return true
    }

    override fun isPauseScreen(): Boolean {
        return false
    }

    private class EntityFocusData(val entity: Entity, val baseY: Double, val hitBoxOffsetY: Double)
}
