package net.ccbluex.liquidbounce.features.module.modules.combat.aimbot

import net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.client.asText
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.client.toDegrees
import net.ccbluex.liquidbounce.utils.entity.box
import net.ccbluex.liquidbounce.utils.input.InputTracker.isPressedOnAny
import net.ccbluex.liquidbounce.utils.math.geometry.NormalizedPlane
import net.ccbluex.liquidbounce.utils.math.plus
import net.ccbluex.liquidbounce.utils.render.WorldToScreen
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.Screen
import net.minecraft.entity.Entity
import net.minecraft.entity.projectile.ProjectileUtil
import net.minecraft.util.math.Box
import net.minecraft.util.math.MathHelper
import net.minecraft.util.math.Vec2f
import net.minecraft.util.math.Vec3d
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
class DroneControlScreen : Screen("BowAimbot Control Panel".asText()) {

    var cameraPos = player.eyePos.add(0.0, 10.0, 0.0)
    var cameraRotation = Vec2f(MathHelper.wrapDegrees(player.yaw), player.pitch.coerceIn(-90.0F, 90.0F))

    private var focusedEntity: EntityFocusData? = null

    private var dragStartPos: Vector2d? = null
    private var dragStartRottion: Vec2f = Vec2f(0.0F, 0.0F)

    private var zoomSteps = 0.0

    fun getZoomFactor(): Float {
        return ZOOM_STEP_BASE.pow(zoomSteps).toFloat()
    }

    override fun mouseDragged(mouseX: Double, mouseY: Double, button: Int, deltaX: Double, deltaY: Double): Boolean {
        val dragStart = this.dragStartPos

        if (button != DRAG_BUTTON || dragStart == null) {
            return false
        }

        val prevWorldRay = WorldToScreen.calculateMouseRay(Vec2f(mouseX.toFloat(), mouseY.toFloat()))
        val newWorldRay = WorldToScreen.calculateMouseRay(Vec2f(dragStart.x.toFloat(), dragStart.y.toFloat()))

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

        this.cameraRotation = this.dragStartRottion.add(Vec2f(-yawDelta, -pitchDelta))

        return true
    }

    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        if (keyCode == GLFW.GLFW_KEY_SPACE) {
            ModuleDroneControl.mayShoot = true
        }

        return super.keyPressed(keyCode, scanCode, modifiers)
    }

    override fun keyReleased(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        return super.keyReleased(keyCode, scanCode, modifiers)
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (button == DRAG_BUTTON) {
            this.dragStartPos = Vector2d(mouseX, mouseY)
            this.dragStartRottion = this.cameraRotation
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

        if (mc.options.sneakKey.isPressedOnAny && focusedEntity != null) {
            val rot = Rotation.lookingAt(point = focusedEntity.entity.box.center, from = this.cameraPos)

            this.cameraRotation = Vec2f(rot.yaw, rot.pitch)
        }
    }

    override fun mouseReleased(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (button == DRAG_BUTTON) {
            this.dragStartPos = null
        }

        if (button != 1) {
            return true
        }

        val line = WorldToScreen.calculateMouseRay(Vec2f(mouseX.toFloat(), mouseY.toFloat()))

        val startPos = line.position
        val endPos = startPos + line.direction.normalize().multiply(10000.0)

        val target = ProjectileUtil.raycast(
            player,
            startPos,
            endPos,
            Box.of(startPos, 0.1, 0.1, 0.1).stretch(line.direction.normalize().multiply(10000.0)),
            { true },
            10000.0
        )

        this.focusedEntity = target?.let { EntityFocusData(it.entity, it.pos.y, it.pos.y - it.entity.pos.y) }

        return super.mouseReleased(mouseX, mouseY, button)
    }

    override fun render(context: DrawContext?, mouseX: Int, mouseY: Int, delta: Float) {
        ModuleDroneControl.currentTarget = null

        this.focusedEntity?.let {
            ModuleDebug.debugGeometry(
                ModuleProjectileAimbot,
                "focusEntity",
                ModuleDebug.DebuggedBox(it.entity.box, Color4b.RED.with(a = 127))
            )

            val plane = NormalizedPlane(Vec3d(0.0, it.baseY, 0.0), Vec3d(0.0, 1.0, 0.0))
            val intersect = plane.intersection(
                WorldToScreen.calculateMouseRay(
                    Vec2f(mouseX.toFloat(), mouseY.toFloat()),
                    cameraPos = this.cameraPos
                )
            )!!

            val entityPos = intersect.subtract(0.0, it.hitBoxOffsetY, 0.0)

            ModuleDroneControl.currentTarget = it.entity to entityPos

            ModuleDebug.debugGeometry(
                ModuleProjectileAimbot,
                "focusEntity",
                ModuleDebug.DebuggedBox(
                    it.entity.dimensions.getBoxAt(entityPos),
                    Color4b.RED.with(a = 127)
                )
            )
        }
    }

    @Suppress("detekt:EmptyFunctionBlock")
    override fun renderBackground(context: DrawContext?, mouseX: Int, mouseY: Int, delta: Float) {

    }

    override fun close() {
        ModuleDroneControl.enabled = false
    }

    override fun shouldCloseOnEsc(): Boolean {
        return true
    }

    override fun shouldPause(): Boolean {
        return false
    }

    class EntityFocusData(val entity: Entity, val baseY: Double, val hitBoxOffsetY: Double)
}
