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
package net.ccbluex.liquidbounce.utils.aiming

import net.ccbluex.liquidbounce.config.types.group.ValueGroup
import net.ccbluex.liquidbounce.config.types.list.Tagged
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.events.MouseRotationEvent
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.PlayerVelocityStrafe
import net.ccbluex.liquidbounce.event.events.RotationUpdateEvent
import net.ccbluex.liquidbounce.event.events.TransferOrigin
import net.ccbluex.liquidbounce.event.events.WorldChangeEvent
import net.ccbluex.liquidbounce.event.events.WorldRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.addon.AddonApi
import net.ccbluex.liquidbounce.features.blink.BlinkManager
import net.ccbluex.liquidbounce.features.module.modules.combat.backtrack.ModuleBacktrack
import net.ccbluex.liquidbounce.features.module.modules.movement.ModuleFreeze
import net.ccbluex.liquidbounce.render.drawBox
import net.ccbluex.liquidbounce.render.drawLine
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.engine.type.Vec3f
import net.ccbluex.liquidbounce.render.renderEnvironment
import net.ccbluex.liquidbounce.utils.aiming.aim.Aim
import net.ccbluex.liquidbounce.utils.aiming.aim.AimGoal
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.aiming.data.Surface
import net.ccbluex.liquidbounce.utils.aiming.features.MovementCorrection
import net.ccbluex.liquidbounce.utils.aiming.utils.RotationUtil
import net.ccbluex.liquidbounce.utils.aiming.utils.RotationUtil.angleDifference
import net.ccbluex.liquidbounce.utils.aiming.utils.setRotation
import net.ccbluex.liquidbounce.utils.aiming.utils.withFixedYaw
import net.ccbluex.liquidbounce.utils.client.RequestHandler
import net.ccbluex.liquidbounce.utils.client.inGame
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.combat.CombatManager
import net.ccbluex.liquidbounce.utils.entity.lastRotation
import net.ccbluex.liquidbounce.utils.entity.rotation
import net.ccbluex.liquidbounce.utils.inventory.InventoryManager
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention.FIRST_PRIORITY
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention.MODEL_STATE
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.ccbluex.liquidbounce.utils.math.fma
import net.ccbluex.liquidbounce.utils.math.toVec3f
import net.minecraft.client.gui.screens.inventory.ContainerScreen
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket
import net.minecraft.network.protocol.game.ClientboundPlayerRotationPacket
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket
import net.minecraft.network.protocol.game.ServerboundUseItemPacket
import net.minecraft.util.Mth
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.Relative
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * A rotation manager. Will also actively affect our F5 model.
 */
@Suppress("TooManyFunctions")
object RotationManager : ValueGroup("RotationManager"), EventListener {

    private val rotationTargetHandler = RequestHandler<RotationTarget>()

    /**
     * The modules waiting in [lookAt] for the view.
     */
    internal val looks = Looks()

    /** The targets of this tick, most important first. */
    private val requests = ArrayList<RotationTarget>()
    private var lastAim: Aim? = null
    private var yawFollowsPlayer = false
    private var pitchFollowsPlayer = false

    @Suppress("unused")
    private val lifecycleListener = object : EventListener {
        private val worldChangeHandler = handler<WorldChangeEvent> { reset() }
    }

    val activeRotationTarget: RotationTarget?
        get() = rotationTargetHandler.getActiveRequestValue() ?: previousRotationTarget
    internal var previousRotationTarget: RotationTarget? = null
        private set

    /**
     * The rotation we want to aim at. This DOES NOT mean that the server already received this rotation.
     */
    @AddonApi
    var currentRotation: Rotation? = null
        private set(value) {
            lastRotation = if (value == null) {
                null
            } else {
                field ?: mc.player?.rotation ?: Rotation.ZERO
            }

            field = value
        }

    // Used for rotation interpolation
    var lastRotation: Rotation? = null
        private set

    // currentRotation of the previous tick. Unlike lastRotation it outlives a reset by one tick, so the
    // F5 model turns back to the vanilla rotation instead of snapping to it.
    private var lastModelRotation: Rotation? = null

