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
package net.ccbluex.liquidbounce.gametest.inventory

import com.mojang.blaze3d.platform.InputConstants
import net.ccbluex.liquidbounce.features.module.modules.player.ModuleAutoWindCharge
import net.minecraft.world.item.Items

private const val CHARGES = 8
private const val THROW_TICKS = 40
private const val MIN_LIFT = 0.5
private const val FALL_HEIGHT = 10

/**
 * The throw key of AutoWindCharge throws one charge at the feet per press, without the jump key: from the ground,
 * which lifts the player, and while falling, where holding jump throws nothing.
 */
fun InventoryBench.checkAutoWindChargeThrowKey() {
    command("item replace entity @a hotbar.0 with minecraft:wind_charge $CHARGES")
    withModule(ModuleAutoWindCharge, {
        ModuleAutoWindCharge.setting<InputConstants.Key>("ThrowKey").set(testKey)
    }) {
        context.waitFor({ client -> client.player?.inventory?.countItem(Items.WIND_CHARGE) == CHARGES }, THROW_TICKS)

        val groundY = onServer { it.y }
        var lift = 0.0
        context.input.pressKey(testKey)
        waitUntil(THROW_TICKS, {
            "the throw key did not lift the player, highest ${"%.2f".format(lift)}, ${count(Items.WIND_CHARGE)} left"
        }) {
            lift = maxOf(lift, onServer { it.y } - groundY)
            count(Items.WIND_CHARGE) == CHARGES - 1 && lift >= MIN_LIFT
        }
        context.waitTicks(THROW_TICKS)
        check(count(Items.WIND_CHARGE) == CHARGES - 1) { "one press threw more than one charge" }

        context.waitFor({ client -> client.player?.onGround() == true }, THROW_TICKS)
        command("execute as @a at @s run tp @s ~ ~$FALL_HEIGHT ~")
        context.waitFor({ client -> client.player?.onGround() == false }, THROW_TICKS)
        context.input.pressKey(testKey)
        waitUntil(THROW_TICKS, { "the throw key did not throw while falling" }) {
            count(Items.WIND_CHARGE) == CHARGES - 2
        }
        check(!onServer { it.onGround() }) { "the throw key waited for the landing" }
    }
}
