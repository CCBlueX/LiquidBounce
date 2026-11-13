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
package net.ccbluex.liquidbounce.features.module.modules.movement.speed.modes.rewound

import net.ccbluex.liquidbounce.config.types.group.Mode
import net.ccbluex.liquidbounce.config.types.group.ModeValueGroup
import net.ccbluex.liquidbounce.config.types.list.Tagged
import net.ccbluex.liquidbounce.event.EventState
import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.PlayerMoveEvent
import net.ccbluex.liquidbounce.event.events.PlayerNetworkMovementTickEvent
import net.ccbluex.liquidbounce.event.events.PlayerStepSuccessEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.movement.speed.ModuleSpeed
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold
import net.ccbluex.liquidbounce.utils.client.Chronometer
import net.ccbluex.liquidbounce.utils.client.Timer
import net.ccbluex.liquidbounce.utils.client.isOlderThan26_3
import net.ccbluex.liquidbounce.utils.entity.getMovementDirectionOfInput
import net.ccbluex.liquidbounce.utils.movement.DirectionalInput
import net.ccbluex.liquidbounce.utils.entity.withStrafe
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention.FIRST_PRIORITY
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.ccbluex.liquidbounce.utils.math.anyNotEmpty
import net.ccbluex.liquidbounce.utils.math.copy
import net.ccbluex.liquidbounce.utils.network.sendPacketSilently
import net.minecraft.core.BlockPos
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket
import net.minecraft.util.Mth
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.MoverType
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

/**
 * Bunny hop for Rewound (2020 Hypixel / NoCheatPlus fork), ported from LiquidBounce Legacy
 * and tested on rewound.cc.
 *
 * [Variant] adds the remaining Exire 1.8.9 NCP modes on top of the default hop.
 *
 * @anticheat NoCheatPlus (Rewound fork, based on April 2017)
 * @testedOn rewound.cc
 */
@Suppress("TooManyFunctions", "CognitiveComplexMethod")
class SpeedRewound20261112(override val parent: ModeValueGroup<*>) : Mode("Rewound-2026-11-12", listOf("Rewound")) {

    private val variant by enumChoice("Variant", Variant.REWOUND)
    private val timer by boolean("Timer", true)
    private val timerRewound by float("TimerRewound", 1.2f, 1.0f..3.0f)
    private val timerRewound2 by float("TimerRewound2", 1.085f, 1.0f..3.0f)
    private val timerUpdatedNcp by float("TimerUpdatedNcp", 1.0766f, 1.0f..3.0f)
    private val damageBoost by boolean("DamageBoost", false)
    private val damageBoostFactor by float("DamageBoostFactor", 0.42f, 0.0f..2.0f)
    private val fastFall by enumChoice("FastFall", FastFall.OFF)
    private val capAverage by float("CapAverage", 1.34f, 1.0f..4.0f)
    private val capHop by float("CapHop", 2.14f, 1.32f..2.15f)
    private val extraPacketsPerSecond by int("ExtraPacketsPerSecond", 20, 0..400)
    private val clipBudget = PacketBudget()

    /**
     * 26.3 puts a tick end in front of every extra position packet, older servers get one packet per clip step.
     */
    private val extraPacketCost
        get() = if (isOlderThan26_3) 1 else 2

    /**
     * Ticks since the last hop. Negative while recovering from a setback or a wall.
     */
    private var stage = 2
    private var collided = false
    private var stair = 0.0
    private var hopStreak = 0.0
    private var slowStreakHop = false
    private var slowedBySetback = false
    private val setbackChronometer = Chronometer()

    private var variantStage = 0
    private var moveSpeed = 0.0
    private var lastDist = 0.0
    private var boost = false
    private var waitingTicks = 0
    private var velocityMoveSpeed = 0.0
    private var lastCapSpeed = 0.0
    private val capRatios = ArrayDeque<Double>()

    private val speedAmplifier
        get() = player.getEffect(MobEffects.SPEED)?.amplifier?.plus(1) ?: 0

    private val jumpAmplifier
        get() = player.getEffect(MobEffects.JUMP_BOOST)?.amplifier?.plus(1) ?: 0