    private val fakeLagging
        get() = BlinkManager.isLagging || ModuleBacktrack.isLagging()

    private val freezing
        get() = ModuleFreeze.running

    @AddonApi
    val serverRotation: Rotation
        get() = if (fakeLagging || freezing) theoreticalServerRotation else actualServerRotation

    /**
     * Yaw used by [net.minecraft.world.entity.Entity.moveRelative] after movement correction.
     */
    val movementYaw: Float
        get() = resolveMovementYaw(
            playerYaw = player.yRot,
            managedYaw = currentRotation?.yaw ?: Float.NaN,
            movementCorrection = activeRotationTarget?.movementCorrection,
        )

    /**
     * The rotation that was already sent to the server and is currently active.
     * The value is not being written by the packets, but we gather the Rotation from the last yaw and pitch variables
     * from our player instance handled by the sendMovementPackets() function.
     */
    var actualServerRotation = Rotation.ZERO
        private set

    private var theoreticalServerRotation = Rotation.ZERO

    private fun reset() {
        rotationTargetHandler.clear()
        requests.clear()
        previousRotationTarget = null
        lastAim = null
        yawFollowsPlayer = false
        pitchFollowsPlayer = false
        currentRotation = null
        lastRotation = null
        lastModelRotation = null
        actualServerRotation = Rotation.ZERO
        theoreticalServerRotation = Rotation.ZERO
    }

    /**
     * Applies the rotation normalization performed by the vanilla server for rotation-bearing
     * movement and item-use packets.
     *
     * @see net.minecraft.server.network.ServerGamePacketListenerImpl.handleMovePlayer
     * @see net.minecraft.server.network.ServerGamePacketListenerImpl.handleUseItem
     */
    private fun serverboundRotation(yaw: Float, pitch: Float) = Rotation(
        yaw = Mth.wrapDegrees(yaw),
        pitch = Mth.wrapDegrees(pitch).coerceIn(-90f, 90f),
        isNormalized = true
    )

    /**
     * Resolves the relative rotation flags used by vanilla player position and rotation updates.
     *
     * @see net.minecraft.world.entity.PositionMoveRotation.calculateAbsolute
     * @see net.minecraft.world.entity.Entity.forceSetRotation
     */
    private fun clientboundRotation(
        yaw: Float,
        pitch: Float,
        relativeYaw: Boolean,
        relativePitch: Boolean
    ) = Rotation(
        yaw = yaw + if (relativeYaw) actualServerRotation.yaw else 0f,
        pitch = (pitch + if (relativePitch) actualServerRotation.pitch else 0f).coerceIn(-90f, 90f),
        isNormalized = true
    )

    @AddonApi
    @Suppress("LongParameterList")
    fun setRotationTarget(
        rotation: Rotation,
        considerInventory: Boolean = true,
        valueGroup: RotationsValueGroup,
        priority: Priority,
        provider: EventListener,
        surface: Surface? = null,
    ) {
        setRotationTarget(valueGroup.toRotationTarget(
            rotation, considerInventory = considerInventory, surface = surface
        ), priority, provider)
    }

    @AddonApi
    fun setRotationTarget(plan: RotationTarget, priority: Priority, provider: EventListener) =
        setRotationTarget(plan, priority.priority, provider)

    @AddonApi
    fun setRotationTarget(plan: RotationTarget, priority: Int, provider: EventListener) {
        if (!allowedToUpdate()) {
            return
        }

        rotationTargetHandler.request(
            RequestHandler.Request(
                if (plan.movementCorrection == MovementCorrection.CHANGE_LOOK) 1 else plan.ticksUntilReset,
                priority,
                provider,
                plan
            )
        )
    }

    /**
     * Checks if the rotation is allowed to be updated
     */
    fun isRotatingAllowed(rotationTarget: RotationTarget): Boolean {
        if (!allowedToUpdate()) {
            return false
        }

        if (rotationTarget.considerInventory) {
            if (InventoryManager.isInventoryOpen || mc.gui.screen() is ContainerScreen) {
                return false
            }
        }

        return true
    }

