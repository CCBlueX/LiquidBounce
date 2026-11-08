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

import com.google.gson.JsonArray
import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.config.gson.util.GSON_DEFAULT
import net.ccbluex.liquidbounce.features.module.modules.world.ModuleProjectilePuncher
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext
import net.minecraft.world.entity.projectile.Projectile
import net.minecraft.world.entity.projectile.hurtingprojectile.AbstractHurtingProjectile
import net.minecraft.world.phys.Vec3
import kotlin.math.abs

/**
 * [ModuleProjectilePuncher] has to send back wind charges once they are selected, only punch while airborne with
 * OnlyInAir and leave projectiles outside its FOV alone.
 *
 * A projectile counts as punched when the server made the player its owner, which is what a deflection does.
 */
class ProjectilePuncherGameTest : FabricClientGameTest {

    private var groundY = 0.0

    override fun runTest(context: ClientGameTestContext) {
        context.waitForClient()
        val saved = context.fromClient { ConfigSystem.serializeValueGroup(ModuleProjectilePuncher) }

        try {
            context.worldBuilder().create().use { world ->
                val server = world.server
                server.runCommand("gamemode survival @a")
                server.runCommand("effect give @a minecraft:resistance infinite 4 true")
                context.waitFor({ client -> client.player?.onGround() == true }, SETTLE_TICKS)
                groundY = context.fromClient { client -> checkNotNull(client.player).y }
                context.onClient { ModuleProjectilePuncher.enabled = true }

                check(!punches(context, server, WIND_CHARGE)) { "A wind charge was punched without being selected" }
                context.onClient {
                    ModuleProjectilePuncher.settings.getValue("Projectiles").deserializeFrom(
                        GSON_DEFAULT,
                        JsonArray().apply { add("Fireball"); add("ShulkerBullet"); add("WindCharge") }
                    )
                }
                check(punches(context, server, WIND_CHARGE)) { "A selected wind charge was not punched" }

                context.onClient { ModuleProjectilePuncher.settings.getValue("OnlyInAir").setByString("true") }
                check(!punches(context, server, FIREBALL)) { "OnlyInAir punched while standing on the ground" }
                check(punches(context, server, FIREBALL, airborne = true)) { "OnlyInAir did not punch in the air" }
                context.onClient { ModuleProjectilePuncher.settings.getValue("OnlyInAir").setByString("false") }

                context.onClient { ModuleProjectilePuncher.settings.getValue("FOV").setByString("90") }
                check(!punches(context, server, FIREBALL, behind = true)) {
                    "A fireball behind the view was punched with FOV 90"
                }
                check(punches(context, server, FIREBALL)) { "A fireball in view was not punched" }
            }
        } finally {
            context.onClient {
                ModuleProjectilePuncher.enabled = false
                ConfigSystem.deserializeValueGroup(ModuleProjectilePuncher, saved)
            }
        }
    }

    /**
     * Fires [projectile] at the eyes of the player, who stands at the spawn looking south, and waits until it is
     * either punched or gone. It comes from the front or from [behind].
     */
    private fun punches(
        context: ClientGameTestContext,
        server: TestServerContext,
        projectile: String,
        airborne: Boolean = false,
        behind: Boolean = false,
    ): Boolean {
        val y = groundY + if (airborne) AIRBORNE_LIFT else 0.0
        server.runCommand("kill @e[type=!player]")

        if (airborne) {
            // The client decides where a creative player is, and flying only starts off the ground
            server.runCommand("gamemode creative @a")
            context.waitFor({ client -> client.player?.abilities?.mayfly == true }, SETTLE_TICKS)
            context.onClient { client ->
                val player = checkNotNull(client.player)
                player.snapTo(0.5, y, 0.5, 0f, 0f)
                player.deltaMovement = Vec3.ZERO
                player.abilities.flying = true
                player.onUpdateAbilities()
            }
        } else {
            server.runCommand("gamemode survival @a")
            server.runCommand("execute as @a run tp @s 0.5 $y 0.5 0 0")
        }

        context.waitFor({ client ->
            val player = checkNotNull(client.player)
            player.onGround() != airborne && abs(player.y - y) < POSITION_TOLERANCE
        }, SETTLE_TICKS)

        val side = if (behind) -1 else 1
        // A fireball's box starts at its position, a wind charge's is centered on it
        val (height, acceleration) = if (projectile == FIREBALL) {
            FIREBALL_EYE_LEVEL to FIREBALL_ACCELERATION
        } else {
            EYE_LEVEL to 0.0
        }
        server.runCommand(
            "summon $projectile 0.5 ${y + height} ${0.5 + side * DISTANCE} " +
                "{Motion:[0.0,0.0,${-side * SPEED}],acceleration_power:${acceleration}d,ExplosionPower:0b}"
        )

        try {
            repeat(FLIGHT_TICKS) {
                context.waitTick()
                val punched = server.computeOnServer<Boolean?, RuntimeException> { minecraft ->
                    val player = minecraft.playerList.players.single()
                    val flying = minecraft.overworld().allEntities.firstOrNull { it is AbstractHurtingProjectile }
                    (flying as Projectile?)?.let { it.owner == player }
                }

                when (punched) {
                    true -> return true
                    null -> return false
                    false -> {}
                }
            }
        } finally {
            server.runCommand("kill @e[type=!player]")
        }

        error("The $projectile neither hit nor got punched")
    }

    private companion object {
        const val FIREBALL = "minecraft:fireball"
        const val WIND_CHARGE = "minecraft:wind_charge"

        const val AIRBORNE_LIFT = 2.0
        const val EYE_LEVEL = 1.6
        const val FIREBALL_EYE_LEVEL = 1.1
        const val DISTANCE = 7.0
        const val SPEED = 0.25

        // Keeps a fireball, which loses 5 % of its speed every tick, at about SPEED
        const val FIREBALL_ACCELERATION = 0.0125

        const val POSITION_TOLERANCE = 0.1

        const val SETTLE_TICKS = 200
        const val FLIGHT_TICKS = 100
    }

}
