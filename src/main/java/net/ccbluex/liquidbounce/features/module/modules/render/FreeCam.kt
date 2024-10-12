/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.MovementUtils.isMoving
import net.ccbluex.liquidbounce.utils.MovementUtils.strafe
import net.ccbluex.liquidbounce.utils.extensions.*
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.util.Vec3

object FreeCam : Module("FreeCam", Category.RENDER, gameDetecting = false, hideModule = false) {

    private val speed by FloatValue("Speed", 0.8f, 0.1f..2f)

    private val allowCameraInteract by BoolValue("AllowCameraInteract", true)
    private val allowRotationChange by BoolValue("AllowRotationChange", true)

    data class PositionPair(var pos: Vec3, var lastPos: Vec3, var extraPos: Vec3 = lastPos) {
        operator fun plusAssign(velocity: Vec3) {
            lastPos = pos
            pos += velocity
        }

        fun interpolate(tickDelta: Float) = Vec3(
            lastPos.xCoord + (pos.xCoord - lastPos.xCoord) * tickDelta,
            lastPos.yCoord + (pos.yCoord - lastPos.yCoord) * tickDelta,
            lastPos.zCoord + (pos.zCoord - lastPos.zCoord) * tickDelta
        )

    }

    override fun onEnable() {
        updatePosition(Vec3_ZERO)
    }

    override fun onDisable() {
        pos = null
        originalPos = null
    }

    @EventTarget
    fun onInputEvent(event: MovementInputEvent) {
        val speed = this.speed.toDouble()

        val yAxisMovement = when {
            event.originalInput.jump -> 1.0f
            event.originalInput.sneak -> -1.0f
            else -> 0.0f
        }

        val velocity = Vec3_ZERO.apply {
            strafe(speed = speed, moveCheck = !isMoving)

            this.yCoord = yAxisMovement * speed
        }

        updatePosition(velocity)

        event.originalInput.reset()
    }

    private var originalPos: PositionPair? = null
    private var pos: PositionPair? = null

    private fun updatePosition(velocity: Vec3) {
        val player = mc.thePlayer ?: return

        pos = (pos ?: PositionPair(player.currPos, player.currPos)).apply { this += velocity }
    }

    fun useModifiedPosition() {
        val player = mc.thePlayer ?: return

        originalPos = PositionPair(player.currPos, player.prevPos, player.lastTickPos)

        val event = CameraPositionEvent(player.currPos, player.prevPos, player.lastTickPos)
        EventManager.callEvent(event)

        event.result?.run {
            player.setPosAndPrevPos(pos, lastPos, extraPos)
            return
        }

        val data = pos ?: return

        player.setPosAndPrevPos(data.pos, data.lastPos, data.lastPos)
    }

    fun restoreOriginalPosition() {
        val player = mc.thePlayer ?: return

        originalPos?.run { player.setPosAndPrevPos(pos, lastPos, extraPos) }
    }

    fun renderPlayerFromAllPerspectives(entity: EntityLivingBase) =
        state && entity == mc.thePlayer || entity.isPlayerSleeping

    fun modifyRaycast(original: Vec3, entity: Entity, tickDelta: Float): Vec3 {
        if (!state || entity != mc.thePlayer || !allowCameraInteract) {
            return original
        }

        return pos?.interpolate(tickDelta)?.apply { yCoord += entity.eyeHeight } ?: original
    }

    fun shouldDisableRotations() = state && !allowRotationChange

    @EventTarget
    fun onWorldChange(event: WorldEvent) {
        // Disable when world changed
        state = false
    }

}