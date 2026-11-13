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
package net.ccbluex.liquidbounce.features.module.modules.movement.fly.modes.rewound

import net.ccbluex.liquidbounce.config.types.group.Mode
import net.ccbluex.liquidbounce.config.types.group.ModeValueGroup
import net.ccbluex.liquidbounce.config.types.list.Tagged
import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.PlayerMoveEvent
import net.ccbluex.liquidbounce.event.events.TransferOrigin
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.movement.fly.ModuleFly
import net.ccbluex.liquidbounce.utils.client.Timer
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention.FIRST_PRIORITY
import net.ccbluex.liquidbounce.utils.entity.getMovementDirectionOfInput
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.ccbluex.liquidbounce.utils.movement.DirectionalInput
import net.ccbluex.liquidbounce.utils.math.copy
import net.ccbluex.liquidbounce.utils.network.sendPacketSilently
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.MoverType
import net.minecraft.world.phys.Vec3
import kotlin.math.cos
import kotlin.math.sin

/**
 * Damage boost glide for Rewound, ported move for move from Exire 1.8.9 `OldNcpFlight` (mode `Old NCP`).
 *
 * Stage order, taken from the reference: [Stage.DAMAGE] sends [damagePairs] pairs of `y + 0.0625` and `y` moves
 * above the current position and ends with a ground move, [Stage.WAIT] stands still until a self velocity arrives
 * (skipped by default, the reference forces the flag), [Stage.JUMP] sets the jump motion and multiplies the speed
 * by the bunny factor, [Stage.BOOST] sets the first boost move and [Stage.GLIDE] keeps decaying it by
 * `speed - speed / decay` while the vertical motion is forced to zero and the position drops by 1e-14 per tick so
 * the server keeps reading ground under the player.
 *
 * Every outgoing move packet is rewritten to `onGround = true`, exactly like the reference; that is what keeps the
 * set back chain from seeing the hover. The damage packets are sent silently, so the rewrite does not touch their
 * own ground flags.
 *
 * @anticheat NoCheatPlus (Rewound fork, based on April 2017)
 * @testedOn rewound.cc
 */
@Suppress("TooManyFunctions")
internal object FlyRewound20261112 : Mode("Rewound-2026-11-12", listOf("Rewound")) {

    override val parent: ModeValueGroup<*>
        get() = ModuleFly.modes

    private val speed by float("Speed", 4.0f, 3.5f..5.5f)
    private val decay by float("Decay", 159.0f, 159.0f..600.0f)
    private val boostTimer by float("BoostTimer", 1.0f, 1.0f..3.5f)
    private val boostTicks by int("BoostTicks", 10, 1..40, "ticks")
    private val useTimer by boolean("UseTimer", false)
    private val waitForDamage by boolean("WaitForDamage", false)
    private val waitTicks by int("WaitTicks", 40, 1..100, "ticks")

    /**
     * Fall distance the damage run fakes, as [damagePairs] pairs of `y + step` and `y`. The reference used 60 pairs
     * of 0.0625 (3.75 blocks, 121 packets). NoFall needs 4.0 blocks for damage and SurvivalFly allows a 0.5 rise
     * from the ground, so 0.5 steps reach the same fall distance with 19 packets instead of 121 and stay inside the
     * 22 moves per second budget.
     */
    private val damagePairs by int("DamagePairs", 9, 1..120, "pairs")
    private val damageStep by float("DamageStep", 0.5f, 0.0625f..0.5f)

    private val verticalMode by enumChoice("Vertical", VerticalMode.BOTH)
    private val climbSpeed by float("ClimbSpeed", 0.25f, 0.0f..0.25f)
    private val sinkSpeed by float("SinkSpeed", 0.25f, 0.0f..0.42f)
    private val targetTolerance by float("TargetTolerance", 0.35f, 0.05f..1.0f)
    private val debug by boolean("Debug", false)

    /** Height the fly climbs or sinks to, or null to follow the keys. */
    var targetY: Double? = null

    enum class Stage {
        DAMAGE,
        WAIT,
        JUMP,
        BOOST,
        GLIDE
    }

    var stage = Stage.DAMAGE
        private set

    /** Whether the boost has started; the GameBot does not count a flight as landed before this. */
    val boosting: Boolean
        get() = stage == Stage.BOOST || stage == Stage.GLIDE

    /** Why the flight gave up, such as `no damage`, or null while it goes on. */
    var abortReason: String? = null
        private set

    /**
     * The port keeps walking through the damage stage and jumps out of the walking momentum, so the GameBot must
     * not stop the input for the flight at any stage.
     */
    val steering: Boolean
        get() = true

    /** Blocks per tick right after the boost, for the GameBot's path planning. */
    val boostPerTick: Double
        get() = BASE_SPEED * speed
    val climbPerTick: Double
        get() = climbSpeed.toDouble()
    val sinkPerTick: Double
        get() = sinkSpeed.toDouble()