    private val baseSpeed
        get() = BASE_SPEED * (1.0 + 0.2 * speedAmplifier)

    private val recentlySetBack
        get() = !setbackChronometer.hasAtLeastElapsed(SETBACK_SLOW_MS)

    override fun enable() {
        stage = 2
        collided = player.horizontalCollision
        stair = 0.0
        hopStreak = 0.0
        slowStreakHop = false
        slowedBySetback = false
        variantStage = 0
        moveSpeed = 0.0
        lastDist = 0.0
        boost = false
        waitingTicks = 0
        velocityMoveSpeed = 0.0
        lastCapSpeed = 0.0
        fillCapWindow()
        clipBudget.clear()
    }

    // Holding jump with Scaffold builds a tower, which needs its own jumps
    private val isTowering
        get() = ModuleScaffold.running && mc.options.keyJump.isDown

    @Suppress("unused")
    private val movementInputHandler = handler<MovementInputEvent> { event ->
        if (!player.isInLiquid && !isTowering) {
            event.jump = false
        }
    }

    @Suppress("unused")
    private val packetHandler = handler<PacketEvent>(priority = FIRST_PRIORITY) { event ->
        when (val packet = event.packet) {
            is ClientboundPlayerPositionPacket -> {
                stage = -4
                setbackChronometer.reset()
                variantStage = 0
                moveSpeed = 0.0
                lastDist = 0.0
                boost = false
                waitingTicks += 3
                lastCapSpeed = 0.0
                fillCapWindow()
            }

            is ClientboundSetEntityMotionPacket if packet.id == player.id -> {
                velocityMoveSpeed = packet.movement.horizontalDistance()
            }
        }
    }

    /**
     * Records the distance of the tick that is about to be sent and applies the Rewound 2 ground offset.
     */
    @Suppress("unused")
    private val networkTickHandler = handler<PlayerNetworkMovementTickEvent> { event ->
        if (event.state != EventState.PRE || variant != Variant.REWOUND_2) {
            return@handler
        }

        lastDist = Mth.length(player.x - player.xo, player.z - player.zo)

        if (movingInput && player.verticalCollision && !player.isInLiquid) {
            event.y += GROUND_OFFSET
        }
    }

    @Suppress("unused")
    private val stepHandler = handler<PlayerStepSuccessEvent> { event ->
        if (variant != Variant.REWOUND || event.movementVec.y > 0.0 || event.adjustedVec.y <= 0.0) {
            return@handler
        }

        val height = event.adjustedVec.y
        if (height > 0.7) {
            hopStreak = 0.0
        }

        // Half a block high: a stair or slab, walk it instead of hopping.
        if (abs(height - 0.5) < 1.0E-4) {
            stair = 0.75
        }
    }

    @Suppress("unused")
    private val moveHandler = handler<PlayerMoveEvent> { event ->
        if (event.type != MoverType.SELF || isTowering) {
            return@handler
        }

        when (variant) {
            Variant.REWOUND -> rewoundMove(event)
            Variant.REWOUND_2 -> rewound2Move(event)
            Variant.UPDATED_NCP -> updatedNcpMove(event)
            Variant.FAST -> fastMove(event)
            Variant.CAP -> capMove(event)
        }
    }

    /**
     * Rides the NoCheatPlus horizontal acceleration check: it limits the 30 move average of the distance over the
     * sprint speed to [capAverage] and forgives any move that stays below the previous one, so every hop may reach
     * [capHop] times the last distance as long as the window average holds.
     */
    private fun capMove(event: PlayerMoveEvent) {
        val base = baseSpeedOf()

        if (!movingInput || player.isInLiquid) {
            capApply(event, base, base)
            return
        }

        val room = capRoom(base)
        val canHop = isNearGround() && !player.horizontalCollision && !recentlySetBack

        val speed = if (canHop && lastCapSpeed >= base && room >= CAP_MIN_HOP * base) {
            player.jumpFromGround()
            setMotionY(event, JUMP_MOTION + jumpAmplifier * 0.1)
            min(capHop * base, room)
        } else if (lastCapSpeed > base) {
            min(lastCapSpeed * CAP_DECREASE, room).coerceAtLeast(base)
        } else {
            base
        }

        capApply(event, speed, base)
    }

