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
package net.ccbluex.liquidbounce.gametest

import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.event.events.PlayerPostTickEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.client.ClientState
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.modules.movement.ModuleParkour
import net.ccbluex.liquidbounce.features.module.modules.movement.ModuleSprint
import net.ccbluex.liquidbounce.features.module.modules.movement.speed.ModuleSpeed
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold
import net.ccbluex.liquidbounce.utils.client.currentClient
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.client.network
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.entity.SimulatedPlayer
import net.ccbluex.liquidbounce.utils.entity.SimulatedPlayer.SimulatedPlayerInput
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention.READ_FINAL_STATE
import net.ccbluex.liquidbounce.utils.movement.DirectionalInput
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket
import net.minecraft.world.phys.Vec3

/**
 * A simulation of the local player runs in a simulated client, a fork of the real one: modules take part in it like
 * in a real tick, while what they change and send stays in the fork.
 *
 * Parkour jumps at the very edge of a block, which only a simulation that lets it take part can foresee. The player
 * runs off a raised platform with it; a prediction made a few blocks before the edge has to follow the jump tick for
 * tick, where one without modules runs off the edge instead.
 */
class SimulatedClientGameTest : FabricClientGameTest {

    override fun runTest(context: ClientGameTestContext) {
        context.waitForClient()

        context.worldBuilder().create().use { world ->
            world.server.runCommand("gamemode survival @a")
            world.server.runCommand("attribute @p minecraft:fall_damage_multiplier base set 0")
            context.waitFor({ client -> client.player?.onGround() == true }, SETTLE_TIMEOUT_TICKS)
            val ground = world.server.groundY()

            predictsParkourJump(context, world, ground)
            keepsPacketsInside(context, world, ground)
            measure(context, world, ground)
        }
    }

    private fun predictsParkourJump(context: ClientGameTestContext, world: TestSingleplayerContext, ground: Int) {
        world.server.runCommand("fill 10 ${ground + 1} ${LANE_Z - 3} 60 ${ground + 12} ${LANE_Z + 3} air")
        world.server.runCommand(
            "fill $LANE_START ${ground + 1} ${LANE_Z - 1} $EDGE_X ${ground + 7} ${LANE_Z + 1} stone"
        )
        world.server.runCommand("tp @p $START_X ${ground + 8} $LANE_Z.5 -90 0")
        context.waitTicks(SETTLE_TICKS)

        context.onClient { client ->
            ModuleParkour.enabled = true
            client.options.keyUp.isDown = true
            client.options.keySprint.isDown = true
            Recorder.recording = Recording()
        }
        context.waitFor({ Recorder.recording?.isComplete == true }, RUN_TIMEOUT_TICKS)

        val recording = checkNotNull(Recorder.recording)
        context.onClient { client ->
            client.options.keyUp.isDown = false
            client.options.keySprint.isDown = false
            ModuleParkour.enabled = false
            Recorder.recording = null
        }

        val jumped = recording.steps.any { it.y > ground + 8 + JUMP_RISE }
        check(jumped) { "the player never jumped at the edge, Parkour did not act: ${recording.steps}" }

        val withModules = checkNotNull(recording.withModules)
        val withoutModules = checkNotNull(recording.withoutModules)
        val differs = recording.steps.indices.firstOrNull { tick ->
            recording.steps[tick].distanceTo(withModules[tick]) > TOLERANCE
        }
        check(differs == null) {
            "the simulation with modules differs in tick $differs: ${withModules[differs!!]}, " +
                "the player was at ${recording.steps[differs]}"
        }
        check(recording.steps.indices.any { tick -> recording.steps[tick].distanceTo(withoutModules[tick]) > MISS }) {
            "a simulation without modules foresaw the Parkour jump as well"
        }
        check(recording.aimYawAfter == SENTINEL_YAW) {
            "a simulation changed the Parkour state of the real client: aim yaw ${recording.aimYawAfter}"
        }
        check(recording.forkedAimYaws.any { it.isNaN() }) {
            "Parkour never ran in the simulated client: ${recording.forkedAimYaws}"
        }
    }

    /**
     * A module sending from a simulated client must not reach the server. The same packet from the real client does.
     */
    private fun keepsPacketsInside(context: ClientGameTestContext, world: TestSingleplayerContext, ground: Int) {
        world.server.runCommand("fill 10 ${ground + 1} ${LANE_Z - 3} 60 ${ground + 12} ${LANE_Z + 3} air")
        world.server.runCommand("fill $LANE_START $ground ${LANE_Z - 1} 40 $ground ${LANE_Z + 1} stone")
        world.server.runCommand("tp @p $START_X ${ground + 1} $LANE_Z.5 -90 0")
        context.waitTicks(SETTLE_TICKS)
        val slot = world.server.player { it.inventory.selectedSlot }

        val sentInFork = context.fromClient {
            Sender.slot = SENT_SLOT
            val simulation = SimulatedPlayer.fromClientPlayer(walkInput())
            repeat(SEND_TICKS) { simulation.tick() }
            Sender.slot = null
            Sender.sent.also { Sender.sent = 0 }
        }
        context.waitTicks(SETTLE_TICKS)

        check(sentInFork > 0) { "the simulation never asked the module to send" }
        val slotAfter = world.server.player { it.inventory.selectedSlot }
        check(slotAfter == slot) { "a packet sent $sentInFork times from a simulated client reached the server" }

        context.onClient { network.send(ServerboundSetCarriedItemPacket(SENT_SLOT)) }
        context.waitTicks(SETTLE_TICKS)
        val slotFromRealClient = world.server.player { it.inventory.selectedSlot }
        context.onClient { network.send(ServerboundSetCarriedItemPacket(slot)) }
        check(slotFromRealClient == SENT_SLOT) { "the same packet from the real client did not arrive either" }
    }

