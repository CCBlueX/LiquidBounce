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
package net.ccbluex.liquidbounce.features.module.modules.world.scaffold.bridging

import net.ccbluex.agent.simulation.SimulatedInput
import net.ccbluex.agent.simulation.SimulatedPlayer
import net.ccbluex.agent.v26_3.toAgent
import net.ccbluex.liquidbounce.config.types.list.Tagged
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.event.events.PlayerAfterJumpEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold.ScaffoldRotationValueGroup
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.aim.AimGoal
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.aiming.utils.RotationUtil.angleDifference
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.warning
import net.ccbluex.liquidbounce.utils.entity.airTicks
import net.ccbluex.liquidbounce.utils.entity.fromClientPlayer
import net.ccbluex.liquidbounce.utils.entity.isCloseToEdge
import net.ccbluex.liquidbounce.utils.entity.moving
import net.ccbluex.liquidbounce.utils.movement.DirectionalInput
import net.minecraft.core.BlockPos
import net.minecraft.world.phys.Vec3
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.sign

/**
 * Runs forwards and jumps at the end of the row, as telly bridgers do, once the jump comes down on the blocks the aim
 * can put down on the way. Right after the jump the view turns round to the end of the row and the blocks go down
 * under the jump, the row it jumped off rather than one under the jump unless SameY has it build up. An aim that is not
 * instant lets go of W meanwhile, which keeps the jump short enough for the blocks it gets down. Once the jump comes
 * down on them the view turns forwards again and the next jump follows. While no block
 * [can go down in the air][ModuleScaffold.canPlaceInAir], it bridges like Normal.
 */
object ScaffoldBridgingTelly : ScaffoldBridging("Telly", "Sneak") {

    /** Whether it tellies: the blocks go down in the air, and the view turns round twice a jump. */
    val isTellying: Boolean
        get() = running && ModuleScaffold.canPlaceInAir

    /** Whether the view faces forwards: on the ground, right after the jump and once the jump has its blocks. */
    val isFacingForward: Boolean
        get() = isTellying &&
                (player.airTicks <= straightTicks || isLanding) &&
                ticksUntilJump >= jumpTicks &&
                !(ModuleScaffold.isTowering && aimOnTower)

    val isTellyBridging: Boolean
        get() = ticksUntilJump >= jumpTicks && player.moving && isTellying

    /**
     * Whether the aim turns around in the first ticks of a jump, onto a goal as wide as the end of the row looks from
     * up there, and back. Human does, in the rehearsed sweeps of a telly bridger.
     */
    val canTurnAround: Boolean
        get() = ScaffoldRotationValueGroup.calculateTicks(
            Rotation(180f, 70f),
            AimGoal.Kind.FLICK,
            from = Rotation.ZERO,
            size = 40f,
        ) <= 3

    /**
     * Where the view looks in the air: back down the row, on the 45 degree step it runs along, and steeply down, where
     * the end of the row comes into view a tick after the eyes pass it. The blocks go down on the faces nearest to it.
     */
    fun waitingLook(): Rotation? = if (isTellying && !player.onGround() && !isFacingForward) {
        Rotation((movement() ?: player.yRot) + 180f, 84f)
    } else {
        null
    }

    /**
     * Where the next block is placed from in the air: where this tick ends, as the next click goes out from there. The
     * placements of the last walk say nothing about it.
     */
    val placementPos: Vec3?
        get() = if (isTellying && !player.onGround()) player.position().add(player.deltaMovement) else null

    /** Forwards a few degrees off the camera: the jump goes the way the view does, and a diagonal leaves no room. */
    override val tolerance: Float
        get() = if (isFacingForward) FORWARD_TOLERANCE else 0f

    private var ticksUntilJump = 0

    /** Whether the jump under way comes down on the blocks already placed, so the view turns forwards again. */
    private var isLanding = false

    /** Where the jump under way took off. */
    private var jumpY = 0.0

    val resetMode by enumChoice("ResetMode", ResetMode.RESET)
    private val straightTicks by int("Straight", 0, 0..5, "ticks")
    private val jumpTicksOpt by intRange("Jump", 0..0, 0..10, "ticks")
    private val aimOnTower by boolean("AimOnTower", true)
    private var jumpTicks = jumpTicksOpt.random()

    override fun enable() {
        if (!canTurnAround) {
            chat(warning(ModuleScaffold.message("telly.slowAim")), ModuleScaffold)
        }
    }

    override fun disable() {
        isLanding = false
        super.disable()
    }

    @Suppress("unused")
    private val gameHandler = handler<GameTickEvent> {
        if (player.onGround()) {
            ticksUntilJump++
        }
    }