    /** Starts the window at the sprint speed the check divides by, so the first hop spends the whole budget. */
    private fun fillCapWindow() {
        capRatios.clear()
        repeat(CAP_WINDOW) {
            capRatios.addLast(1.0)
        }
    }

    /**
     * Distance the 30 move average still allows. The oldest entry leaves the window for the move that is about to
     * be recorded, so its ratio is added back before the cap is applied.
     */
    private fun capRoom(base: Double): Double {
        val sum = capRatios.sum()
        val evicted = capRatios.removeFirst()
        return (capAverage * CAP_WINDOW - sum + evicted).coerceAtLeast(1.0) * base
    }

    private fun capApply(event: PlayerMoveEvent, speed: Double, base: Double) {
        event.movement = event.movement.withStrafe(speed = speed)
        player.deltaMovement = player.deltaMovement.withStrafe(speed = speed)
        lastCapSpeed = speed
        capRatios.addLast((speed / base).coerceIn(1.0, CAP_RATIO_MAX))
        while (capRatios.size > CAP_WINDOW) {
            capRatios.removeFirst()
        }
        requestTimer(timerRewound)
    }

    private fun rewoundMove(event: PlayerMoveEvent) {
        if (player.horizontalCollision) {
            collided = true
        }

        if (collided) {
            stage = -1
        }

        if (stair > 0.0) {
            stair -= 0.25
        }

        hopStreak = (hopStreak - if (hopStreak > 1.0) 0.12 else 0.11).coerceAtLeast(0.0)

        if (!player.isInLiquid && isNearGround() && movingInput) {
            collided = player.horizontalCollision

            if (stage >= 0 || collided) {
                stage = 0

                if (stair == 0.0) {
                    player.jumpFromGround()
                    setMotionY(event, JUMP_MOTION + jumpAmplifier * 0.1)
                }

                hopStreak++
                slowStreakHop = hopStreak > 1.0 && !slowStreakHop
                hopStreak = hopStreak.coerceAtMost(1.12)
            }
        }

        if (movingInput && !collided) {
            requestTimer(timerRewound)
        }

        if (stage == 5 && !player.onGround()) {
            fastFall.motionY(player.deltaMovement.y)?.let { setMotionY(event, it) }
        }

        var speed = (stageSpeed(stage) + 0.0331) * 0.91
        if (stair > 0.0) {
            speed *= 0.7 - speedAmplifier * 0.1
        }

        if (stage < 0) {
            speed = baseSpeed
        }

        if (slowStreakHop) {
            speed *= 0.95
        }

        if (player.isInLiquid) {
            speed = 0.12
        }

        if (movingInput) {
            val target = boosted(speed)
            event.movement = event.movement.withStrafe(speed = target)
            player.deltaMovement = player.deltaMovement.withStrafe(speed = target)
            stage++
        }
    }

    private fun rewound2Move(event: PlayerMoveEvent) {
        if (player.isInLiquid || !movingInput) {
            requestTimer(1.0f)
            return
        }

        val base = baseSpeedOf()

        when (variantStage) {
            2 -> {
                if (player.onGround() && player.verticalCollision) {
                    requestTimer(1.0f)
                    setMotionY(event, HOP_JUMP_MOTION + jumpAmplifier * 0.1)
                    moveSpeed = base * 1.5
                }
            }

            3 -> moveSpeed = lastDist + 0.62 * (lastDist - base)

            else -> {
                if (variantStage == 7 && !player.onGround()) {
                    fastFall.motionY(player.deltaMovement.y)?.let { setMotionY(event, it) }
                }

                if (collidesWithMovement() || player.verticalCollision && player.onGround()) {
                    variantStage = 1
                    requestTimer(timerRewound2)
                }

                moveSpeed = lastDist - lastDist / 149.0
            }
        }

        moveSpeed = max(moveSpeed, base)
        event.movement = event.movement.withStrafe(speed = boosted(moveSpeed))
        variantStage++
    }

