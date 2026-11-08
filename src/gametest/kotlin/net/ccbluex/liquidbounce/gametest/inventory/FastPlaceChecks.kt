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

import com.google.gson.JsonArray
import net.ccbluex.liquidbounce.config.gson.util.GSON_DEFAULT
import net.ccbluex.liquidbounce.features.module.modules.world.ModuleFastPlace
import net.ccbluex.liquidbounce.gametest.onClient
import net.minecraft.world.item.Items

private const val EGGS = 64
private const val USE_TICKS = 20

/**
 * FastPlace also drops the use delay of items that are neither blocks nor projectiles, here spawn eggs.
 */
fun InventoryBench.checkFastPlace() {
    command("item replace entity @a hotbar.0 with minecraft:chicken_spawn_egg $EGGS")
    withModule(ModuleFastPlace, {
        ModuleFastPlace.setting<Any>("ApplyTo").deserializeFrom(
            GSON_DEFAULT, JsonArray().apply { add("UsableItems") }
        )
    }) {
        context.onClient { client -> checkNotNull(client.player).xRot = 90f }
        context.input.holdKey { options -> options.keyUse }
        context.waitTicks(USE_TICKS)
        context.input.releaseKey { options -> options.keyUse }

        val used = EGGS - count(Items.CHICKEN_SPAWN_EGG)
        // The vanilla delay of 4 ticks allows 5 uses in 20 ticks
        check(used >= USE_TICKS / 2) { "$used spawn eggs used in $USE_TICKS ticks" }
    }
}