    @Suppress("unused")
    private val movementInputHandler = handler<MovementInputEvent> { event ->
        // The keys of this tick: last tick's are the ones let go of in the air
        if (!event.directionalInput.isMoving || ModuleScaffold.blockCount <= 0 || !ModuleScaffold.canPlaceInAir) {
            return@handler
        }

        if (!player.onGround()) {
            // Once W from here comes down on the blocks, it stays held: the jump is not let go of again
            isLanding = isLanding || !ModuleScaffold.landsOffBridge(event.directionalInput)
            val view = RotationManager.currentRotation
            if (!isLanding && !ModuleScaffold.hasInstantAim && view != null &&
                abs(angleDifference(view.yaw, player.yRot)) > 90f
            ) {
                // Turned round, a telly bridger lets go of W, and pulls back while it would carry him past his blocks
                val back = event.directionalInput.invert()
                val isShort = landsOnBlocks(DirectionalInput.NONE, jump = false) || !landsOnBlocks(back, jump = false)
                event.directionalInput = if (isShort) DirectionalInput.NONE else back
            }
            return@handler
        }
        isLanding = false

        // A jump with the view turned away has no sprint, and its push goes the way the view does
        val view = RotationManager.currentRotation
        val isForward = view == null || abs(angleDifference(view.yaw, player.yRot)) <= FORWARD_TOLERANCE

        // A held jump waits as well, until it goes forwards and comes down on the blocks the aim gets down
        if (event.jump && !ModuleScaffold.hasInstantAim && !ModuleScaffold.isTowering &&
            !(isForward && landsOnBlocks(event.directionalInput, jump = true))
        ) {
            event.jump = false
        }

        val isStraight = RotationManager.currentRotation == null || straightTicks == 0

        when (resetMode) {
            ResetMode.REVERSE -> event.jump = true
            // At the end of the row, once the jump comes down on the blocks the aim gets down on the way
            ResetMode.RESET -> if (isStraight && isForward && ticksUntilJump >= jumpTicks &&
                player.isCloseToEdge(event.directionalInput, 0.5) &&
                landsOnBlocks(event.directionalInput, jump = true)
            ) {
                event.jump = true
            }
        }
    }

    /**
     * Whether the jump under way, or one now with [input], comes down on blocks: ones there already, or ones the aim
     * puts down on the way. A block goes down once the eyes are some way past the face it goes on, unless the aim is
     * instant: closer, the face is a sliver straight down. Then after two ticks to turn round. It lets go of W once
     * turned round.
     */
    private fun landsOnBlocks(input: DirectionalInput, jump: Boolean): Boolean {
        val simulation = SimulatedPlayer.fromClientPlayer(
            SimulatedInput(input.toInput(jump = jump, sprint = player.isSprinting).toAgent())
        )
        val letGoAt = if (jump && !ModuleScaffold.hasInstantAim) 2 else 0
        return landsOnBlocks(simulation, if (jump) player.y else jumpY, letGoAt)
    }

    /** Whether [simulation] comes down on blocks at the height of [groundY], letting go of W on tick [letGoAt]. */
    private fun landsOnBlocks(simulation: SimulatedPlayer, groundY: Double, letGoAt: Int): Boolean {
        val rowY = floor(groundY) - 1
        val isInstant = ModuleScaffold.hasInstantAim
        val lag = if (isInstant) 0.0 else 0.3
        val reached = ArrayDeque<BlockPos>()
        val placed = ArrayList<BlockPos>()
        // Over the edge already, the middle of the player has its block still to come
        var last = BlockPos.containing(simulation.position().x, rowY, simulation.position().z)
        listOf(last).filterTo(reached) { world.getBlockState(it).isAir }
        var due = letGoAt + 1
        for (tick in 1..20) {
            simulation.tick()
            if (tick == letGoAt) {
                simulation.input = SimulatedInput(DirectionalInput.NONE.toInput().toAgent())
            }
            val pos = simulation.position()
            if (pos.y <= groundY) {
                return isStanding(pos, rowY, placed)
            }
            // Past the face on each side the jump crosses one: on a diagonal both, the east and the south face
            val motion = simulation.deltaMovement
            val cell = BlockPos.containing(pos.x - lag * sideOf(motion.x), rowY, pos.z - lag * sideOf(motion.z))
            steps(last, cell).filterTo(reached) { it !in placed && it !in reached && world.getBlockState(it).isAir }
            last = cell
            if (reached.isNotEmpty() && tick >= due) {
                placed += reached.removeFirst()
                due = tick + if (isInstant) 1 else 2
            }
        }
        return false
    }

    /** The blocks a row needs from [last] to [cell]: one goes down against a face, so a diagonal step takes two. */
    private fun steps(last: BlockPos, cell: BlockPos) = when {
        cell == last -> emptyList()
        cell.x != last.x && cell.z != last.z -> listOf(BlockPos(cell.x, cell.y, last.z), cell)
        else -> listOf(cell)
    }

    /** Which way a movement of [speed] a tick goes along an axis, none for a drift. */
    private fun sideOf(speed: Double) = if (abs(speed) < 0.05) 0.0 else sign(speed)

    /** Whether a player at [pos] stands on the row at [rowY]: on blocks there already, or [placed] ones. */
    private fun isStanding(pos: Vec3, rowY: Double, placed: List<BlockPos>): Boolean {
        val half = player.bbWidth / 2 - 0.01
        return listOf(-half, half).any { x ->
            listOf(-half, half).any { z ->
                val below = BlockPos.containing(pos.x + x, rowY, pos.z + z)
                below in placed || !world.getBlockState(below).isAir
            }
        }
    }

    @Suppress("unused")
    private val afterJumpHandler = handler<PlayerAfterJumpEvent> {
        ticksUntilJump = 0
        jumpTicks = jumpTicksOpt.random()
        jumpY = player.y
    }

    /** Degrees the view may rest off the camera forwards, and still jump. */
    private const val FORWARD_TOLERANCE = 3f

    enum class ResetMode(override val tag: String) : Tagged {
        REVERSE("Reverse"),
        RESET("Reset")
    }

}
