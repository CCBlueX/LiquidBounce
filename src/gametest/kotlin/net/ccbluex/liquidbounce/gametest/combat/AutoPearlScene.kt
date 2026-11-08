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

import net.ccbluex.liquidbounce.features.module.modules.misc.ModuleAutoPearl
import net.ccbluex.liquidbounce.gametest.onClient
import net.minecraft.server.MinecraftServer
import net.minecraft.world.entity.EntityTypes
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.phys.Vec3

const val PEARL_TICKS = 30

/**
 * Gives the player ender pearls and enables AutoPearl after [configure].
 */
fun CombatScene.startAutoPearl(configure: () -> Unit) {
    reset()
    world.server.runCommand("item replace entity @a hotbar.0 with minecraft:ender_pearl 16")
    context.onClient {
        configure()
        ModuleAutoPearl.enabled = true
    }
}

/**
 * Launches a pearl owned by [owner] from [from] and returns after how many ticks the player threw one,
 * or null if it did not within [PEARL_TICKS]. Removes all pearls before they land.
 */
fun CombatScene.pearlThrownAfter(owner: Int?, from: Vec3): Int? {
    onServer { server ->
        val level = server.overworld()
        val pearl = if (owner == null) {
            ThrownEnderpearl(EntityTypes.ENDER_PEARL, level)
        } else {
            ThrownEnderpearl(level, serverEntity(server, owner) as LivingEntity, ItemStack(Items.ENDER_PEARL))
        }
        pearl.setPos(from)
        pearl.deltaMovement = Vec3(0.0, 0.4, 1.2)
        level.addFreshEntity(pearl)
    }

    var thrownAfter: Int? = null
    for (tick in 1..PEARL_TICKS) {
        context.waitTick()
        if (onServer(::hasOwnPearl)) {
            thrownAfter = tick
            break
        }
    }

    world.server.runCommand("kill @e[type=minecraft:ender_pearl]")
    context.waitTicks(2)
    return thrownAfter
}

private fun CombatScene.hasOwnPearl(server: MinecraftServer): Boolean {
    val player = serverPlayer(server)
    return server.overworld().getEntities(EntityTypes.ENDER_PEARL) { it.owner === player }.isNotEmpty()
}
