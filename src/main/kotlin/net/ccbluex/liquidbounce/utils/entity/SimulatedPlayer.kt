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
package net.ccbluex.liquidbounce.utils.entity

import net.ccbluex.liquidbounce.event.EventManager.callEvent
import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.event.events.PlayerMoveEvent
import net.ccbluex.liquidbounce.event.events.PlayerSafeWalkEvent
import net.ccbluex.liquidbounce.event.events.PlayerSneakMultiplier
import net.ccbluex.liquidbounce.event.events.PlayerUseMultiplier
import net.ccbluex.liquidbounce.event.events.SprintEvent
import net.ccbluex.liquidbounce.features.client.ClientContext
import net.ccbluex.liquidbounce.features.module.modules.movement.ModuleSprint
import net.ccbluex.liquidbounce.features.module.modules.movement.noslow.ModuleNoSlow
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.movement.DirectionalInput
import net.ccbluex.liquidbounce.utils.movement.getDegreesRelativeToView
import net.ccbluex.liquidbounce.utils.movement.getDirectionalInputForDegrees
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.player.AbstractClientPlayer
import net.minecraft.client.player.ClientInput
import net.minecraft.client.player.KeyboardInput
import net.minecraft.client.player.LocalPlayer
import net.minecraft.core.component.DataComponents
import net.minecraft.util.Mth
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.MoverType
import net.minecraft.world.entity.Pose
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.player.Input
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.component.UseEffects
import net.minecraft.world.phys.Vec2
import net.minecraft.world.phys.Vec3
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.sqrt

/**
 * A vanilla player entity that is never added to a level and moves through the real one with a fixed [input].
 *
 * Movement is [net.minecraft.world.entity.LivingEntity.aiStep] itself. Only what
 * [LocalPlayer] adds on top of it is ported here, since a [LocalPlayer] cannot tick without sending packets.
 * Entities are not pushed and do not push back.
 *
 * Every simulation runs in its own simulated client, a fork of the client it was made in, so nothing it causes
 * changes the real client or leaves the game. A simulation of the local player lets modules take part like in a
 * tick of the local player: they see [input] as the keys pressed and may change them, jump, strafe and step.
 * Simulations that modules start while taking part only move the vanilla way.
 */
