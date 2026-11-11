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
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.event.events.PlayerPostTickEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.utils.entity.FallingPlayer
import net.ccbluex.liquidbounce.utils.entity.SimulatedPlayer
import net.ccbluex.liquidbounce.utils.entity.SimulatedPlayer.SimulatedPlayerInput
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention.READ_FINAL_STATE
import net.ccbluex.liquidbounce.utils.movement.DirectionalInput
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext
import net.minecraft.client.KeyMapping
import net.minecraft.client.Minecraft
import net.minecraft.client.player.LocalPlayer
import net.minecraft.core.BlockPos
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.block.RedStoneOreBlock
import net.minecraft.world.phys.Vec3
import kotlin.jvm.optionals.getOrNull

/**
 * [SimulatedPlayer] and [FallingPlayer] have to predict where vanilla moves the player, tick for tick.
 *
 * Every scenario builds a lane, holds a set of keys and compares the predicted positions with the ones the player
 * went through. A simulation is made both before the player's tick and during it, where the client makes most of its
 * own.
 */
class PlayerSimulationGameTest : LocalGameTest() {

    override fun test(context: ClientGameTestContext) {
        context.worldBuilder().create().use { world ->
            world.server.runCommand("gamemode survival @a")
            world.server.runCommand("attribute @p minecraft:fall_damage_multiplier base set 0")
            context.waitFor({ client -> client.player?.onGround() == true }, SETTLE_TIMEOUT_TICKS)

            val ground = world.server.groundY()
            val failures = scenarios.flatMap { scenario -> run(context, world, scenario, ground) }

            check(failures.isEmpty()) { "simulated movement differs:\n" + failures.joinToString("\n") }
            assertLevelStaysUntouched(context, world, ground)
        }
    }

    /**
     * Redstone ore lights up under whoever walks over it, on the client as well.
     */
    private fun assertLevelStaysUntouched(context: ClientGameTestContext, world: TestSingleplayerContext, ground: Int) {
        val lane = Lane(world, ground)
        lane.fill(LANE_START - 2, 0, LANE_END, LANE_HEIGHT, "air", halfWidth = 3)
        lane.floor(LANE_START, 20)
        lane.floor(21, LANE_END, "redstone_ore")
        world.server.runCommand("tp @p 18.5 ${ground + 1} $LANE_Z.5 -90 0")
        context.waitTicks(10)

        val (reached, lit) = context.fromClient { client ->
            val simulation = SimulatedPlayer.fromClientPlayer(
                SimulatedPlayerInput(DirectionalInput.FORWARDS, jumping = false, sprinting = true, sneaking = false)
            )
            repeat(ORE_WALK_TICKS) { simulation.tick() }

            val level = checkNotNull(client.level)
            simulation.blockX to (21..LANE_END).count { x ->
                level.getBlockState(BlockPos(x, ground, LANE_Z)).getValue(RedStoneOreBlock.LIT)
            }
        }

        check(reached > 21) { "the simulation only walked to x=$reached and never stepped on the ore" }
        check(lit == 0) { "a simulation walking over redstone ore lit $lit blocks of it" }
    }

    private fun run(
        context: ClientGameTestContext,
        world: TestSingleplayerContext,
        scenario: Scenario,
        ground: Int,
    ): List<String> {
        val lane = Lane(world, ground)
        lane.fill(LANE_START - 2, 0, LANE_END, LANE_HEIGHT, "air", halfWidth = 3)
        scenario.build(lane)
        context.waitTicks(scenario.buildTicks)

        val (x, height) = scenario.start
        world.server.runCommand("tp @p $x ${ground + height} $LANE_Z.5 -90 ${scenario.pitch}")
        context.waitTicks(scenario.settleTicks)
        scenario.prepare(context)

        context.onClient { client ->
            scenario.keys.forEach { key -> key.mapping(client).isDown = true }
            Recorder.pending = scenario
        }
        context.waitFor({ Recorder.recording?.isComplete == true }, ARM_TIMEOUT_TICKS + scenario.ticks)

        val recording = checkNotNull(Recorder.recording)
        context.onClient { client ->
            Key.entries.forEach { key -> key.mapping(client).isDown = false }
            Recorder.recording = null
        }
        scenario.cleanUp(world)

        return recording.failures()
    }

