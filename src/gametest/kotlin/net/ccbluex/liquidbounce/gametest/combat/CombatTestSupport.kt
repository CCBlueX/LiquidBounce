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
package net.ccbluex.liquidbounce.gametest.combat

import com.google.gson.JsonArray
import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.config.gson.util.GSON_DEFAULT
import net.ccbluex.liquidbounce.config.types.Value
import net.ccbluex.liquidbounce.config.types.group.ValueGroup
import net.ccbluex.liquidbounce.config.types.list.MultiChoiceListValue
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.events.AttackEntityEvent
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.gametest.fromClient
import net.ccbluex.liquidbounce.gametest.onClient
import net.ccbluex.liquidbounce.gametest.waitForClient
import net.ccbluex.liquidbounce.utils.client.mc
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext
import net.minecraft.network.protocol.game.ServerboundPunchPacket
import net.minecraft.util.Mth
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.monster.zombie.Husk
import net.minecraft.world.level.levelgen.Heightmap
import kotlin.math.abs

/**
 * Runs [checks] in a survival world with a stone floor around the player at 0.5 y 0.5, facing south. The settings
 * of [modules] are restored afterwards.
 */
internal fun ClientGameTestContext.combatTest(
    vararg modules: ClientModule,
    checks: (server: TestServerContext, y: Int) -> Unit,
) {
    waitForClient()
    val saved = fromClient { modules.map(ConfigSystem::serializeValueGroup) }

    try {
        worldBuilder().create().use { world ->
            val server = world.server
            val y = server.computeOnServer<Int, RuntimeException> {
                it.overworld().getHeight(Heightmap.Types.MOTION_BLOCKING, 0, 0)
            }
            server.runCommand("gamerule spawn_mobs false")
            server.runCommand("fill -8 ${y - 1} -8 8 ${y - 1} 8 stone")
            server.runCommand("fill -8 $y -8 8 ${y + 4} 8 air")
            server.runCommand("gamemode survival @a")
            server.runCommand("clear @a")
            server.runCommand("kill @e[type=!player]")
            face(this, server, y, 0f)

            checks(server, y)
        }
    } finally {
        onClient {
            modules.forEach { it.enabled = false }
            modules.zip(saved).forEach { (module, json) -> ConfigSystem.deserializeValueGroup(module, json) }
        }
    }
}

/**
 * What the combat checks observe: attacks with the state of the player, and swings.
 */
internal object CombatEvents : EventListener {
    @Volatile
    var attacks = mutableListOf<Entity>()

    @Volatile
    var punches = 0

    @Volatile
    var fallingAttacks = 0

    @Volatile
    var airborneAttacks = 0

    @Suppress("unused")
    private val attackHandler = handler<AttackEntityEvent> { event ->
        val player = checkNotNull(mc.player)
        attacks += event.entity
        if (!player.onGround()) {
            airborneAttacks++
            if (player.deltaMovement.y < 0.0) {
                fallingAttacks++
            }
        }
    }

    @Suppress("unused")
    private val packetHandler = handler<PacketEvent> { event ->
        if (event.packet is ServerboundPunchPacket) {
            punches++
        }
    }

    fun reset() {
        attacks = mutableListOf()
        punches = 0
        fallingAttacks = 0
        airborneAttacks = 0
    }
}

internal fun summonHusk(
    context: ClientGameTestContext,
    server: TestServerContext,
    y: Int,
    x: Double,
    z: Double,
    health: Int = HUSK_HEALTH,
    equipment: String = "{}",
) = summon(
    context, server, "husk $x $y $z", "NoAI:1b,PersistenceRequired:1b,Silent:1b,Health:${health}f," +
        "attributes:[{id:\"minecraft:max_health\",base:${maxOf(health, MAX_HEALTH)}d}],equipment:$equipment"
)

internal fun summonCrystal(context: ClientGameTestContext, server: TestServerContext, y: Int, x: Double, z: Double) =
    summon(context, server, "end_crystal $x $y $z", "ShowBottom:0b,Invulnerable:1b")

/**
 * Summons an entity with [nbt] and waits for the client to see it.
 */
internal fun summon(context: ClientGameTestContext, server: TestServerContext, what: String, nbt: String): Int {
    val tag = "combat${summonCount++}"
    server.runCommand("summon $what {Tags:[\"$tag\"],$nbt}")
    val id = server.computeOnServer<Int, RuntimeException> { s ->
        s.overworld().allEntities.first { tag in it.entityTags() }.id
    }
    context.waitFor({ client -> client.level?.getEntity(id) != null }, WAIT_TICKS)
    return id
}

private var summonCount = 0

internal fun TestServerContext.discard(id: Int) = runOnServer<RuntimeException> { s ->
    s.overworld().getEntity(id)?.discard()
}

internal fun TestServerContext.huskHealth(id: Int) = computeOnServer<Int, RuntimeException> { s ->
    (s.overworld().getEntity(id) as Husk).health.toInt()
}

internal fun face(
    context: ClientGameTestContext,
    server: TestServerContext,
    y: Int,
    yaw: Float,
    pitch: Float = 0f,
) {
    server.runCommand("tp @a 0.5 $y 0.5 $yaw $pitch")
    context.waitFor({ client ->
        val player = client.player ?: return@waitFor false
        abs(Mth.wrapDegrees(player.yRot - yaw)) < 1f && abs(player.xRot - pitch) < 1f && player.y == y.toDouble()
    }, WAIT_TICKS)
    context.waitTicks(SETTLE_TICKS)
}

internal fun ValueGroup.value(path: String): Value<*> {
    var group = this
    val names = path.split('/')
    for (name in names.dropLast(1)) {
        group = group.containedValues.first { it.name == name } as ValueGroup
    }
    return group.containedValues.first { it.name == names.last() }
}

internal fun ValueGroup.set(path: String, value: String) {
    val target = value(path)
    if (target is MultiChoiceListValue<*>) {
        target.deserializeFrom(GSON_DEFAULT, JsonArray().apply { value.split(',').forEach(::add) })
    } else {
        target.setByString(value)
    }
}

/**
 * Sets the reaction delay group at [path] to always take [REACTION_MS], or turns it off.
 */
internal fun ValueGroup.setReactionDelay(path: String, enabled: Boolean) {
    set("$path/Mean", REACTION_MS.toString())
    set("$path/Deviation", "0")
    set("$path/Limits", "0..$REACTION_MS")
    set("$path/Enabled", enabled.toString())
}

@Suppress("UNCHECKED_CAST")
internal fun <T> Any.privateField(name: String): T =
    javaClass.getDeclaredField(name).apply { isAccessible = true }.get(this) as T

internal const val WAIT_TICKS = 100
internal const val SETTLE_TICKS = 5
internal const val MEASURE_TICKS = 30
internal const val HUSK_HEALTH = 1000
internal const val MAX_HEALTH = 20
internal const val REACTION_MS = 1000
internal const val REACTION_TICKS = REACTION_MS / 50