    private fun updatedNcpMove(event: PlayerMoveEvent) {
        if (player.onGround()) {
            variantStage = 0
        }

        if (!movingInput) {
            return
        }

        requestTimer(timerUpdatedNcp)
        val base = baseSpeedOf()

        when (variantStage) {
            0 -> {
                if (!player.onGround()) {
                    return
                }

                setMotionY(event, UPDATED_NCP_JUMP_MOTION)
                moveSpeed = baseSpeedOf(0.5)
            }

            1 -> moveSpeed = baseSpeedOf(0.34)

            else -> moveSpeed -= moveSpeed / 159.9
        }

        variantStage++
        event.movement = event.movement.withStrafe(speed = boosted(max(moveSpeed, base)))
    }

    private fun fastMove(event: PlayerMoveEvent) {
        if (!movingInput) {
            moveSpeed = 0.0
            event.movement = event.movement.copy(x = 0.0, z = 0.0)
            return
        }

        if (waitingTicks > 0) {
            waitingTicks--
            return
        }

        if (player.onGround() && !player.horizontalCollision) {
            if (fastClipAndFlag() > 0) {
                setMotionY(event, HOP_JUMP_MOTION)
                moveSpeed = baseSpeedOf(0.6)
                boost = true
            } else {
                // No packet budget left, so the jump gets no clipped ground behind it
                setMotionY(event, JUMP_MOTION)
                moveSpeed = baseSpeedOf()
            }
        } else if (boost) {
            moveSpeed = 1.2
            boost = false
            event.movement = event.movement.withStrafe(speed = boosted(moveSpeed))
            return
        } else {
            moveSpeed = max(moveSpeed - moveSpeed / 154.0, baseSpeedOf())
        }

        event.movement = event.movement.withStrafe(speed = boosted(moveSpeed))
    }

    /**
     * Clips 0.15 blocks along the movement direction up to seven times and sends a ground packet after each step.
     * The step is forward when the column ahead is free and has ground below it, backwards otherwise.
     * [clipBudget] stops the steps once [extraPacketsPerSecond] is used up and returns how many went out.
     */
    private fun fastClipAndFlag(): Int {
        val yaw = Math.toRadians(player.getMovementDirectionOfInput().toDouble())
        val sin = sin(yaw)
        val cos = cos(yaw)

        var steps = 0
        repeat(CLIP_STEPS) { step ->
            if (!clipBudget.tryTake(extraPacketsPerSecond, extraPacketCost)) {
                return steps
            }

            val offset = CLIP_OFFSET * step
            val column = BlockPos.containing(player.position())
            val feet = offsetBlock(player.x, player.y, player.z, sin, cos, offset)
            val head = offsetBlock(column.x.toDouble(), column.y + 1.0, column.z.toDouble(), sin, cos, offset)
            val below = offsetBlock(column.x.toDouble(), column.y - 1.0, column.z.toDouble(), sin, cos, offset)

            val clear = !isFullBlock(feet) && !isFullBlock(head) && !world.getBlockState(below).isAir
            val clip = if (clear) CLIP_OFFSET else -CLIP_OFFSET

            player.setPos(player.x - sin * clip, player.y, player.z + cos * clip)
            sendPacketSilently(
                ServerboundMovePlayerPacket.Pos(
                    player.x, player.y, player.z, true, player.horizontalCollision
                )
            )
            steps++
        }
        return steps
    }

    private fun offsetBlock(
        x: Double,
        y: Double,
        z: Double,
        sin: Double,
        cos: Double,
        offset: Double,
    ) = BlockPos.containing(x - sin * offset, y, z + cos * offset)

    private fun isFullBlock(pos: BlockPos): Boolean {
        val state = world.getBlockState(pos)
        return state.isCollisionShapeFullBlock(world, pos)
    }

    private fun collidesWithMovement() =
        world.getBlockCollisions(player, player.boundingBox.move(0.0, player.deltaMovement.y, 0.0)).anyNotEmpty()

    private fun setMotionY(event: PlayerMoveEvent, motionY: Double) {
        player.deltaMovement = player.deltaMovement.copy(y = motionY)
        event.movement = event.movement.copy(y = motionY)
    }

    private fun requestTimer(timerSpeed: Float) {
        if (timer) {
            Timer.requestTimerSpeed(timerSpeed, Priority.IMPORTANT_FOR_USAGE_1, ModuleSpeed)
        }
    }