    /**
     * Turns the view one tick towards the targets. Each axis follows the most important target that turns it; an axis
     * no target turns goes back to where the player looks and then follows the mouse. Once both do and no target is
     * left, the view is handed back.
     */
    fun update() {
        lastModelRotation = currentRotation
        rotationTargetHandler.activeRequestValues(requests)
        val lead = requests.firstOrNull()
        val target = lead ?: previousRotationTarget

        if (target != null && (lead != null || currentRotation != null) && isRotatingAllowed(target)) {
            if (lead == null && target.movementCorrection == MovementCorrection.CHANGE_LOOK) {
                release()
            } else {
                turn(target, lead)
            }
        }

        rotationTargetHandler.tick()
    }

    private fun turn(target: RotationTarget, lead: RotationTarget?) {
        val camera = player.rotation
        val view = currentRotation ?: camera
        val yawTarget = requests.firstOrNull { it.yaw != null }
        val pitchTarget = requests.firstOrNull { it.pitch != null }
        val changeLook = target.movementCorrection == MovementCorrection.CHANGE_LOOK
        // Under ChangeLook the player looks through the view, so a free axis has nowhere else to go
        val own = if (changeLook) view else camera
        val goal = Rotation(yawTarget?.yaw ?: own.yaw, pitchTarget?.pitch ?: own.pitch)
        val step = RotationUtil.gcd.toFloat()
        val aim = target.aim
        val delta = if (aim == null) {
            view.rotationDeltaTo(goal)
        } else {
            aim.turn(view, aimGoal(view, goal, yawTarget, pitchTarget, step), aim !== lastAim)
        }
        lastAim = aim

        var next = RotationUtil.turnByCounts(
            view,
            (delta.deltaYaw / step).roundToInt(),
            (delta.deltaPitch / step).roundToInt(),
        )

        if (yawTarget != null) {
            yawFollowsPlayer = false
        } else if (!changeLook && (yawFollowsPlayer || abs(angleDifference(camera.yaw, next.yaw)) < step / 2)) {
            yawFollowsPlayer = true
            next = Rotation(next.yaw + angleDifference(camera.yaw, next.yaw), next.pitch)
        }

        if (pitchTarget != null) {
            pitchFollowsPlayer = false
        } else if (!changeLook && (pitchFollowsPlayer || abs(camera.pitch - next.pitch) < step / 2)) {
            pitchFollowsPlayer = true
            next = Rotation(next.yaw, camera.pitch)
        }

        currentRotation = next

        if (lead == null && yawFollowsPlayer && pitchFollowsPlayer) {
            release()
            return
        }

        previousRotationTarget = target
        looks.offer(next, requests)
    }

    private fun aimGoal(
        view: Rotation,
        goal: Rotation,
        yawTarget: RotationTarget?,
        pitchTarget: RotationTarget?,
        step: Float,
    ): AimGoal {
        val owner = yawTarget.takeIf { it === pitchTarget }
        val surface = owner?.hitSurface
        if (surface != null) {
            val eyes = player.eyePosition
            return surfaceGoal(goal, eyes, surface, surface.isHitBy(eyes, view), owner.entity)
        }

        val tolerance = max(yawTarget?.tolerance ?: 0f, pitchTarget?.tolerance ?: 0f)
        val margin = max(tolerance, step / 2)
        val delta = view.rotationDeltaTo(goal)
        val reached = abs(delta.deltaYaw) < margin && abs(delta.deltaPitch) < margin
        return AimGoal(goal, tolerance * 2, tolerance * 2, reached, owner?.entity)
    }

