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

import net.ccbluex.liquidbounce.features.module.modules.combat.crystalaura.ModuleCrystalAura
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext
import net.minecraft.core.BlockPos
import net.minecraft.util.Mth
import net.minecraft.world.entity.boss.enderdragon.EndCrystal
import net.minecraft.world.entity.monster.zombie.Husk
import net.minecraft.world.item.Items
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.AABB
import kotlin.math.abs

/**
 * Runs [block] in a new world against a NoAI husk on a crying obsidian floor, where crystals only fit on the obsidian
 * blocks the test sets. The player has Resistance V, so its own crystals never hurt it. The CrystalAura options are
 * restored afterwards.
 */
fun ClientGameTestContext.inCrystalAuraArena(block: CrystalAuraArena.() -> Unit) = restoring(ModuleCrystalAura) {
    worldBuilder().create().use { world ->
        CrystalAuraArena(this, world.server).apply {
            build()
            block()
        }
    }
}

class CrystalAuraArena(val context: ClientGameTestContext, private val server: TestServerContext) {

    lateinit var origin: BlockPos
        private set

    /** In the player's crosshair while it looks at the husk. */
    val a get() = origin.offset(0, -1, 2)

    val huskPos get() = origin.offset(0, 0, 3)

    /** Beside the husk, out of the crosshair. */
    val right get() = origin.offset(2, -1, 3)

    fun cmd(command: String) = server.runCommand(command)

    fun pos(pos: BlockPos) = "${pos.x} ${pos.y} ${pos.z}"

    fun build() {
        cmd("gamemode survival @a")
        cmd("difficulty normal")
        origin = server.query { it.playerList.players[0].blockPosition() }
        val (x, y, z) = Triple(origin.x, origin.y, origin.z)
        cmd("fill ${x - 6} ${y - 1} ${z - 6} ${x + 6} ${y - 1} ${z + 8} crying_obsidian")
        cmd("fill ${x - 6} $y ${z - 6} ${x + 6} ${y + 3} ${z + 8} air")
        cmd("setblock ${pos(a)} obsidian")
        cmd("tp @a ${x + 0.5} $y ${z + 0.5} 0 0")
        cmd("effect give @a resistance infinite 4 true")
        cmd("attribute @p minecraft:explosion_knockback_resistance base set 1")
        cmd("give @a end_crystal 64")
        configure("BasePlace/Enabled" to "false")
        cmd("summon husk ${x + 0.5} $y ${huskPos.z + 0.5} {NoAI:1b,PersistenceRequired:1b,Silent:1b," +
            "Health:1024f,attributes:[{id:\"minecraft:max_health\",base:1024d}," +
            "{id:\"minecraft:explosion_knockback_resistance\",base:1d}]}")
        context.waitFor({ client ->
            client.level!!.entitiesForRendering().any { it is Husk } &&
                client.player!!.inventory.countItem(Items.END_CRYSTAL) > 0 &&
                client.level!!.getBlockState(a).`is`(Blocks.OBSIDIAN)
        }, TIMEOUT)
    }

    fun crystalsOn(base: BlockPos): Int = crystalIdsOn(base).size

    fun crystalIdsOn(base: BlockPos): List<Int> = server.query {
        it.overworld().getEntitiesOfClass(EndCrystal::class.java, AABB(base.above())).map { crystal -> crystal.id }
    }

    fun serverRotation(): Pair<Float, Float> = server.query {
        it.playerList.players[0].let { player -> player.yRot to player.xRot }
    }

    fun configure(vararg options: Pair<String, String>) = context.onClient {
        for ((path, value) in options) {
            ModuleCrystalAura.set(path, value)
        }
    }

    fun Pair<Float, Float>.isCloseTo(other: Pair<Float, Float>) =
        abs(Mth.wrapDegrees(first - other.first)) <= ROTATION_TOLERANCE &&
            abs(second - other.second) <= ROTATION_TOLERANCE

    companion object {
        const val TIMEOUT = 60
        const val ROTATION_TOLERANCE = 0.5f
    }

}

/**
 * Waits up to [limit] ticks, calling [done] after each, and returns whether it returned `true` once.
 */
fun ClientGameTestContext.ticksUntil(limit: Int, done: () -> Boolean): Boolean {
    repeat(limit) {
        waitTick()
        if (done()) {
            return true
        }
    }

    return false
}
