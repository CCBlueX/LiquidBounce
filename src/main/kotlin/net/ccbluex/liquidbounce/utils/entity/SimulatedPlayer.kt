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
import net.ccbluex.liquidbounce.event.events.PlayerMoveEvent
import net.ccbluex.liquidbounce.event.events.PlayerSafeWalkEvent
import net.ccbluex.liquidbounce.event.events.PlayerSneakMultiplier
import net.ccbluex.liquidbounce.event.events.PlayerUseMultiplier
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
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.component.UseEffects
import net.minecraft.world.phys.Vec2
import net.minecraft.world.phys.Vec3
import kotlin.math.acos
import kotlin.math.sqrt

/**
 * A vanilla player entity that is never added to a level and moves through the real one with a fixed [input].
 *
 * Movement is [net.minecraft.world.entity.LivingEntity.aiStep] itself. Only what
 * [LocalPlayer] adds on top of it is ported here, since a [LocalPlayer] cannot tick without sending packets.
 * Entities are not pushed and do not push back, and the level stays untouched while a simulation ticks.
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
) : AbstractClientPlayer(source.level() as ClientLevel, source.gameProfile), PlayerSimulation {

    private val local = source is LocalPlayer

    private var crouching = source.isCrouching

    /**
     * Crouching follows the sneak key of the tick before.
     */
    private var wasShiftKeyDown = source.isShiftKeyDown
    private var probesLedge = false

    private val itemUseEffects = source.takeIf { it.isUsingItem && !it.isPassenger }
        ?.useItem?.getOrDefault(DataComponents.USE_EFFECTS, UseEffects.DEFAULT)

    /**
     * Sideways and forward speed while using an item.
     */
    private val itemUseMultiplier = itemUseEffects?.speedMultiplier()?.let { multiplier ->
        if (local) {
            val event = callEvent(PlayerUseMultiplier(multiplier, multiplier))
            Vec2(event.sideways, event.forward)
        } else {
            Vec2(multiplier, multiplier)
        }
    }

    private val isSlowDueToUsingItem = itemUseEffects?.canSprint() == false && !(local && ModuleNoSlow.running)

    private val sneakingSpeed by lazy {
        val speed = getAttributeValue(Attributes.SNEAKING_SPEED)
        (if (local) callEvent(PlayerSneakMultiplier(speed)).multiplier else speed).toFloat()
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

        ticking.set(true)
        try {
            if (continuesTick) {
                aiStep()
                updatePlayerPose()
            } else {
                commonTick()
                super<AbstractClientPlayer>.tick()
            }
        } finally {
            ticking.set(false)
        }

        removeExpiredEffects()
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
        }

        val keys = input.keyPresses
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

    private fun updateCrouching() {
        val fitsStanding = isSleeping || canPlayerFitWithinBlocksAndEntitiesWhen(Pose.STANDING)
        val mayCrouch = !abilities.flying && !isSwimming && !isPassenger
        crouching = mayCrouch && canPlayerFitWithinBlocksAndEntitiesWhen(Pose.CROUCHING) &&
            (wasShiftKeyDown || !fitsStanding)
    }

    private fun updateSprinting(sprintKey: Boolean) {
        if (sprintKey && canStartSprinting()) {
            isSprinting = true
        }
        if (isSprinting && if (isSwimming) shouldStopSwimSprinting() else shouldStopRunSprinting()) {
            isSprinting = false
        }
    }

    /**
     * @see LocalPlayer.applyInput
     */
    override fun applyInput() {
        var move = input.moveVector
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
        jumping = input.keyPresses.jump
    }

    private fun isSprintingPossible(allowedInShallowWater: Boolean) =
        !isMobilityRestricted && hasEnoughFoodToDoExhaustiveManoeuvres() &&
            (allowedInShallowWater || !isInShallowWater)

    private fun canStartSprinting(): Boolean {
        val canSprint = isSprintingPossible(abilities.flying) && !isSlowDueToUsingItem
        val free = isUnderWater || !isFallFlying && !isMovingSlowly
        return !isSprinting && input.hasForwardImpulse() && canSprint && free
    }

    private fun shouldStopRunSprinting() =
        !isSprintingPossible(abilities.flying) || !input.hasForwardImpulse() ||
            horizontalCollision && !minorHorizontalCollision

    private fun shouldStopSwimSprinting() =
        !isSprintingPossible(true) || !isInWater ||
            !input.hasForwardImpulse() && !onGround() && !input.keyPresses.shift

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

    override fun move(moverType: MoverType, delta: Vec3) {
        super.move(moverType, if (local) callEvent(PlayerMoveEvent(moverType, delta)).movement else delta)
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
        !input.ignoreClippingAtLedge && (input.keyPresses.shift || input.forceSafeWalk)

    override fun isStayingOnGroundSurface() = probesLedge || staysOnGroundSurface()

    override fun isShiftKeyDown() = input.keyPresses.shift

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
                jump: Boolean = player.input.keyPresses.jump,
                sprinting: Boolean = player.isSprinting || player.input.keyPresses.sprint,
                sneaking: Boolean = player.isShiftKeyDown
            ): SimulatedPlayerInput {
                val input = SimulatedPlayerInput(
                    directionalInput,
                    jump,
                    sprinting,
                    sneaking
                )

                val safeWalkEvent = PlayerSafeWalkEvent()

                callEvent(safeWalkEvent)

                if (safeWalkEvent.isSafeWalk) {
                    input.forceSafeWalk = true
                }

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

        private val ticking = ThreadLocal.withInitial { false }

        /**
         * A simulation ticks on this thread. Blocks reacting to it must not change the level or show particles.
         */
        @JvmStatic
        val isTicking: Boolean
            get() = ticking.get()

        @JvmStatic
        fun fromClientPlayer(input: SimulatedPlayerInput) = SimulatedPlayer(
            player,
            input,
            player.deltaMovement,
            continuesTick = PlayerSimulationCache.isLocalPlayerMoving,
        )

        @JvmStatic
        fun fromOtherPlayer(player: Player, input: SimulatedPlayerInput) = SimulatedPlayer(
            player,
            input,
            deltaMovement = player.position().subtract(player.lastPos),
            continuesTick = false,
        )
    }
}