    private class Lane(private val world: TestSingleplayerContext, private val ground: Int) {

        /**
         * Fills the lane from [fromX] to [toX], with heights counted from the first free block above the ground.
         */
        fun fill(fromX: Int, fromHeight: Int, toX: Int, toHeight: Int, block: String, halfWidth: Int = 1) {
            world.server.runCommand(
                "fill $fromX ${ground + fromHeight} ${LANE_Z - halfWidth} " +
                    "$toX ${ground + toHeight} ${LANE_Z + halfWidth} $block"
            )
        }

        fun command(command: String) {
            world.server.runCommand(command)
        }

        fun floor(fromX: Int, toX: Int, block: String = "stone") = fill(fromX, 0, toX, 0, block)

        /**
         * A glass basin with its water surface at [depth].
         */
        fun pool(fromX: Int, toX: Int, depth: Int) {
            fill(fromX - 1, 0, toX + 1, depth, "glass", halfWidth = 2)
            fill(fromX, 0, toX, depth, "water")
        }
    }

    private enum class Key(val mapping: (Minecraft) -> KeyMapping) {
        FORWARD({ it.options.keyUp }),
        LEFT({ it.options.keyLeft }),
        JUMP({ it.options.keyJump }),
        SNEAK({ it.options.keyShift }),
        SPRINT({ it.options.keySprint }),
    }

    /**
     * @param start x and height above the ground the player is teleported to, looking down the lane
     * @param armWhen the tick the comparison starts at, with [keys] held since [prepare]
     * @param landing also checks that [FallingPlayer] finds the tick and block the player lands on
     */
    private class Scenario(
        val name: String,
        val keys: Set<Key>,
        val ticks: Int,
        val start: Pair<Double, Int> = 18.5 to 1,
        val pitch: Float = 0.0f,
        val buildTicks: Int = 2,
        val settleTicks: Int = 10,
        val landing: Boolean = false,
        val armWhen: (LocalPlayer) -> Boolean = { true },
        val prepare: (ClientGameTestContext) -> Unit = {},
        val cleanUp: (TestSingleplayerContext) -> Unit = {},
        val build: (Lane) -> Unit,
    ) {
        fun input() = SimulatedPlayerInput(
            DirectionalInput(forwards = Key.FORWARD in keys, backwards = false, left = Key.LEFT in keys, right = false),
            jumping = Key.JUMP in keys,
            sprinting = Key.SPRINT in keys,
            sneaking = Key.SNEAK in keys,
        )
    }

    private data class Step(val pos: Vec3, val onGround: Boolean) {
        constructor(player: Player) : this(player.position(), player.onGround())
    }

    private class Recording(val scenario: Scenario) {
        var beforeTick: List<Step>? = null
        var duringTick: List<Step>? = null
        var landing: FallingPlayer.CollisionResult? = null

        val steps = ArrayList<Step>()
        val supports = ArrayList<BlockPos?>()

        val isComplete get() = steps.size >= scenario.ticks

        fun predict(): List<Step> {
            val simulation = SimulatedPlayer.fromClientPlayer(scenario.input())
            return List(scenario.ticks) {
                simulation.tick()
                Step(simulation)
            }
        }

        fun failures(): List<String> = listOfNotNull(
            difference("before the tick", checkNotNull(beforeTick)),
            difference("during the tick", checkNotNull(duringTick)),
            landingDifference().takeIf { scenario.landing },
        ).map { "${scenario.name}, $it" }

