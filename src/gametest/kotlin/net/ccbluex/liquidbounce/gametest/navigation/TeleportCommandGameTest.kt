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

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.features.command.CommandManager
import net.ccbluex.liquidbounce.features.global.GlobalSettingsTeleport
import net.ccbluex.liquidbounce.features.module.ModuleManager
import net.ccbluex.liquidbounce.features.module.modules.combat.tpaura.ModuleTpAura
import net.ccbluex.liquidbounce.features.module.modules.exploit.ModuleTeleportReach
import net.ccbluex.liquidbounce.gametest.command
import net.ccbluex.liquidbounce.gametest.fromClient
import net.ccbluex.liquidbounce.gametest.onClient
import net.ccbluex.liquidbounce.gametest.suggest
import net.ccbluex.liquidbounce.gametest.waitForClient
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext
import net.minecraft.world.level.levelgen.Heightmap
import net.minecraft.world.phys.Vec3
import kotlin.math.abs

/** The teleport commands work without the retired Teleport module, and old configs still load. */
class TeleportCommandGameTest : FabricClientGameTest {

    override fun runTest(context: ClientGameTestContext) {
        if (!navigationGameTests) {
            return
        }
        context.waitForClient()
        check(context.fromClient { ModuleManager["Teleport"] == null }) { "The Teleport module is still registered" }

        val savedTeleport = context.fromClient { ConfigSystem.serializeValueGroup(GlobalSettingsTeleport) }
        val savedTpAura = context.fromClient { ConfigSystem.serializeValueGroup(ModuleTpAura) }

        try {
            assertStaleConfigLoads(context)

            context.worldBuilder().create().use { world ->
                val groundY = world.server.computeOnServer<Int, RuntimeException> { server ->
                    server.overworld().getHeight(Heightmap.Types.MOTION_BLOCKING, 8, 8)
                }.toDouble()
                world.server.runCommand("gamemode creative @a")
                world.server.runCommand("tp @a 8.5 $groundY 8.5")
                context.waitFor({ client -> client.player?.position()?.near(Vec3(8.5, groundY, 8.5)) == true }, 200)

                val target = Vec3(10.5, groundY, 12.5)
                context.command("teleport ${target.x} ${target.y} ${target.z}")
                context.waitFor({ client -> client.player?.position()?.near(target) == true }, 20)
                context.waitTicks(10)
                check(context.fromClient { checkNotNull(it.player).position().near(target) }) {
                    "The client was set back after teleporting"
                }
                val serverPosition = world.server.computeOnServer<Vec3, RuntimeException> {
                    it.playerList.players.first().position()
                }
                check(serverPosition.near(target)) {
                    "The server did not accept the teleport"
                }

                context.command("tp ~3 ~ ~")
                val relative = target.add(3.0, 0.0, 0.0)
                context.waitFor({ client -> client.player?.position()?.near(relative) == true }, 20)

                val highY = groundY + 5.0
                val highTpY = context.fromClient { mc ->
                    val amount = GlobalSettingsTeleport.containedValues.first { it.name == "HighTPAmount" }
                    amount.setByString(highY.toString())
                    CommandManager.execute("teleport 9.5 ~ 9.5 true")
                    checkNotNull(mc.player).y
                }
                check(abs(highTpY - highY) < EPSILON) { "HighTP moved to $highTpY instead of $highY" }

                val (before, after) = context.fromClient { mc ->
                    val player = checkNotNull(mc.player)
                    val before = player.y
                    CommandManager.execute("vclip by 2")
                    before to player.y
                }
                check(abs(after - before - 2.0) < EPSILON) { "vclip moved from $before to $after" }

                checkPlayerTeleport(context, groundY)
            }
        } finally {
            context.onClient {
                ConfigSystem.deserializeValueGroup(GlobalSettingsTeleport, savedTeleport)
                ConfigSystem.deserializeValueGroup(ModuleTpAura, savedTpAura)
            }
        }
    }

    private fun assertStaleConfigLoads(context: ClientGameTestContext) = context.onClient {
        val tpAuraBefore = ConfigSystem.serializeValueGroup(ModuleTpAura).toString()
        val clickTpBefore = ConfigSystem.serializeValueGroup(ModuleTeleportReach).toString()
        val enabledBefore = ModuleManager.associate { it.name to it.enabled }

        val json = ConfigSystem.serializeValueGroup(ModuleManager.modulesConfig)
        val modules = json.getAsJsonArray("value")
        modules.valuesOf(ModuleTpAura.name).apply {
            add(JsonObject().apply {
                addProperty("name", "Mode")
                addProperty("active", "AStar")
                add("choices", JsonObject())
                add("value", JsonArray())
            })
            add(namedValue("AttackRange", JsonPrimitive(4.2)))
        }
        modules.valuesOf(ModuleTeleportReach.name).add(namedValue("LagAfter", JsonPrimitive(40)))
        modules.add(JsonObject().apply {
            addProperty("name", "Teleport")
            add("value", JsonArray().apply { add(namedValue("Enabled", JsonPrimitive(true))) })
        })

        ConfigSystem.deserializeValueGroup(ModuleManager.modulesConfig, json)

        check(ModuleManager["Teleport"] == null) { "Loading a stale config registered a Teleport module" }
        check(ConfigSystem.serializeValueGroup(ModuleTpAura).toString() == tpAuraBefore) {
            "Stale TpAura settings changed the current ones"
        }
        check(ConfigSystem.serializeValueGroup(ModuleTeleportReach).toString() == clickTpBefore) {
            "Stale ClickTp settings changed the current ones"
        }
        check(ModuleManager.associate { it.name to it.enabled } == enabledBefore) {
            "Loading a stale config changed other modules"
        }
    }

    /**
     * Players in the world are found by name even when the tab list does not show them, such as a fake player.
     */
    private fun checkPlayerTeleport(context: ClientGameTestContext, groundY: Double) {
        val twin = Vec3(14.5, groundY, 6.5)
        context.command("teleport ${twin.x} ${twin.y} ${twin.z}")
        context.command("fakeplayer spawn Twin")
        context.command("teleport 8.5 $groundY 8.5")
        try {
            check(context.suggest(".playerteleport Tw").contains("Twin")) { "The fake player was not suggested" }
            context.command("playerteleport Twin")
            context.waitFor({ client -> client.player?.position()?.near(twin) == true }, 20)
        } finally {
            context.command("fakeplayer clear")
        }
    }

    private fun JsonArray.valuesOf(module: String) =
        first { it.asJsonObject["name"].asString == module }.asJsonObject.getAsJsonArray("value")

    private fun namedValue(name: String, value: JsonPrimitive) = JsonObject().apply {
        addProperty("name", name)
        add("value", value)
    }

    private fun Vec3.near(other: Vec3) = distanceToSqr(other) < EPSILON * EPSILON

    private companion object {
        const val EPSILON = 1.0E-3
    }

}
