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

import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleStorageESP
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext
import net.minecraft.util.ARGB
import net.minecraft.world.level.levelgen.Heightmap

/**
 * [ModuleStorageESP] highlights trapped chests and droppers in their own colours, apart from chests and dispensers.
 * The player looks east at a chest (bottom left), a dispenser (top left), a trapped chest (bottom right) and a
 * dropper (top right); every quarter of the screenshot has to glow in the colour of its block's category only.
 */
class StorageEspCategoriesGameTest : FabricClientGameTest {

    override fun runTest(context: ClientGameTestContext) {
        context.waitForClient()
        val saved = context.fromClient { ConfigSystem.serializeValueGroup(ModuleStorageESP) }

        try {
            context.worldBuilder().create().use { world ->
                val server = world.server
                val y = server.computeOnServer<Int, RuntimeException> {
                    it.overworld().getHeight(Heightmap.Types.MOTION_BLOCKING, 0, 0)
                }
                server.runCommand("fill -12 $y -12 24 ${y + 10} 12 air")
                server.runCommand("fill -12 ${y - 1} -12 24 ${y - 1} 12 smooth_stone")
                server.runCommand("gamemode creative @a")
                server.runCommand("time set 6000")
                server.runCommand("weather clear")
                server.runCommand("setblock 6 $y -2 chest")
                server.runCommand("setblock 6 ${y + 2} -2 dispenser")
                server.runCommand("setblock 6 $y 3 trapped_chest")
                server.runCommand("setblock 6 ${y + 2} 3 dropper")
                server.runCommand("tp @a 0.5 $y 0.5 -90 0")
                world.connection.waitForChunksRender()

                context.onClient {
                    setValue(ModuleStorageESP, "Mode", to = "Glow")
                    // keeps storage generated out of sight from glowing through the terrain
                    setValue(ModuleStorageESP, "DistanceFade", "FarStart", to = "12")
                    setValue(ModuleStorageESP, "DistanceFade", "FarEnd", to = "16")
                    setValue(ModuleStorageESP, "Chest", "Color", to = "#FF00FF")
                    setValue(ModuleStorageESP, "Dispenser", "Color", to = "#0000FF")
                    setValue(ModuleStorageESP, "TrappedChest", "Color", to = "#0000FF")
                    setValue(ModuleStorageESP, "Dropper", "Color", to = "#FF00FF")
                    ModuleStorageESP.enabled = true
                }
                context.waitTicks(SETTLE_TICKS)

                context.checkQuarter("chest", right = false, top = false, ::isMagentaGlow, ::isBlueGlow)
                context.checkQuarter("dispenser", right = false, top = true, ::isBlueGlow, ::isMagentaGlow)
                context.checkQuarter("trapped-chest", right = true, top = false, ::isBlueGlow, ::isMagentaGlow)
                context.checkQuarter("dropper", right = true, top = true, ::isMagentaGlow, ::isBlueGlow)
            }
        } finally {
            context.onClient {
                ModuleStorageESP.enabled = false
                ConfigSystem.deserializeValueGroup(ModuleStorageESP, saved)
            }
        }
    }

    private fun ClientGameTestContext.checkQuarter(
        block: String,
        right: Boolean,
        top: Boolean,
        own: (Int) -> Boolean,
        other: (Int) -> Boolean,
    ) {
        val (ownPixels, otherPixels) = countPixels("storage-esp-$block", own, other) { x, y ->
            (x > 0.5F) == right && (y < 0.5F) == top
        }
        check(ownPixels > MIN_GLOW_PIXELS && otherPixels < MIN_GLOW_PIXELS) {
            "The $block glows in $ownPixels pixels of its own colour and $otherPixels of the other one"
        }
    }

    private fun isMagentaGlow(pixel: Int) = ARGB.red(pixel) > 150 && ARGB.blue(pixel) > 150 && ARGB.green(pixel) < 60

    private fun isBlueGlow(pixel: Int) = ARGB.blue(pixel) > 150 && ARGB.red(pixel) < 60 && ARGB.green(pixel) < 60

    private companion object {
        const val SETTLE_TICKS = 10
        const val MIN_GLOW_PIXELS = 100
    }

}