    /**
     * Exire applies the boosted knockback speed for a single tick, when hurt time counts down from ten to nine.
     */
    private fun boosted(speed: Double): Double {
        if (!damageBoost || player.hurtTime != HURT_TIME_PEAK) {
            return speed
        }

        return speed + velocityMoveSpeed * damageBoostFactor
    }

    /**
     * Exire base speed with the slowness potion divider.
     */
    private fun baseSpeedOf(scale: Double = BASE_SPEED): Double {
        var speed = scale * (1.0 + 0.2 * speedAmplifier)
        player.getEffect(MobEffects.SLOWNESS)?.let { speed /= 1.0 + 0.2 * (it.amplifier + 1) }
        return speed
    }

    private fun isNearGround() = !world.noCollision(player, player.boundingBox.move(0.0, -0.01, 0.0))

    /**
     * Whether the player is moving: the keys [withStrafe] reads, or the displacement of the last tick when a
     * navigator drives the movement without touching them.
     */
    private val movingInput
        get() = DirectionalInput(player.input).isMoving ||
            Mth.length(player.x - player.xo, player.z - player.zo) > 1.0E-3

    private fun stageSpeed(stage: Int): Double {
        val amplifier = speedAmplifier
        val first = 0.4145 + amplifier / 12.5

        var speed = when {
            stage == 0 -> {
                slowedBySetback = recentlySetBack
                0.64 + (amplifier + 0.028 * amplifier) * 0.134
            }
            stage == 1 -> first
            stage >= 2 -> first - stage / 250.0
            else -> baseSpeed + 0.028 * amplifier + amplifier / 15.0
        }

        if (slowedBySetback || recentlySetBack || collided) {
            speed = if (stage == 0) 0.0 else 0.2
        }

        return if (slowedBySetback) speed else max(speed, baseSpeed + 0.028 * amplifier)
    }

    private enum class Variant(override val tag: String) : Tagged {
        REWOUND("Rewound"),
        REWOUND_2("Rewound 2"),
        UPDATED_NCP("Updated NCP"),
        FAST("Fast"),
        CAP("Cap")
    }

    /**
     * Limits the extra position packets of [fastClipAndFlag] to [extraPacketsPerSecond] over any one second, so the
     * proxy packet limiter never sees more. Steps over the budget are skipped and go out once the window has room
     * again.
     */
    private class PacketBudget {

        private val sentAt = ArrayDeque<Long>()

        fun tryTake(perSecond: Int, packetCost: Int): Boolean {
            val stepsPerSecond = perSecond / packetCost
            if (stepsPerSecond <= 0) {
                return false
            }

            val now = System.currentTimeMillis()
            while (sentAt.isNotEmpty() && now - sentAt.first() >= WINDOW_MS) {
                sentAt.removeFirst()
            }

            if (sentAt.size >= stepsPerSecond) {
                return false
            }

            sentAt.addLast(now)
            return true
        }

        fun clear() = sentAt.clear()

        private companion object {
            const val WINDOW_MS = 1_000L
        }
    }

    private enum class FastFall(override val tag: String) : Tagged {
        OFF("Off"),
        SNAP("Snap"),
        PUSH("Push");

        fun motionY(current: Double): Double? = when (this) {
            OFF -> null
            SNAP -> -0.18517061160392762
            PUSH -> current - (0.18999 + 4.0E-8 * Random.nextDouble())
        }
    }

    private companion object {
        const val BASE_SPEED = 0.2873
        const val JUMP_MOTION = 0.407
        const val HOP_JUMP_MOTION = 0.42
        const val UPDATED_NCP_JUMP_MOTION = 0.39999
        const val GROUND_OFFSET = 7.435E-4
        const val HURT_TIME_PEAK = 9
        const val CLIP_STEPS = 7
        const val CLIP_OFFSET = 0.15
        const val SETBACK_SLOW_MS = 500L
        const val CAP_WINDOW = 30
        const val CAP_RATIO_MAX = 10.0
        const val CAP_MIN_HOP = 1.32
        const val CAP_DECREASE = 0.993
    }

}
