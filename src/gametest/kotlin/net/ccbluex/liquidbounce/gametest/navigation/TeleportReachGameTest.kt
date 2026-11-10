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

package net.ccbluex.liquidbounce.gametest.navigation

import com.google.gson.JsonPrimitive
import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.features.module.ModuleManager
import net.ccbluex.liquidbounce.features.module.modules.exploit.ModuleTeleportReach
import net.ccbluex.liquidbounce.gametest.fromClient
import net.ccbluex.liquidbounce.gametest.groundY
import net.ccbluex.liquidbounce.gametest.onClient
import net.ccbluex.liquidbounce.gametest.player
import net.ccbluex.liquidbounce.gametest.playerPosition
import net.ccbluex.liquidbounce.gametest.waitForClient
import net.ccbluex.liquidbounce.utils.entity.rotation
import net.ccbluex.liquidbounce.utils.navigation.teleport.TeleportDispatcher
import net.ccbluex.liquidbounce.utils.raytracing.traceFromPlayer
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext
import net.minecraft.client.KeyMapping
import net.minecraft.client.Options
import net.minecraft.client.gui.screens.inventory.ContainerScreen
import net.minecraft.core.BlockPos
import net.minecraft.world.inventory.ChestMenu
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.Vec3

/**
 * A survival player reaches far targets with TeleportReach: middle click teleports, left click hits a husk and
 * breaks a block, right click places a block and opens a chest that stays open, and every action comes back
 * unhurt. Configs saved under the old ClickTp name still load.
 */
@Suppress("TooManyFunctions")
class TeleportReachGameTest : FabricClientGameTest {

    override fun runTest(context: ClientGameTestContext) {
        if (!navigationGameTests) {
            return
        }
        context.waitForClient()
        context.restoring(ModuleTeleportReach) {
            checkOldNameLoads(context)

            context.worldBuilder().create().use { world ->
                val server = world.server
                val y = server.groundY()
                server.runCommand("fill 0 ${y - 1} -12 30 ${y - 1} 12 stone")
                server.runCommand("fill 0 $y -12 30 ${y + 10} 12 air")
                server.runCommand("gamemode survival @a")
                server.runCommand("gamerule natural_health_regeneration false")
                server.runCommand("clear @a")
                context.onClient { ModuleTeleportReach.enabled = true }

                checkTeleport(context, server, y)
                checkHit(context, server, y)
                checkBreak(context, server, y)
                checkPlace(context, server, y)
                checkChest(context, server, y)

                check(server.player { it.health } == server.player { it.maxHealth }) { "Reaching hurt the player" }
            }
        }
    }

    private fun checkOldNameLoads(context: ClientGameTestContext) = context.onClient {
        val json = ConfigSystem.serializeValueGroup(ModuleManager.modulesConfig)
        val entry = json.getAsJsonArray("value").first {
            it.asJsonObject["name"].asString == ModuleTeleportReach.name
        }.asJsonObject
        entry.addProperty("name", "ClickTp")
        entry.getAsJsonArray("value").first { it.asJsonObject["name"].asString == "Range" }
            .asJsonObject.add("value", JsonPrimitive(OLD_RANGE))

        ConfigSystem.deserializeValueGroup(ModuleManager.modulesConfig, json)

        val range = ModuleTeleportReach.containedValues.first { it.name == "Range" }.get()
        check(range == OLD_RANGE) { "A ClickTp config set the range to $range instead of $OLD_RANGE" }
    }

    private fun checkTeleport(context: ClientGameTestContext, server: TestServerContext, y: Int) {
        val start = origin(y)
        moveTo(context, server, start)
        context.input.lookAt(BlockPos(14, y - 1, 0))
        context.waitTicks(2)
        context.input.holdKeyFor({ it.keyPickItem }, 3)
        context.waitFor({ client ->
            val position = client.player?.position() ?: return@waitFor false
            position.x > start.x + 4 && !TeleportDispatcher.isMoving
        }, WAIT_TICKS)

        val landed = context.playerPosition()
        check(landed.y == y.toDouble()) { "Teleport landed at $landed" }
        server.waitFor({ it.player().position().near(landed) }, WAIT_TICKS)
    }