        private fun difference(made: String, predicted: List<Step>): String? {
            val tick = steps.indices.firstOrNull { tick ->
                val real = steps[tick]
                val simulated = predicted[tick]
                real.onGround != simulated.onGround || real.pos.distanceTo(simulated.pos) > TOLERANCE
            } ?: return null

            return "simulation made $made: tick $tick is ${predicted[tick]}, the player was ${steps[tick]}"
        }

        private fun landingDifference(): String? {
            val tick = steps.indexOfFirst { it.onGround }
            val found = landing

            return if (tick == -1 || found == null || found.tick != tick || found.pos != supports[tick]) {
                "landing: found tick ${found?.tick} on ${found?.pos}, " +
                    "the player landed in tick $tick on ${supports.getOrNull(tick)}"
            } else {
                null
            }
        }
    }

    private object Recorder : EventListener {
        @Volatile
        var pending: Scenario? = null

        @Volatile
        var recording: Recording? = null

        @Suppress("unused")
        private val tickHandler = handler<GameTickEvent> {
            val scenario = pending ?: return@handler
            val player = Minecraft.getInstance().player ?: return@handler
            if (!scenario.armWhen(player)) {
                return@handler
            }

            pending = null
            recording = Recording(scenario).apply { beforeTick = predict() }
        }

        @Suppress("unused")
        private val inputHandler = handler<MovementInputEvent>(priority = READ_FINAL_STATE) {
            val recording = recording?.takeIf { it.duringTick == null } ?: return@handler
            val player = Minecraft.getInstance().player ?: return@handler

            recording.duringTick = recording.predict()
            if (recording.scenario.landing) {
                recording.landing = FallingPlayer.fromPlayer(player).findCollision(recording.scenario.ticks)
            }
        }

        @Suppress("unused")
        private val postTickHandler = handler<PlayerPostTickEvent> {
            val recording = recording?.takeIf { it.duringTick != null && !it.isComplete } ?: return@handler
            val player = Minecraft.getInstance().player ?: return@handler

            recording.steps += Step(player)
            recording.supports += player.mainSupportingBlockPos.getOrNull()
        }
    }