    /**
     * [goal] on [surface], with the yaw and pitch extents of it as seen from [eyes].
     */
    private fun surfaceGoal(goal: Rotation, eyes: Vec3, surface: Surface, reached: Boolean, entity: Entity?): AimGoal {
        if (surface.box.contains(eyes)) {
            return AimGoal(goal, 180f, 180f, reached, entity)
        }

        val corners = surface.corners
        val center = Rotation.lookingAt(corners.fold(Vec3.ZERO, Vec3::add).scale(1.0 / corners.size), eyes)
        var minYaw = Float.POSITIVE_INFINITY
        var maxYaw = Float.NEGATIVE_INFINITY
        var minPitch = Float.POSITIVE_INFINITY
        var maxPitch = Float.NEGATIVE_INFINITY
        for (corner in corners) {
            val rotation = Rotation.lookingAt(corner, eyes)
            val yaw = angleDifference(rotation.yaw, center.yaw)
            minYaw = min(minYaw, yaw)
            maxYaw = max(maxYaw, yaw)
            minPitch = min(minPitch, rotation.pitch)
            maxPitch = max(maxPitch, rotation.pitch)
        }
        return AimGoal(goal, maxYaw - minYaw, maxPitch - minPitch, reached, entity)
    }

    private fun release() {
        currentRotation?.let { currentRotation ->
            player.yRot = player.withFixedYaw(currentRotation)
            player.yBob = player.yRot
            player.yBobO = player.yRot
        }

        currentRotation = null
        previousRotationTarget = null
        lastAim = null
        yawFollowsPlayer = false
        pitchFollowsPlayer = false
    }

    fun applyChangeLookRotation(partialTicks: Float) {
        val activeRotationTarget = this.activeRotationTarget ?: return

        if (isRotatingAllowed(activeRotationTarget)
            && activeRotationTarget.movementCorrection == MovementCorrection.CHANGE_LOOK) {
            val currentRotation = currentRotation ?: return
            val lastRotation = lastRotation ?: currentRotation

            player.setRotation(lastRotation.interpolateTo(currentRotation, partialTicks))
        }
    }

    @Suppress("unused")
    private val mouseMovement = handler<MouseRotationEvent> { event ->
        val activeRotationTarget = this.activeRotationTarget ?: return@handler
        if (!isRotatingAllowed(activeRotationTarget) ||
            activeRotationTarget.movementCorrection != MovementCorrection.CHANGE_LOOK) {
            return@handler
        }

        fun adjustRotation(rotation: Rotation): Rotation =
            RotationUtil.applyMouseTurnDelta(rotation, event.cursorDeltaX, event.cursorDeltaY)

        val currentRotation = currentRotation ?: return@handler
        val lastRotation = lastRotation ?: currentRotation

        // The setter would move lastRotation onto the old currentRotation, so shift both ends instead
        this.currentRotation = adjustRotation(currentRotation)
        this.lastRotation = adjustRotation(lastRotation)
        lastModelRotation = lastModelRotation?.let(::adjustRotation)
    }

    /**
     * Checks if it should update the server-side rotations
     */
    private fun allowedToUpdate() = !CombatManager.shouldPauseRotation

    fun rotationMatchesPreviousRotation(): Boolean {
        val player = mc.player ?: return false

        currentRotation?.let {
            return it == lastRotation
        }

        return player.rotation == player.lastRotation
    }

    @Suppress("unused")
    private val velocityHandler = handler<PlayerVelocityStrafe>(priority = MODEL_STATE) { event ->
        if (activeRotationTarget?.movementCorrection != MovementCorrection.OFF) {
            val rotation = currentRotation ?: return@handler

            event.velocity = Entity.getInputVector(
                event.movementInput,
                event.speed,
                rotation.yaw
            )
        }
    }

    @Suppress("unused")
    private val gameTickHandler = handler<GameTickEvent>(
        priority = FIRST_PRIORITY
    ) { event ->
        EventManager.callEvent(RotationUpdateEvent)
        looks.request(::setRotationTarget)
        update()
    }

    /**
     * Resumes the modules whose look was met this tick: after every other first handler, such as the clicker planning
     * this tick's clicks, and before modules tick and the movement packet goes out.
     */
    @Suppress("unused")
    private val lookHandler = handler<GameTickEvent>(priority = (FIRST_PRIORITY - 1).toShort()) {
        looks.settle()
    }