    private fun checkHit(context: ClientGameTestContext, server: TestServerContext, y: Int) {
        moveTo(context, server, origin(y))
        server.runCommand("summon husk 16.5 $y 0.5 {NoAI:1b,PersistenceRequired:1b}")
        context.waitTicks(SETTLE_TICKS)

        click(context, server, y, BlockPos(16, y + 1, 0), { it.keyAttack }) {
            server.huskHurt()
        }
        server.runCommand("kill @e[type=husk]")
    }

    private fun checkBreak(context: ClientGameTestContext, server: TestServerContext, y: Int) {
        val target = BlockPos(16, y, 4)
        server.runCommand("setblock ${target.x} ${target.y} ${target.z} dirt")
        moveTo(context, server, origin(y))

        click(context, server, y, target, { it.keyAttack }) { server.blockAt(target).isAir }
    }

    private fun checkPlace(context: ClientGameTestContext, server: TestServerContext, y: Int) {
        val against = BlockPos(16, y, -4)
        server.runCommand("setblock ${against.x} ${against.y} ${against.z} stone")
        server.runCommand("item replace entity @a weapon.mainhand with cobblestone 8")
        moveTo(context, server, origin(y))
        context.input.lookAt(against)
        context.waitTicks(2)

        val placed = context.fromClient {
            val hit = traceFromPlayer(checkNotNull(it.player).rotation, LOOK_RANGE)
            hit.blockPos.relative(hit.direction)
        }
        click(context, server, y, against, { it.keyUse }) { server.blockAt(placed).`is`(Blocks.COBBLESTONE) }
    }

    /**
     * The chest stays open from far away while the server holds the player beside it, and closing it brings
     * the player back.
     */
    private fun checkChest(context: ClientGameTestContext, server: TestServerContext, y: Int) {
        val chest = BlockPos(16, y, 8)
        server.runCommand("setblock ${chest.x} ${chest.y} ${chest.z} chest")
        moveTo(context, server, origin(y))
        context.input.lookAt(chest)
        context.waitTicks(2)
        context.input.holdKeyFor({ it.keyUse }, 1)

        context.waitFor({ client -> client.gui.screen() is ContainerScreen }, WAIT_TICKS)
        context.waitTicks(HOLD_OPEN_TICKS)
        check(context.fromClient { it.gui.screen() is ContainerScreen }) { "The chest closed while held beside it" }
        check(server.player { it.containerMenu is ChestMenu }) { "The server closed the chest" }
        check(context.playerPosition().near(origin(y))) {
            "The client left its position to open the chest"
        }

        context.onClient { checkNotNull(it.player).closeContainer() }
        awaitReturn(context, server, y)
    }

    /** Looks at [target], clicks [key] once, waits for [done] and for the player to come back. */
    private fun click(
        context: ClientGameTestContext,
        server: TestServerContext,
        y: Int,
        target: BlockPos,
        key: (Options) -> KeyMapping,
        done: () -> Boolean,
    ) {
        context.input.lookAt(target)
        context.waitTicks(2)
        context.input.holdKeyFor(key, 1)

        repeat(WAIT_TICKS) {
            if (done()) {
                awaitReturn(context, server, y)
                return
            }
            context.waitTick()
        }

        val held = server.player { it.position() }
        error("Clicking $target did nothing within $WAIT_TICKS ticks, the server has the player at $held")
    }

    private fun awaitReturn(context: ClientGameTestContext, server: TestServerContext, y: Int) {
        context.waitFor({ !TeleportDispatcher.holds(ModuleTeleportReach) && !TeleportDispatcher.isMoving }, WAIT_TICKS)
        server.waitFor({ it.player().position().near(origin(y)) }, WAIT_TICKS)
    }

    private fun origin(y: Int) = Vec3(2.5, y.toDouble(), 0.5)

    private fun moveTo(context: ClientGameTestContext, server: TestServerContext, position: Vec3) {
        moveTo(context, server, position, yaw = -90f, toleranceSqr = NEAR_SQ)
    }

    private fun TestServerContext.blockAt(pos: BlockPos) =
        computeOnServer<BlockState, RuntimeException> {
            it.overworld().getBlockState(pos)
        }

    private fun Vec3.near(other: Vec3): Boolean = near(other, NEAR_SQ)

    private companion object {
        const val OLD_RANGE = 42f
        const val LOOK_RANGE = 60.0
        const val NEAR_SQ = 0.01
        const val WAIT_TICKS = 200
        const val SETTLE_TICKS = 5
        const val HOLD_OPEN_TICKS = 40
    }

}