    private var moveSpeed = 0.0
    private var boostTick = 0
    private var damaged = false
    private var waited = 0
    private var wantsUp = false
    private var wantsDown = false

    private val speedAmplifier: Int
        get() = player.getEffect(MobEffects.SPEED)?.amplifier?.plus(1) ?: 0
    private val slownessAmplifier: Int
        get() = player.getEffect(MobEffects.SLOWNESS)?.amplifier?.plus(1) ?: 0

    override fun enable() {
        moveSpeed = 0.0
        stage = Stage.DAMAGE
        boostTick = 0
        damaged = false
        waited = 0
        wantsUp = false
        wantsDown = false
        abortReason = null
        targetY = null
    }

    override fun disable() {
        player.deltaMovement = player.deltaMovement.copy(x = 0.0, z = 0.0)
        Timer.requestTimerSpeed(1.0f, Priority.IMPORTANT_FOR_USAGE_1, ModuleFly)
    }

    private fun abort(reason: String) {
        abortReason = reason
        ModuleFly.enabled = false
    }

    @Suppress("unused")
    private val packetHandler = handler<PacketEvent>(priority = FIRST_PRIORITY) { event ->
        when (val packet = event.packet) {
            is ClientboundSetEntityMotionPacket if packet.id == player.id -> {
                damaged = true
                event.cancelEvent()
            }

            is ClientboundPlayerPositionPacket -> {
                if (debug) {
                    logger.info("[Fly] S08 to={} at={} stage={}", packet.change.position, player.position(), stage)
                }
                moveSpeed = baseSpeed()
                boostTick = Int.MAX_VALUE
            }
        }
    }

    /**
     * The reference forces every move to read as on ground, which is what keeps the hover alive. The field is
     * final, so the packet is rebuilt and sent silently instead of being written to; this runs last so the
     * rotations of the original packet are already in its values.
     */
    @Suppress("unused")
    private val groundHandler = handler<PacketEvent>(priority = EventPriorityConvention.READ_FINAL_STATE) { event ->
        if (event.origin != TransferOrigin.OUTGOING) {
            return@handler
        }

        val packet = event.packet
        if (packet !is ServerboundMovePlayerPacket || packet.isOnGround) {
            return@handler
        }

        event.cancelEvent()

        sendPacketSilently(
            when (packet) {
                is ServerboundMovePlayerPacket.Pos -> ServerboundMovePlayerPacket.Pos(
                    packet.getX(0.0), packet.getY(0.0), packet.getZ(0.0), true, false
                )

                is ServerboundMovePlayerPacket.PosRot -> ServerboundMovePlayerPacket.PosRot(
                    packet.getX(0.0), packet.getY(0.0), packet.getZ(0.0),
                    packet.getYRot(0.0f), packet.getXRot(0.0f), true, false
                )

                is ServerboundMovePlayerPacket.Rot -> ServerboundMovePlayerPacket.Rot(
                    packet.getYRot(0.0f), packet.getXRot(0.0f), true, false
                )

                else -> ServerboundMovePlayerPacket.StatusOnly(true, false)
            }
        )
    }

    @Suppress("unused")
    private val movementInputHandler = handler<MovementInputEvent> { event ->
        wantsUp = event.jump
        wantsDown = event.sneak
        event.jump = false
    }

    @Suppress("unused")
    private val moveHandler = handler<PlayerMoveEvent> { event ->
        if (event.type != MoverType.SELF || event.simulated) {
            return@handler
        }

        val direction = heading()

        if (!damaged && stage == Stage.WAIT) {
            if (waited++ > waitTicks) {
                abort("no damage")
                return@handler
            }

            strafe(event, 0.0, direction)
            return@handler
        }

        if (player.horizontalCollision) {
            moveSpeed = baseSpeed()
        }

        val current = stage
        var skipToBoost = false

        when (current) {
            Stage.DAMAGE -> if (player.fallDistance < FALL_DISTANCE) {
                sendDamagePackets()

                if (!waitForDamage) {
                    damaged = true
                }
            } else {
                // Falling in from a cage drop already has the fall distance the damage packets would fake.
                skipToBoost = true
            }

            Stage.WAIT -> moveSpeed = baseSpeedOf(WAIT_SPEED)

            Stage.JUMP -> {
                setY(event, JUMP_MOTION)
                moveSpeed *= BUNNY_MOTION
            }

            Stage.BOOST -> {
                moveSpeed = baseSpeed() * speed
                boostTick = 0
            }

            Stage.GLIDE -> {
                moveSpeed -= moveSpeed / decay
                applyGlide(event)
            }
        }

        if (current.ordinal >= Stage.BOOST.ordinal) {
            val normal = if (useTimer) 1.0766f else 1.0f
            val boost = boostTimer
            val ticks = boostTicks
            val timerSpeed = if (boostTick < ticks && boost > normal) {
                boost - (boost - normal) * boostTick / ticks
            } else {
                normal
            }
            Timer.requestTimerSpeed(timerSpeed, Priority.IMPORTANT_FOR_USAGE_1, ModuleFly)
            if (boostTick < Int.MAX_VALUE) {
                boostTick++
            }
        }

        stage = when {
            current == Stage.DAMAGE && skipToBoost -> Stage.BOOST
            current == Stage.DAMAGE -> Stage.WAIT
            current == Stage.WAIT -> Stage.JUMP
            current == Stage.JUMP -> Stage.BOOST
            else -> Stage.GLIDE
        }

        strafe(event, maxOf(moveSpeed, baseSpeed()), direction)

        if (debug) {
            logger.info(
                "[Fly] tick stage={} moveSpeed={} input={} motion={} move={} pos={} col={} hp={}",
                current, moveSpeed, DirectionalInput(player.input), player.deltaMovement, event.movement,
                player.position(), player.horizontalCollision, player.health
            )
        }
    }