    private companion object {
        const val LANE_Z = 20
        const val LANE_START = 16
        const val LANE_END = 60
        const val LANE_HEIGHT = 12

        const val TOLERANCE = 1.0E-9

        const val SETTLE_TIMEOUT_TICKS = 200
        const val ARM_TIMEOUT_TICKS = 200
        const val ORE_WALK_TICKS = 40

        val walk = setOf(Key.FORWARD)
        val sprint = setOf(Key.FORWARD, Key.SPRINT)
        val sprintJump = setOf(Key.FORWARD, Key.SPRINT, Key.JUMP)
        val effects = listOf("speed", "jump_boost", "slow_falling")

        val scenarios = listOf(
            Scenario("sprint jumping from a standstill", sprintJump, ticks = 50) { lane ->
                lane.floor(LANE_START, LANE_END)
            },
            Scenario("strafing while walking", setOf(Key.FORWARD, Key.LEFT), ticks = 20) { lane ->
                lane.fill(LANE_START, 0, LANE_END, 0, "stone", halfWidth = 3)
            },
            Scenario("sneaking up to a ledge", setOf(Key.FORWARD, Key.SNEAK), ticks = 60, start = 18.5 to 3) { lane ->
                lane.fill(LANE_START, 0, 20, 2, "stone")
            },
            Scenario("walking off a ledge", walk, ticks = 50, start = 18.5 to 8) { lane ->
                lane.fill(LANE_START, 0, 20, 7, "stone")
            },
            Scenario("stepping up slabs and running into a wall", sprint, ticks = 100) { lane ->
                lane.floor(LANE_START, LANE_END)
                lane.fill(24, 1, 26, 1, "stone_slab")
                lane.fill(27, 1, 29, 1, "stone")
                lane.fill(36, 1, 36, 3, "stone")
            },
            Scenario("jumping into a pool and swimming", setOf(Key.FORWARD, Key.JUMP), ticks = 80, start = 18.5 to 4) {
                    lane ->
                lane.fill(LANE_START, 0, 21, 3, "stone", halfWidth = 2)
                lane.pool(22, 30, depth = 3)
            },
            Scenario(
                "sprint swimming under water",
                sprint,
                ticks = 40,
                start = 23.5 to 3,
                pitch = 5.0f,
            ) { lane ->
                lane.pool(22, 32, depth = 5)
            },
            Scenario("drifting in flowing water", emptySet(), ticks = 40, start = 19.5 to 1, buildTicks = 100) { lane ->
                lane.floor(LANE_START, LANE_END)
                lane.fill(LANE_START, 1, 40, 2, "glass")
                lane.fill(17, 1, 39, 2, "air", halfWidth = 0)
                lane.fill(17, 1, 17, 1, "water", halfWidth = 0)
            },
            Scenario("climbing a ladder and dropping behind it", walk, ticks = 90, start = 20.5 to 1) { lane ->
                lane.floor(LANE_START, LANE_END)
                lane.fill(24, 1, 24, 8, "stone")
                lane.fill(23, 1, 23, 8, "ladder[facing=west]", halfWidth = 0)
            },
            Scenario("sprint jumping over soul sand, ice, slime, honey and into cobwebs", sprintJump, ticks = 160) {
                    lane ->
                lane.floor(LANE_START, LANE_END)
                lane.floor(21, 24, "soul_sand")
                lane.floor(25, 28, "packed_ice")
                lane.floor(29, 32, "slime_block")
                lane.floor(33, 36, "honey_block")
                lane.fill(39, 1, 39, 2, "cobweb")
            },
            Scenario(
                "bouncing on slime",
                emptySet(),
                ticks = 80,
                start = 18.5 to 10,
                settleTicks = 3,
                landing = true,
                armWhen = { player -> player.fallDistance > 0.3 },
            ) { lane ->
                lane.floor(LANE_START, 22, "slime_block")
            },
            Scenario(
                "falling onto the edge of a slab",
                walk,
                ticks = 40,
                start = 18.5 to 8,
                landing = true,
                armWhen = { player -> player.fallDistance > 0.3 },
            ) { lane ->
                lane.fill(LANE_START, 0, 20, 7, "stone")
                lane.floor(21, 22)
                lane.fill(23, 1, 24, 1, "stone_slab")
            },
            Scenario(
                "sprint jumping with speed, jump boost and slow falling",
                sprintJump,
                ticks = 80,
                prepare = { context ->
                    context.waitFor({ client ->
                        checkNotNull(client.player).activeEffects.size == effects.size
                    }, SETTLE_TIMEOUT_TICKS)
                },
                cleanUp = { world -> world.server.runCommand("effect clear @p") },
            ) { lane ->
                lane.floor(LANE_START, LANE_END)
                effects.forEach { effect -> lane.command("effect give @p minecraft:$effect 600 1 true") }
            },
            Scenario(
                "gliding with an elytra",
                walk,
                ticks = 60,
                start = 18.5 to 70,
                pitch = 15.0f,
                settleTicks = 3,
                armWhen = { player -> player.isFallFlying },
                prepare = { context ->
                    context.waitFor({ client ->
                        checkNotNull(client.player).deltaMovement.y < -0.5
                    }, SETTLE_TIMEOUT_TICKS)
                    context.input.holdKey { options -> options.keyJump }
                    context.waitTicks(2)
                    context.input.releaseKey { options -> options.keyJump }
                },
                cleanUp = { world -> world.server.runCommand("item replace entity @p armor.chest with minecraft:air") },
            ) { lane ->
                lane.command("item replace entity @p armor.chest with minecraft:elytra")
            },
        )
    }

}