@Suppress("TooManyFunctions")
class SimulatedPlayer private constructor(
    source: Player,
    var input: SimulatedPlayerInput,
    deltaMovement: Vec3,
    /**
     * The source already ran the part of its tick before [aiStep], so the first tick continues from there.
     */
    private var continuesTick: Boolean,
    withModules: Boolean,
) : AbstractClientPlayer(source.level() as ClientLevel, source.gameProfile), PlayerSimulation {

    private val local: Boolean = source is LocalPlayer || source is SimulatedPlayer && source.local

    private val client = ClientContext.current.let { origin ->
        origin.fork().also { fork ->
            if (withModules && local && !origin.isSimulated) {
                fork.simulatedPlayer = this
            }
        }
    }

    private val modulesTakePart: Boolean = client.simulatedPlayer === this

    /**
     * Modules take part in this simulation. Only on the game thread, where they run and where `player` may be lent.
     */
    val firesEvents: Boolean
        get() = modulesTakePart && ClientContext.isGameThread

    /**
     * The keys of the tick, as modules left them.
     */
    private var polled: SimulatedPlayerInput? = null

    internal val tickInput: SimulatedPlayerInput
        get() = polled ?: input

    private var crouching = source.isCrouching

    /**
     * Crouching follows the sneak key of the tick before.
     */
    private var wasShiftKeyDown = source.isShiftKeyDown
    private var probesLedge = false

    private val itemUseEffects = source.takeIf { it.isUsingItem && !it.isPassenger }
        ?.useItem?.getOrDefault(DataComponents.USE_EFFECTS, UseEffects.DEFAULT)

    /**
     * Sideways and forward speed while using an item. A simulation made from another one takes it over,
     * since it does not use the item itself.
     */
    private val itemUseMultiplier: Vec2? = if (source is SimulatedPlayer) {
        source.itemUseMultiplier
    } else {
        itemUseEffects?.speedMultiplier()?.let { multiplier ->
            if (firesEvents) {
                val event = ClientContext.within(client) { callEvent(PlayerUseMultiplier(multiplier, multiplier)) }
                Vec2(event.sideways, event.forward)
            } else {
                Vec2(multiplier, multiplier)
            }
        }
    }

    private val isSlowDueToUsingItem: Boolean = if (source is SimulatedPlayer) {
        source.isSlowDueToUsingItem
    } else {
        itemUseEffects?.canSprint() == false && !(local && ModuleNoSlow.running)
    }

    private val sourceSneakingSpeed: Float? = (source as? SimulatedPlayer)?.sneakingSpeed

    private val sneakingSpeed: Float by lazy {
        sourceSneakingSpeed ?: run {
            val speed = getAttributeValue(Attributes.SNEAKING_SPEED)
            (if (firesEvents) callEvent(PlayerSneakMultiplier(speed)).multiplier else speed).toFloat()
        }
    }

    /**
     * Sneaking or safe walk would have held the player back at a ledge in the last tick.
     */
    var clipLedged = false
        private set

    override val pos: Vec3
        get() = position()

    init {
        attributes.assignAllValues(source.attributes)
        for (effect in source.activeEffects) {
            addEffect(MobEffectInstance(effect))
        }
        for (slot in EquipmentSlot.VALUES) {
            equipment.set(slot, source.getItemBySlot(slot))
        }
        abilities.flying = source.abilities.flying
        abilities.mayfly = source.abilities.mayfly
        abilities.flyingSpeed = source.abilities.flyingSpeed
        foodData.foodLevel = source.getFoodData().foodLevel

        pose = source.pose
        setPos(source.position())
        setOldPosAndRot(source.oldPosition(), source.yRotO, source.xRotO)
        yRot = source.yRot
        xRot = source.xRot
        this.deltaMovement = deltaMovement

        setOnGround(source.onGround())
        mainSupportingBlockPos = source.mainSupportingBlockPos
        horizontalCollision = source.horizontalCollision
        verticalCollision = source.verticalCollision
        verticalCollisionBelow = source.verticalCollisionBelow
        minorHorizontalCollision = source.minorHorizontalCollision
        fallDistance = source.fallDistance
        stuckSpeedMultiplier = source.stuckSpeedMultiplier
        isInPowderSnow = source.isInPowderSnow
        wasInPowderSnow = source.wasInPowderSnow
        noJumpDelay = source.noJumpDelay

        firstTick = false
        fluidInteraction.update(this, true)
        wasTouchingWater = source.isInWater
        wasUnderwater = source.isUnderWater

        isSwimming = source.isSwimming
        isSprinting = source.isSprinting
        setSharedFlag(FLAG_FALL_FLYING, source.isFallFlying)
    }

    override fun tick() {
        clipLedged = false

        ClientContext.within(client) {
            if (continuesTick) {
                aiStep()
                updatePlayerPose()
            } else {
                commonTick()
                super<AbstractClientPlayer>.tick()
            }

            removeExpiredEffects()
        }

        // Nothing of the simulated client outlives the tick
        client.release()
    }

    /**
     * The server removes an expired effect, which a simulation never hears of.
     */
    private fun removeExpiredEffects() {
        activeEffects
            .filter { effect -> !effect.isInfiniteDuration && effect.duration <= 0 }
            .forEach { effect -> removeEffect(effect.effect) }
    }

    private val isMovingSlowly
        get() = isCrouching || isVisuallyCrawling

    /**
     * @see LocalPlayer.aiStep
     */
    override fun aiStep() {
        if (continuesTick) {
            continuesTick = false
        } else {
            updateCrouching()
            if (firesEvents) {
                polled = pollModules()
            }
        }

        val keys = tickInput.keyPresses
        wasShiftKeyDown = keys.shift
        updateSprinting(keys.sprint)

        if (isInWater && keys.shift && isAffectedByFluids) {
            goDownInWater()
        }

        val flyingUp = (if (keys.jump) 1 else 0) - (if (keys.shift) 1 else 0)
        if (abilities.flying && flyingUp != 0) {
            addDeltaMovement(Vec3(0.0, (flyingUp * abilities.flyingSpeed * 3.0f).toDouble(), 0.0))
        }

        super.aiStep()

        if (onGround() && abilities.flying && !isSpectator) {
            abilities.flying = false
        }
    }

    /**
     * The keys of this tick as modules change them.
     *
     * @see net.ccbluex.liquidbounce.injection.mixins.minecraft.client.MixinKeyboardInput
     */
    private fun pollModules() = standIn {
        val keys = input.keyPresses
        val movement = callEvent(MovementInputEvent(input.directionalInput, keys.jump, keys.shift))
        val sprint = callEvent(SprintEvent(movement.directionalInput, keys.sprint, SprintEvent.Source.INPUT))
        val safeWalk = callEvent(PlayerSafeWalkEvent())

        SimulatedPlayerInput(
            movement.directionalInput,
            movement.jump,
            sprint.sprint,
            movement.sneak,
            input.ignoreClippingAtLedge,
        ).apply {
            forceSafeWalk = input.forceSafeWalk || safeWalk.isSafeWalk
        }
    }

    private fun updateCrouching() {
        val fitsStanding = isSleeping || canPlayerFitWithinBlocksAndEntitiesWhen(Pose.STANDING)
        val mayCrouch = !abilities.flying && !isSwimming && !isPassenger
        crouching = mayCrouch && canPlayerFitWithinBlocksAndEntitiesWhen(Pose.CROUCHING) &&
            (wasShiftKeyDown || !fitsStanding)
    }

    private fun updateSprinting(sprintKey: Boolean) {
        if (modulesSprint(canStartSprinting()) && modulesSprint(sprintKey)) {
            isSprinting = true
        }
        if (isSprinting && if (isSwimming) shouldStopSwimSprinting() else shouldStopRunSprinting()) {
            isSprinting = false
        }
    }

    /**
     * @see net.ccbluex.liquidbounce.injection.mixins.minecraft.entity.MixinLocalPlayer.hookSprint0
     */
    private fun modulesSprint(sprint: Boolean) = if (firesEvents) {
        standIn {
            callEvent(SprintEvent(tickInput.directionalInput, sprint, SprintEvent.Source.MOVEMENT_TICK)).sprint
        }
    } else {
        sprint
    }

    /**
     * @see LocalPlayer.applyInput
     */
    override fun applyInput() {
        val keys = tickInput
        var move = keys.moveVector
        if (move.lengthSquared() != 0.0f) {
            move = move.scale(0.98f)
            itemUseMultiplier?.let { move = Vec2(move.x * it.x, move.y * it.y) }
            if (isMovingSlowly) {
                move = move.scale(sneakingSpeed)
            }
            move = LocalPlayer.modifyInputSpeedForSquareMovement(move)
        }

        xxa = move.x
        zza = move.y
        jumping = keys.keyPresses.jump
    }

    private fun isSprintingPossible(allowedInShallowWater: Boolean) =
        !isMobilityRestricted && hasEnoughFoodToDoExhaustiveManoeuvres() &&
            (allowedInShallowWater || !isInShallowWater)

    private fun canStartSprinting(): Boolean {
        val canSprint = isSprintingPossible(abilities.flying) && !isSlowDueToUsingItem
        val free = isUnderWater || !isFallFlying && !isMovingSlowly
        return !isSprinting && hasSprintImpulse() && canSprint && free
    }

    /**
     * @see net.ccbluex.liquidbounce.injection.mixins.minecraft.entity.MixinLocalPlayer.hookIsWalking
     */
    private fun hasSprintImpulse(): Boolean {
        if (!local || !ModuleSprint.shouldSprintOmnidirectional) {
            return tickInput.hasForwardImpulse()
        }

        val move = tickInput.moveVector
        return if (isUnderWater) {
            abs(move.y) > MIN_IMPULSE || abs(move.x) > MIN_IMPULSE
        } else {
            abs(move.y) >= WALKING_IMPULSE || abs(move.x) >= WALKING_IMPULSE
        }
    }

    private fun shouldStopRunSprinting(): Boolean {
        val blocked = horizontalCollision && !minorHorizontalCollision && !(local && ModuleSprint.shouldIgnoreCollision)
        return !isSprintingPossible(abilities.flying) || !tickInput.hasForwardImpulse() || blocked ||
            firesEvents && !modulesSprint(true)
    }

    private fun shouldStopSwimSprinting() =
        !isSprintingPossible(true) || !isInWater ||
            !tickInput.hasForwardImpulse() && !onGround() && !tickInput.keyPresses.shift

    /**
     * @see LocalPlayer.isHorizontalCollisionMinor
     */
    override fun isHorizontalCollisionMinor(movement: Vec3): Boolean {
        val yaw = (yRot * Mth.DEG_TO_RAD).toDouble()
        val sin = Mth.sin(yaw).toDouble()
        val cos = Mth.cos(yaw).toDouble()
        val wantedX = xxa * cos - zza * sin
        val wantedZ = zza * cos + xxa * sin
        val wantedLengthSqr = wantedX * wantedX + wantedZ * wantedZ
        val movedLengthSqr = movement.x * movement.x + movement.z * movement.z
        if (wantedLengthSqr < MINOR_COLLISION_MIN_LENGTH_SQR || movedLengthSqr < MINOR_COLLISION_MIN_LENGTH_SQR) {
            return false
        }

        val dot = wantedX * movement.x + wantedZ * movement.z
        return acos(dot / sqrt(wantedLengthSqr * movedLengthSqr)) < MINOR_COLLISION_ANGLE
    }

    override fun move(moverType: MoverType, delta: Vec3) = standIn {
        super.move(moverType, if (firesEvents) callEvent(PlayerMoveEvent(moverType, delta)).movement else delta)
    }

    override fun jumpFromGround() = standIn {
        super.jumpFromGround()
    }

    override fun moveRelative(speed: Float, input: Vec3) = standIn {
        super.moveRelative(speed, input)
    }

    /**
     * Modules write to `player`, which stays the real one in a simulated client. While they take part in this
     * simulation, the real player lends its velocity to this one, and whatever they change on it is moved over
     * to this one when they are done.
     */
    private inline fun <T> standIn(block: () -> T): T {
        val real = client.player
        if (!firesEvents || real == null) {
            return block()
        }

        val velocity = real.deltaMovement
        val position = real.position()
        val sprinting = real.isSprinting
        val onGround = real.onGround()
        val yaw = real.yRot
        val pitch = real.xRot
        val fallDistance = real.fallDistance

        val lent = deltaMovement
        real.deltaMovement = lent
        try {
            return block()
        } finally {
            if (real.deltaMovement !== lent) {
                deltaMovement = real.deltaMovement
            }
            real.deltaMovement = velocity
            if (real.isSprinting != sprinting) {
                isSprinting = real.isSprinting
                real.isSprinting = sprinting
            }
            if (real.yRot != yaw || real.xRot != pitch) {
                yRot = real.yRot
                xRot = real.xRot
                real.yRot = yaw
                real.xRot = pitch
            }
            if (real.fallDistance != fallDistance) {
                this.fallDistance = real.fallDistance
                real.fallDistance = fallDistance
            }
            if (real.position() !== position) {
                real.setPos(position)
            }
            if (real.onGround() != onGround) {
                real.setOnGround(onGround)
            }
        }
    }

    override fun maybeBackOffFromEdge(delta: Vec3, moverType: MoverType): Vec3 {
        probesLedge = true
        val backedOff = try {
            super.maybeBackOffFromEdge(delta, moverType)
        } finally {
            probesLedge = false
        }

        clipLedged = backedOff != delta
        return if (staysOnGroundSurface()) backedOff else delta
    }

    private fun staysOnGroundSurface() =
        !tickInput.ignoreClippingAtLedge && (tickInput.keyPresses.shift || tickInput.forceSafeWalk)

    override fun isStayingOnGroundSurface() = probesLedge || staysOnGroundSurface()

    override fun isShiftKeyDown() = tickInput.keyPresses.shift

    override fun isCrouching() = crouching

    override fun isUnderWater() = wasUnderwater

    override fun isSuppressingSlidingDownLadder() = !abilities.flying && super.isSuppressingSlidingDownLadder()

    override fun isLocalClientAuthoritative() = true

    override fun isEffectiveAi() = true

    override fun pushEntities() {
        // Would push the real entities around
    }

    class SimulatedPlayerInput(
        val directionalInput: DirectionalInput,
        jumping: Boolean,
        sprinting: Boolean,
        sneaking: Boolean,
        var ignoreClippingAtLedge: Boolean = false
    ) : ClientInput() {
        var forceSafeWalk: Boolean = false

        init {
            set(
                forward = directionalInput.forwards,
                backward = directionalInput.backwards,
                left = directionalInput.left,
                right = directionalInput.right,
                jump = jumping,
                sneak = sneaking,
                sprint = sprinting
            )
            moveVector = Vec2(
                KeyboardInput.calculateImpulse(directionalInput.left, directionalInput.right),
                KeyboardInput.calculateImpulse(directionalInput.forwards, directionalInput.backwards)
            ).normalized()
        }

        override fun toString() = "SimulatedPlayerInput($keyPresses)"

        companion object {
            private const val MAX_WALKING_SPEED = 0.121

            @JvmStatic
            fun fromClientPlayer(
                directionalInput: DirectionalInput,
                jump: Boolean = localKeys.jump,
                sprinting: Boolean = localPlayer.isSprinting || localKeys.sprint,
                sneaking: Boolean = localPlayer.isShiftKeyDown
            ): SimulatedPlayerInput {
                val input = SimulatedPlayerInput(
                    directionalInput,
                    jump,
                    sprinting,
                    sneaking
                )

                // Inside a simulation of the local player, modules already decided on safe walk for this tick
                input.forceSafeWalk = simulatedLocalPlayer?.tickInput?.forceSafeWalk
                    ?: callEvent(PlayerSafeWalkEvent()).isSafeWalk

                return input
            }

            /**
             * Guesses the current input of a server player based on player position and velocity
             */
            @JvmStatic
            fun guessInput(entity: Player): SimulatedPlayerInput {
                val velocity = entity.position().subtract(entity.lastPos)

                val horizontalVelocity = velocity.horizontalDistanceSqr()

                val sprinting = horizontalVelocity >= MAX_WALKING_SPEED * MAX_WALKING_SPEED

                val input = if (horizontalVelocity > 0.05 * 0.05) {
                    val velocityAngle = getDegreesRelativeToView(velocity, yaw = entity.yRot)

                    val velocityAngle1 = Mth.wrapDegrees(velocityAngle)

                    getDirectionalInputForDegrees(DirectionalInput.NONE, velocityAngle1)
                } else {
                    DirectionalInput.NONE
                }

                val jumping = !entity.onGround()

                return SimulatedPlayerInput(
                    input,
                    jumping,
                    sprinting,
                    sneaking = entity.isShiftKeyDown
                )
            }
        }

    }


    companion object {
        private const val FLAG_FALL_FLYING = 7
        private const val MINOR_COLLISION_MIN_LENGTH_SQR = 1.0E-5f
        private const val MINOR_COLLISION_ANGLE = 0.13962634f
        private const val MIN_IMPULSE = 1.0E-5f
        private const val WALKING_IMPULSE = 0.8f

        /**
         * The local player the simulated client of the current thread simulates.
         */
        private val simulatedLocalPlayer: SimulatedPlayer?
            get() = ClientContext.current.simulatedPlayer as SimulatedPlayer?

        /**
         * The local player as a simulation sees it: while modules take part in a simulation, the simulated one.
         */
        private val localPlayer: Player
            get() = simulatedLocalPlayer ?: player

        private val localKeys: Input
            get() = simulatedLocalPlayer?.tickInput?.keyPresses ?: player.input.keyPresses

        /**
         * @param withModules lets modules take part, see [SimulatedPlayer]. A simulation that tries out keys of its
         * own goes without them, or a module steering the player would steer it too.
         */
        @JvmStatic
        @JvmOverloads
        fun fromClientPlayer(input: SimulatedPlayerInput, withModules: Boolean = true): SimulatedPlayer {
            val source = localPlayer
            return SimulatedPlayer(
                source,
                input,
                source.deltaMovement,
                // Modules only see a simulated player while it is in its tick
                continuesTick = source is SimulatedPlayer || PlayerSimulationCache.isLocalPlayerMoving,
                withModules = withModules,
            )
        }

        @JvmStatic
        fun fromOtherPlayer(player: Player, input: SimulatedPlayerInput) = SimulatedPlayer(
            player,
            input,
            deltaMovement = player.position().subtract(player.lastPos),
            continuesTick = false,
            withModules = false,
        )
    }
}
