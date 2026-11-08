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
import com.mojang.authlib.GameProfile
import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.config.gson.util.GSON_DEFAULT
import net.ccbluex.liquidbounce.config.types.group.ValueGroup
import net.ccbluex.liquidbounce.features.global.GlobalSettingsTarget
import net.ccbluex.liquidbounce.features.module.modules.world.traps.ModuleAutoTrap
import net.ccbluex.liquidbounce.utils.world.nextLocalEntityId
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.player.RemotePlayer
import net.minecraft.core.BlockPos
import net.minecraft.world.entity.Entity
import net.minecraft.world.item.Items
import net.minecraft.world.level.block.Blocks
import java.util.UUID

/**
 * The AutoWeb part of [ModuleAutoTrap] against a player thrown straight up 2.5 blocks in front of the player:
 * OnlyFalling webs the landing spot while the target is still in the air and leaves it alone on the ground,
 * LeadTicks keeps it from starting too late, HeadHeight adds a web above the first one, without Rotate it only places
 * where the player looks, and Refill pulls webs into the hotbar.
 *
 * The target only exists on the client and its fall is scripted, so the server sees nothing but the webs.
 */
class AutoTrapWebGameTest : FabricClientGameTest {

    private var groundY = 0
    private val feet get() = BlockPos(0, groundY, TARGET_BLOCK_Z)
    private val head get() = feet.above()

    override fun runTest(context: ClientGameTestContext) {
        context.waitForClient()
        val saved = context.fromClient { ConfigSystem.serializeValueGroup(ModuleAutoTrap) }
        val savedTargets = context.fromClient { ConfigSystem.serializeValueGroup(GlobalSettingsTarget) }

        try {
            context.worldBuilder().create().use { world ->
                val server = world.server
                server.runCommand("gamemode survival @a")
                context.waitFor({ client -> client.player?.onGround() == true }, SETTLE_TICKS)
                groundY = context.fromClient { client -> checkNotNull(client.player).blockY }
                server.runCommand("execute as @a run tp @s 0.5 $groundY 0.5 0 0")
                server.runCommand("item replace entity @a hotbar.0 with cobweb 64")

                val target = context.fromClient { client -> addTarget(checkNotNull(client.level)) }
                context.onClient {
                    GlobalSettingsTarget.combatChoices.deserializeFrom(
                        GSON_DEFAULT, JsonArray().apply { add("Players") }
                    )
                    // The target is closer than the default minimum of 3 while it stands
                    ModuleAutoTrap.settings.getValue("Range").setByString("2.0..5.0")
                    ModuleAutoTrap.settings.getValue("Delay").setByString("0")
                    autoWeb("OnlyFalling", "true")
                    autoWeb("LeadTicks", "20")
                    ModuleAutoTrap.enabled = true
                }

                try {
                    assertFalls(context, server, target)
                } finally {
                    context.onClient { client ->
                        checkNotNull(client.level).removeEntity(target.id, Entity.RemovalReason.DISCARDED)
                    }
                }
            }
        } finally {
            context.onClient {
                ModuleAutoTrap.enabled = false
                ConfigSystem.deserializeValueGroup(ModuleAutoTrap, saved)
                ConfigSystem.deserializeValueGroup(GlobalSettingsTarget, savedTargets)
            }
        }
    }