    /** Sends the damage run above the current position, then a ground move that closes it. */
    private fun sendDamagePackets() {
        val y = player.y
        repeat(damagePairs) {
            // The return move reads as on ground, so the server keeps its set back and reset position at y
            // instead of at the risen point; the jump then starts from the ground.
            sendPos(y + damageStep.toDouble(), false)
            sendPos(y, true)
        }
    }

    /**
     * The glide keeps the horizontal motion decaying and forces the vertical one to zero. Without a vertical
     * request the position drops by 1e-14 whenever no block sits below the box, which is the reference's way of
     * keeping the server's on-ground reading under the hover.
     */
    private fun applyGlide(event: PlayerMoveEvent) {
        val vertical = verticalStep()
        setY(event, vertical)

        if (vertical == 0.0 && !isOnGround(-1.0E-4)) {
            player.setPos(player.x, player.y - HOLD_OFFSET, player.z)
        }
    }

    private fun verticalStep(): Double = when (verticalRequest()) {
        1 -> climbSpeed.toDouble()
        -1 -> -sinkSpeed.toDouble()
        else -> 0.0
    }

    private fun verticalRequest(): Int = when (verticalMode) {
        VerticalMode.HOLD -> 0
        VerticalMode.KEYS -> keysRequest()
        VerticalMode.TARGET -> targetRequest()
        VerticalMode.BOTH -> keysRequest().takeIf { it != 0 } ?: targetRequest()
    }

    private fun keysRequest(): Int = when {
        wantsUp && !wantsDown -> 1
        wantsDown && !wantsUp -> -1
        else -> 0
    }

    private fun targetRequest(): Int {
        val target = targetY ?: return 0
        val delta = target - player.y
        return when {
            delta > targetTolerance -> 1
            delta < -targetTolerance -> -1
            else -> 0
        }
    }

    private fun isOnGround(offset: Double) =
        !world.noCollision(player, player.boundingBox.move(0.0, offset, 0.0))

    /**
     * Heading of the flight: the movement input direction while keys hold one, otherwise the rotation yaw. The
     * move's own displacement is not used, the navigator's path corrections on the walk-up would bend the boost.
     */
    private fun heading(): Vec3 {
        val input = DirectionalInput(player.input)
        val yaw = if (input.isMoving) {
            player.getMovementDirectionOfInput(input)
        } else {
            player.yRot
        }
        val radians = Math.toRadians(yaw.toDouble())
        return Vec3(-sin(radians), 0.0, cos(radians))
    }

    private fun strafe(event: PlayerMoveEvent, speed: Double, direction: Vec3) {
        val x = direction.x * speed
        val z = direction.z * speed
        event.movement = event.movement.copy(x = x, z = z)
        player.deltaMovement = player.deltaMovement.copy(x = x, y = event.movement.y, z = z)
    }

    private fun setY(event: PlayerMoveEvent, y: Double) {
        player.deltaMovement = player.deltaMovement.copy(y = y)
        event.movement = event.movement.copy(y = y)
    }

    private fun sendPos(y: Double, onGround: Boolean) {
        sendPacketSilently(
            ServerboundMovePlayerPacket.Pos(
                player.x,
                y,
                player.z,
                onGround,
                player.horizontalCollision
            )
        )
    }

    private fun baseSpeed(): Double = baseSpeedOf(BASE_SPEED)

    /** Exire's `getBaseMoveSpeed(scale)`: the scale goes through the speed and slowness potions. */
    private fun baseSpeedOf(scale: Double): Double {
        var base = scale * (1.0 + 0.2 * speedAmplifier)
        if (slownessAmplifier > 0) {
            base /= 1.0 + 0.2 * slownessAmplifier
        }
        return base
    }

    private enum class VerticalMode(override val tag: String) : Tagged {
        HOLD("Hold"),
        KEYS("Keys"),
        TARGET("Target"),
        BOTH("Both")
    }

    private const val BASE_SPEED = 0.2873
    private const val WAIT_SPEED = 0.3
    private const val JUMP_MOTION = 0.3999
    private const val BUNNY_MOTION = 2.149
    private const val FALL_DISTANCE = 3.75f
    private const val HOLD_OFFSET = 1.0E-14
}