    /**
     * Track rotation changes
     *
     * We cannot only rely on player.lastYaw and player.lastPitch because
     * sometimes we update the rotation off chain (e.g. on interactItem)
     * and the player.lastYaw and player.lastPitch are not updated.
     */
    @Suppress("unused")
    val packetHandler = handler<PacketEvent>(
        priority = EventPriorityConvention.READ_FINAL_STATE
    ) { event ->
        val rotation = when (val packet = event.packet) {
            is ServerboundMovePlayerPacket -> {
                // If we are not changing the look, we don't need to update the rotation
                if (!packet.hasRot) {
                    return@handler
                }

                serverboundRotation(packet.yRot, packet.xRot)
            }
            is ClientboundPlayerPositionPacket -> clientboundRotation(
                packet.change.yRot,
                packet.change.xRot,
                Relative.Y_ROT in packet.relatives,
                Relative.X_ROT in packet.relatives,
            )
            is ClientboundPlayerRotationPacket -> clientboundRotation(
                packet.yRot,
                packet.xRot,
                packet.relativeY,
                packet.relativeX,
            )
            is ServerboundUseItemPacket -> serverboundRotation(packet.yRot, packet.xRot)
            else -> return@handler
        }

        // Incoming corrections already changed the server-side player before they were sent. For
        // outgoing packets, only packets that pass the event pipeline can affect the server.
        if (event.origin == TransferOrigin.INCOMING || !event.isCancelled) {
            actualServerRotation = rotation
        }
        theoreticalServerRotation = rotation
    }

    /**
     * It overwrites the body part's yaw and rotation, so the F5 view model looks
     * at the [RotationManager.currentRotation].
     */
    private val bodyPart by multiEnumChoice("BodyPart", BodyPart.entries)

    @Suppress("unused")
    enum class BodyPart(
        override val tag: String,
    ) : Tagged {
        HEAD("Head"),
        BODY("Body");
    }

    /**
     * Yaw of [part] on the F5 model. Blends from or to [vanillaYaw] on the tick a rotation starts or ends.
     */
    fun modelYaw(part: BodyPart, partialTicks: Float, vanillaYaw: Float): Float {
        if (part !in bodyPart) {
            return vanillaYaw
        }

        val from = lastModelRotation?.yaw ?: vanillaYaw
        val to = currentRotation?.yaw ?: vanillaYaw
        return Mth.rotLerp(partialTicks, from, to)
    }

    fun modelPitch(partialTicks: Float, vanillaPitch: Float): Float {
        if (BodyPart.HEAD !in bodyPart) {
            return vanillaPitch
        }

        val from = lastModelRotation?.pitch ?: vanillaPitch
        val to = currentRotation?.pitch ?: vanillaPitch
        return Mth.lerp(partialTicks, from, to)
    }

    /**
     * Draws a tracer line to the point we aim at.
     */
    private val vectorLine by color("VectorLine", Color4b.WHITE.with(a = 0)) // alpha 0 means OFF

    /**
     * Draws a dot at the point we aim at.
     */
    private val vectorDot by color("VectorDot", Color4b(0x00, 0x80, 0xFF, 0x00))

    @Suppress("unused")
    private val renderHandler = handler<WorldRenderEvent> { event ->
        val drawVectorLine = vectorLine.a > 0
        val drawVectorDot = vectorDot.a > 0

        if (drawVectorLine || drawVectorDot) {
            val currentRotation = currentRotation ?: return@handler
            val previousRotation = lastRotation ?: currentRotation

            val interpolatedRotationVec = previousRotation.directionVector
                .lerp(currentRotation.directionVector, event.partialTicks.toDouble())
                .toVec3f()

            val eyeVector = Vec3f.eyeVector(event.camera)

            event.renderEnvironment {
                val vector = eyeVector.fma(100f, interpolatedRotationVec)
                if (drawVectorLine) {
                    drawLine(eyeVector, vector, vectorLine.argb)
                }

                if (drawVectorDot) {
                    drawBox(AABB.ofSize(vector.toVec3d(), 2.5, 2.5, 2.5), vectorDot)
                }
            }
        }
    }

    override val running: Boolean
        get() = inGame

}

internal fun resolveMovementYaw(
    playerYaw: Float,
    managedYaw: Float,
    movementCorrection: MovementCorrection?,
): Float = if (managedYaw.isFinite() && movementCorrection != null && movementCorrection != MovementCorrection.OFF) {
    managedYaw
} else {
    playerYaw
}