    /**
     * Logs what a simulation of [MEASURED_TICKS] ticks of the local player costs with common movement modules on.
     */
    private fun measure(context: ClientGameTestContext, world: TestSingleplayerContext, ground: Int) {
        world.server.runCommand("tp @p $START_X ${ground + 1} $LANE_Z.5 -90 0")
        context.waitTicks(SETTLE_TICKS)

        val modules = listOf(ModuleSprint, ModuleSpeed, ModuleScaffold, ModuleParkour)
        val (withModules, withoutModules) = context.fromClient {
            val states = modules.map(ClientModule::enabled)
            modules.forEach { it.enabled = true }
            try {
                micros(withModules = true) to micros(withModules = false)
            } finally {
                modules.zip(states).forEach { (module, enabled) -> module.enabled = enabled }
            }
        }

        logger.info(
            "A simulation of $MEASURED_TICKS ticks with Sprint, Speed, Scaffold and Parkour takes " +
                "%.1f µs with modules taking part, %.1f µs without".format(withModules, withoutModules)
        )
    }

    private fun micros(withModules: Boolean): Double {
        repeat(MEASURE_WARMUP) { simulate(withModules) }

        val start = System.nanoTime()
        repeat(MEASURE_RUNS) { simulate(withModules) }
        return (System.nanoTime() - start) / MEASURE_RUNS / NANOS_PER_MICRO
    }

    private fun simulate(withModules: Boolean) {
        val simulation = SimulatedPlayer.fromClientPlayer(walkInput(), withModules)
        repeat(MEASURED_TICKS) { simulation.tick() }
    }

    private class Recording {
        val steps = ArrayList<Vec3>()
        var withModules: List<Vec3>? = null
        var withoutModules: List<Vec3>? = null
        var aimYawAfter = Float.NaN
        val forkedAimYaws = ArrayList<Float>()

        val isComplete get() = steps.size >= PREDICTED_TICKS
        val isArmed get() = withModules != null
    }

    private object Recorder : EventListener {
        @Volatile
        var recording: Recording? = null

        @Suppress("UNCHECKED_CAST")
        private val aimYaw = ModuleParkour::class.java.getDeclaredField("aimYaw\$delegate")
            .apply { isAccessible = true }
            .get(ModuleParkour) as ClientState<Float>

        @Suppress("unused")
        private val inputHandler = handler<MovementInputEvent>(priority = READ_FINAL_STATE) { event ->
            val recording = recording ?: return@handler
            if (currentClient.isSimulated) {
                recording.forkedAimYaws += aimYaw.get()
                return@handler
            }
            if (recording.isArmed || player.x < ARM_X) {
                return@handler
            }

            // Parkour clears the aim in every tick it has no jump to assist, the simulated one too
            aimYaw.set(SENTINEL_YAW)
            val input = SimulatedPlayerInput(event.directionalInput, event.jump, sprinting = true, event.sneak)
            recording.withModules = predict(input, withModules = true)
            recording.withoutModules = predict(input, withModules = false)
            recording.aimYawAfter = aimYaw.get()
            aimYaw.set(Float.NaN)
        }

        @Suppress("unused")
        private val postTickHandler = handler<PlayerPostTickEvent> {
            val recording = recording?.takeIf { it.isArmed && !it.isComplete } ?: return@handler
            recording.steps += player.position()
        }

        private fun predict(input: SimulatedPlayerInput, withModules: Boolean): List<Vec3> {
            val simulation = SimulatedPlayer.fromClientPlayer(input, withModules)
            return List(PREDICTED_TICKS) {
                simulation.tick()
                simulation.position()
            }
        }
    }

    /**
     * Sends a hotbar slot whenever a simulated client asks for the keys of a tick.
     */
    private object Sender : EventListener {
        @Volatile
        var slot: Int? = null

        @Volatile
        var sent = 0

        @Suppress("unused")
        private val inputHandler = handler<MovementInputEvent> {
            val slot = slot ?: return@handler
            if (currentClient.isSimulated) {
                network.send(ServerboundSetCarriedItemPacket(slot))
                sent++
            }
        }
    }

    private companion object {
        const val LANE_Z = 20
        const val LANE_START = 16
        const val EDGE_X = 26
        const val START_X = 18.5
        const val ARM_X = 21.0

        const val JUMP_RISE = 0.5
        const val SENTINEL_YAW = 123f

        const val PREDICTED_TICKS = 30
        const val TOLERANCE = 1.0E-6
        const val MISS = 0.1

        const val SEND_TICKS = 5
        const val SENT_SLOT = 5

        const val MEASURED_TICKS = 20
        const val MEASURE_WARMUP = 200
        const val MEASURE_RUNS = 1000
        const val NANOS_PER_MICRO = 1000.0

        const val SETTLE_TICKS = 10
        const val SETTLE_TIMEOUT_TICKS = 200
        const val RUN_TIMEOUT_TICKS = 400

        fun walkInput() =
            SimulatedPlayerInput(DirectionalInput.FORWARDS, jumping = false, sprinting = true, sneaking = false)
    }

}
