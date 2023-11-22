package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.event.events.*
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.player.ModuleBlink
import net.ccbluex.liquidbounce.render.drawLineStrip
import net.ccbluex.liquidbounce.render.engine.Color4b
import net.ccbluex.liquidbounce.render.engine.Vec3
import net.ccbluex.liquidbounce.render.renderEnvironmentForWorld
import net.ccbluex.liquidbounce.render.withColor
import net.ccbluex.liquidbounce.utils.combat.findEnemy
import net.ccbluex.liquidbounce.utils.entity.SimulatedPlayer
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket
import kotlin.math.min

/**
 * TickBase module
 *
 * Calls tick function to speed up, when needed
 */

object ModuleTickBase : Module("TickBase", Category.COMBAT) {

    private val distanceToWork by floatRange("DistanceToWork", 3f..4f, 0f..10f)
    private val balanceRecoveryIncrement by float("BalanceRecoverIncrement", 1f, 0f..2f)
    private val balanceMaxValue by int("BalanceMaxValue", 20, 0..200)
    private val maxTicksAtATime by int("MaxTicksAtATime", 2, 1..20)
    private val pauseOnFlag by boolean("PauseOfFlag", true)
    private val pauseAfterTick by int("PauseAfterTick", 3, 0..100)
    private var ticksToSkip = 0
    private var tickBalance = 0f
    private var reachedTheLimit = false

    private val simLines = mutableListOf<Vec3>()

    val tickHandler = handler<PlayerTickEvent> {
        // We do not want this module to conflict with blink
        if (player.vehicle != null || ModuleBlink.enabled) {
            return@handler
        }

        if (ticksToSkip-- > 0) {
            it.cancelEvent()
        }
    }

    var duringTickModification = false

    val postTickHandler = handler<PlayerPostTickEvent> {
        // We do not want this module to conflict with blink
        if (player.vehicle != null || ModuleBlink.enabled || duringTickModification) {
            return@handler
        }

        if (simLines.isEmpty()) {
            return@handler
        }

        val nearbyEnemy = world.findEnemy(distanceToWork)
            ?: return@handler

        // todo: find the tick dealing the most amount of damage (by crit)
        // todo: find the tick furthest away from the enemy crosshair
        // todo: add prioritising options
        val (closestTick, _) = simLines.mapIndexed { index, vec3 ->
            index to nearbyEnemy.squaredDistanceTo(vec3.toVec3d())
        }.minByOrNull { (_, distance) -> distance } ?: return@handler

        // Tick as much as we can
        duringTickModification = true
        repeat(closestTick) {
            player.tick()
            tickBalance -= 1
        }
        ticksToSkip = closestTick + pauseAfterTick
        duringTickModification = false
    }

    val inputHandler = handler<MovementInputEvent> { event ->
        // We do not want this module to conflict with blink
        if (player.vehicle != null || ModuleBlink.enabled) {
            return@handler
        }

        simLines.clear()

        val input =
            SimulatedPlayer.SimulatedPlayerInput(
                event.directionalInput,
                player.input.jumping,
                player.isSprinting
            )

        val simulatedPlayer = SimulatedPlayer.fromClientPlayer(input)

        if (tickBalance <= 0) {
            reachedTheLimit = true
        }
        if (tickBalance > balanceMaxValue / 2) {
            reachedTheLimit = false
        }
        if (tickBalance <= balanceMaxValue) {
            tickBalance += balanceRecoveryIncrement
        }

        if (reachedTheLimit) {
            return@handler
        }

        repeat(min(tickBalance.toInt(), maxTicksAtATime)) {
            simulatedPlayer.tick()
            simLines.add(Vec3(simulatedPlayer.pos))
        }
    }

    val renderHandler = handler<WorldRenderEvent> { event ->
        renderEnvironmentForWorld(event.matrixStack) {
            withColor(Color4b.BLUE) {
                drawLineStrip(lines = simLines.toTypedArray())
            }
        }
    }

    val packetHandler = handler<PacketEvent> {
        // Stops when you got flagged
        if (it.packet is PlayerPositionLookS2CPacket && pauseOnFlag) {
            tickBalance = 0f
        }
    }

    override fun disable() {
        tickBalance = 0f
        super.disable()
    }
}
