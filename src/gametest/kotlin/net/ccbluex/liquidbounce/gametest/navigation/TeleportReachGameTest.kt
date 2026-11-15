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
import net.ccbluex.gametest.harness.GameTestScope
import net.ccbluex.gametest.harness.ServerGameTest
import net.ccbluex.gametest.harness.action.aim
import net.ccbluex.gametest.harness.action.awaitServer
import net.ccbluex.gametest.harness.action.fill
import net.ccbluex.gametest.harness.action.setBlock
import net.ccbluex.gametest.harness.action.summon
import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.features.module.ModuleManager
import net.ccbluex.liquidbounce.features.module.modules.exploit.ModuleTeleportReach
import net.ccbluex.liquidbounce.gametest.awaitServerAt
import net.ccbluex.liquidbounce.gametest.fromClient
import net.ccbluex.liquidbounce.gametest.onClient
import net.ccbluex.liquidbounce.gametest.playerPosition
import net.ccbluex.liquidbounce.gametest.restoring
import net.ccbluex.liquidbounce.utils.entity.rotation
import net.ccbluex.liquidbounce.utils.navigation.teleport.TeleportManager
import net.ccbluex.liquidbounce.utils.raytracing.traceFromPlayer
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext
import net.minecraft.client.KeyMapping
import net.minecraft.client.Options
import net.minecraft.client.gui.screens.inventory.ContainerScreen
import net.minecraft.core.BlockPos
import net.minecraft.world.entity.monster.zombie.Husk
import net.minecraft.world.phys.Vec3

private const val OLD_RANGE = 42f
private const val LOOK_RANGE = 60.0
private const val NEAR_SQ = 0.01
private const val WAIT_TICKS = 200
private const val HOLD_OPEN_TICKS = 40

/**
 * A survival player reaches far targets with TeleportReach: middle click teleports, left click hits a husk and
 * breaks a block, right click places a block and opens a chest that stays open, and every action comes back
 * unhurt. Configs saved under the old ClickTp name still load.
 */
class TeleportReachGameTest : ServerGameTest({
    context.restoring(ModuleTeleportReach) {
        checkOldNameLoads(context)

        val y = origin.y
        fill(BlockPos(0, y - 1, -12), BlockPos(30, y - 1, 12), "minecraft:stone")
        fill(BlockPos(0, y, -12), BlockPos(30, y + 10, 12), "minecraft:air")
        awaitServer("block 30 ${y - 1} 12 minecraft:stone")
        run("gamerule minecraft:natural_health_regeneration false")
        // Peaceful has no husks
        run("difficulty easy")
        context.onClient { ModuleTeleportReach.enabled = true }

        unhurt("Reaching") {
            checkTeleport(y)
            checkHit(y)
            checkBreak(y)
            checkPlace(y)
            checkChest(y)
        }
    }
})

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


private fun GameTestScope.checkTeleport(y: Int) {
    val start = origin(y)
    moveTo(start, yaw = -90f)
    aim(BlockPos(14, y - 1, 0))
    input.holdKeyFor({ it.keyPickItem }, 3)
    awaitClient("the teleport", WAIT_TICKS) { client ->
        val position = client.player?.position() ?: return@awaitClient false
        position.x > start.x + 4 && !TeleportManager.isMoving
    }

    val landed = context.playerPosition()
    check(landed.y == y.toDouble()) { "Teleport landed at $landed" }
    awaitServerAt(landed, timeout = WAIT_TICKS)
}

private fun GameTestScope.checkHit(y: Int) {
    moveTo(origin(y), yaw = -90f)
    summon("minecraft:husk", BlockPos(16, y, 0), "{NoAI:1b,PersistenceRequired:1b}")
    awaitClient("the husk") { mc -> checkNotNull(mc.level).entitiesForRendering().any { it is Husk } }

    click(y, BlockPos(16, y + 1, 0), { it.keyAttack }) { awaitHuskHurt(WAIT_TICKS) }
    run("kill @e[type=minecraft:husk]")
}

private fun GameTestScope.checkBreak(y: Int) {
    val target = BlockPos(16, y, 4)
    setBlock(target, "minecraft:dirt")
    moveTo(origin(y), yaw = -90f)
    awaitClient("the dirt") { !checkNotNull(it.level).getBlockState(target).isAir }

    click(y, target, { it.keyAttack }) { awaitServer("block 16 $y 4 minecraft:air", WAIT_TICKS) }
}

private fun GameTestScope.checkPlace(y: Int) {
    val against = BlockPos(16, y, -4)
    setBlock(against, "minecraft:stone")
    run("item replace entity $playerName weapon.mainhand with minecraft:cobblestone 8")
    moveTo(origin(y), yaw = -90f)
    awaitClient("the stone") { !checkNotNull(it.level).getBlockState(against).isAir }
    aim(against)

    val placed = context.fromClient {
        val hit = traceFromPlayer(checkNotNull(it.player).rotation, LOOK_RANGE)
        hit.blockPos.relative(hit.direction)
    }
    click(y, against, { it.keyUse }) {
        awaitServer("block ${placed.x} ${placed.y} ${placed.z} minecraft:cobblestone", WAIT_TICKS)
    }
}

/**
 * The chest stays open from far away while the server holds the player beside it, and closing it brings
 * the player back.
 */
private fun GameTestScope.checkChest(y: Int) {
    val chest = BlockPos(16, y, 8)
    setBlock(chest, "minecraft:chest")
    moveTo(origin(y), yaw = -90f)
    awaitClient("the chest") { !checkNotNull(it.level).getBlockState(chest).isAir }
    aim(chest)
    input.holdKeyFor({ it.keyUse }, 1)

    awaitClient("the chest screen", WAIT_TICKS) { it.gui.screen() is ContainerScreen }
    // A server that closes the chest tells the client so
    ticks(HOLD_OPEN_TICKS)
    check(client { it.gui.screen() is ContainerScreen }) { "The chest closed while held beside it" }
    check(context.playerPosition().near(origin(y), NEAR_SQ)) { "The client left its position to open the chest" }

    client { checkNotNull(it.player).closeContainer() }
    awaitReturn(y)
}

/** Looks at [target], clicks [key] once, waits until [done] and for the player to come back. */
private fun GameTestScope.click(y: Int, target: BlockPos, key: (Options) -> KeyMapping, done: () -> Unit) {
    aim(target)
    input.holdKeyFor(key, 1)
    done()
    awaitReturn(y)
}

private fun GameTestScope.awaitReturn(y: Int) {
    awaitClient("the player back", WAIT_TICKS) {
        !TeleportManager.holds(ModuleTeleportReach) && !TeleportManager.isMoving
    }
    awaitServerAt(origin(y), timeout = WAIT_TICKS)
}

private fun origin(y: Int) = Vec3(2.5, y.toDouble(), 0.5)