    private fun assertFalls(context: ClientGameTestContext, server: TestServerContext, target: RemotePlayer) {
        check(launch(context, server, target).feet == null) { "LeadTicks 20 still found time in a short fall" }

        context.onClient { autoWeb("LeadTicks", "8") }
        repeat(STAND_TICKS) {
            context.onClient { target.setOnGround(true) }
            context.waitTick()
        }
        check(!server.hasWeb(feet)) { "OnlyFalling webbed a target standing on the ground" }

        val webbed = launch(context, server, target)
        check(webbed.feet != null && webbed.feet > IN_AIR) { "No web at the landing spot while falling: $webbed" }

        clearWebs(server)
        context.onClient { autoWeb("HeadHeight", "true") }
        val both = launch(context, server, target)
        check(both.feet != null && both.head != null) { "HeadHeight did not add the head web: $both" }

        clearWebs(server)
        context.onClient {
            autoWeb("HeadHeight", "false")
            ModuleAutoTrap.settings.getValue("Rotate").setByString("false")
        }
        server.runCommand("execute as @a run tp @s 0.5 $groundY 0.5 0 $LOOK_UP_PITCH")
        check(launch(context, server, target).feet == null) { "Without Rotate, a web went where nobody aimed" }
        server.runCommand("execute as @a run tp @s 0.5 $groundY 0.5 0 $LANDING_SPOT_PITCH")
        check(launch(context, server, target).feet != null) { "Without Rotate, the aimed landing spot was not webbed" }

        clearWebs(server)
        context.onClient { ModuleAutoTrap.settings.getValue("Rotate").setByString("true") }
        server.runCommand("execute as @a run tp @s 0.5 $groundY 0.5 0 0")
        server.runCommand("clear @a")
        server.runCommand("item replace entity @a inventory.0 with cobweb 64")
        check(launch(context, server, target).feet == null) { "A web came out of the inventory without Refill" }
        context.onClient { autoWeb("Refill", "Enabled", "true") }
        check(launch(context, server, target).feet != null) { "Refill did not web from the inventory" }
        check(context.fromClient { client ->
            (0..8).any { slot -> checkNotNull(client.player).inventory.getItem(slot).`is`(Items.COBWEB) }
        }) { "Refill left the hotbar without webs" }
    }

    /**
     * Throws the target straight up and lets it fall back with the game's gravity, one position per tick.
     *
     * @return how high above the ground the target was when each web showed up, null if it did not before landing
     */
    private fun launch(context: ClientGameTestContext, server: TestServerContext, target: RemotePlayer): Webbed {
        var height = 0.0
        var speed = LAUNCH_SPEED
        var feetAt: Double? = null
        var headAt: Double? = null

        repeat(FALL_TICKS) {
            val lastHeight = height
            height = (height + speed).coerceAtLeast(0.0)
            speed = (speed - GRAVITY) * DRAG
            val landed = height == 0.0

            context.onClient {
                target.xo = TARGET_X
                target.yo = groundY + lastHeight
                target.zo = TARGET_Z
                target.setPos(TARGET_X, groundY + height, TARGET_Z)
                target.setOnGround(landed)
            }
            context.waitTick()

            if (feetAt == null && server.hasWeb(feet)) {
                feetAt = height
            }
            if (headAt == null && server.hasWeb(head)) {
                headAt = height
            }

            if (landed) {
                return Webbed(feetAt, headAt)
            }
        }

        error("The target did not land")
    }

    private data class Webbed(val feet: Double?, val head: Double?)

    private fun TestServerContext.hasWeb(pos: BlockPos) = computeOnServer<Boolean, RuntimeException> { minecraft ->
        minecraft.overworld().getBlockState(pos).`is`(Blocks.COBWEB)
    }

    private fun clearWebs(server: TestServerContext) {
        server.runCommand("fill -2 $groundY 1 2 ${groundY + 2} 5 air replace cobweb")
    }

    private fun autoWeb(vararg path: String) {
        var group = ModuleAutoTrap.settings.getValue("AutoWeb") as ValueGroup
        for (name in path.dropLast(2)) {
            group = group.containedValues.first { it.name == name } as ValueGroup
        }
        group.containedValues.first { it.name == path[path.size - 2] }.setByString(path.last())
    }

    private fun addTarget(level: ClientLevel) =
        RemotePlayer(level, GameProfile(UUID.randomUUID(), "WebTarget")).apply {
            id = level.nextLocalEntityId()
            health = 20f
            setPos(TARGET_X, groundY.toDouble(), TARGET_Z)
            setOnGround(true)
            level.addEntity(this)
        }

    private companion object {
        const val TARGET_X = 0.5
        const val TARGET_Z = 3.5
        const val TARGET_BLOCK_Z = 3

        // The game's gravity and drag for a falling player
        const val GRAVITY = 0.08
        const val DRAG = 0.98

        // Lifts the target about 4 blocks and keeps it in the air for 21 ticks
        const val LAUNCH_SPEED = 0.8
        const val IN_AIR = 0.3

        const val LOOK_UP_PITCH = -60
        // Looks at the ground below the target from the eyes, 3 blocks away and 1.62 above
        const val LANDING_SPOT_PITCH = 28.4

        const val SETTLE_TICKS = 200
        const val STAND_TICKS = 20
        const val FALL_TICKS = 60
    }

}
