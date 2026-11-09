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

import net.ccbluex.liquidbounce.features.module.modules.world.ModuleAutoTool
import net.ccbluex.liquidbounce.gametest.onClient
import net.minecraft.core.registries.BuiltInRegistries

private const val PICKAXE_SLOT = 1
private const val SHOVEL_SLOT = 2
private const val SWITCH_TICKS = 40
private const val SWITCH_DELAY = 5

/**
 * AutoTool switches to the pickaxe while only aiming at stone, and with OnlyBlockBelow only for the block below,
 * after its switch delay.
 */
fun InventoryBench.checkAutoTool() {
    // Stone at eye height in front, looking east
    val front = origin.offset(1, 1, 0)
    command("setblock ${front.x} ${front.y} ${front.z} minecraft:stone")
    command("item replace entity @a hotbar.0 with minecraft:dirt")
    command("item replace entity @a hotbar.$PICKAXE_SLOT with minecraft:diamond_pickaxe")

    fun look(pitch: Float) = context.onClient { client ->
        val player = checkNotNull(client.player)
        player.yRot = -90f
        player.xRot = pitch
    }

    withModule(ModuleAutoTool, { ModuleAutoTool.set("OnAim", value = "true") }) {
        look(0f)
        waitUntil(SWITCH_TICKS, { "aiming at stone did not select the pickaxe" }) { serverSlot == PICKAXE_SLOT }
    }

    val below = origin.below()
    command("setblock ${below.x} ${below.y} ${below.z} minecraft:stone")
    command("item replace entity @a hotbar.0 with minecraft:dirt")
    command("item replace entity @a hotbar.$PICKAXE_SLOT with minecraft:diamond_pickaxe")
    withModule(ModuleAutoTool, {
        ModuleAutoTool.set("OnAim", value = "true")
        ModuleAutoTool.set("OnlyBlockBelow", value = "true")
        ModuleAutoTool.set("SwitchDelay", value = "$SWITCH_DELAY..$SWITCH_DELAY")
    }) {
        look(0f)
        context.waitTicks(SWITCH_TICKS / 2)
        check(serverSlot == 0) { "the pickaxe was selected for a block that is not below" }

        look(90f)
        val ticks = waitUntil(SWITCH_TICKS, { "aiming at the block below did not select the pickaxe" }) {
            serverSlot == PICKAXE_SLOT
        }
        check(ticks >= SWITCH_DELAY) { "the pickaxe was selected after $ticks ticks, before the delay" }
    }

    command("setblock ${front.x} ${front.y} ${front.z} minecraft:air")
}

/**
 * AutoTool leaves unbreakable blocks alone, and PickaxeOnly never picks another tool.
 */
fun InventoryBench.checkAutoToolFilters() {
    val below = origin.below()
    fun setBelow(block: String) {
        command("setblock ${below.x} ${below.y} ${below.z} minecraft:$block")
        context.waitFor({ client ->
            BuiltInRegistries.BLOCK.getKey(checkNotNull(client.level).getBlockState(below).block).path == block
        }, SWITCH_TICKS)
    }
    fun giveTools() {
        command("item replace entity @a hotbar.0 with minecraft:dirt")
        command("item replace entity @a hotbar.$PICKAXE_SLOT with minecraft:diamond_pickaxe")
        command("item replace entity @a hotbar.$SHOVEL_SLOT with minecraft:diamond_shovel")
    }
    fun lookDown() = context.onClient { client -> checkNotNull(client.player).xRot = 90f }
    fun expectNoSwitch(reason: String) {
        context.waitTicks(SWITCH_TICKS / 2)
        check(serverSlot == 0) { "slot $serverSlot was selected for $reason" }
    }

    // The static slot would be selected for any block
    setBelow("bedrock")
    giveTools()
    withModule(ModuleAutoTool, {
        ModuleAutoTool.set("OnAim", value = "true")
        ModuleAutoTool.set("ToolSelector", value = "Static")
        ModuleAutoTool.set("ToolSelector", "Static", "Slot", value = "$PICKAXE_SLOT")
    }) {
        lookDown()
        expectNoSwitch("bedrock")

        setBelow("stone")
        waitUntil(SWITCH_TICKS, { "the static slot was not selected for stone" }) { serverSlot == PICKAXE_SLOT }
    }

    setBelow("dirt")
    giveTools()
    withModule(ModuleAutoTool, {
        ModuleAutoTool.set("OnAim", value = "true")
        ModuleAutoTool.set("PickaxeOnly", value = "true")
    }) {
        lookDown()
        expectNoSwitch("dirt with PickaxeOnly")

        context.onClient { ModuleAutoTool.set("PickaxeOnly", value = "false") }
        waitUntil(SWITCH_TICKS, { "the shovel was not selected for dirt without PickaxeOnly" }) {
            serverSlot == SHOVEL_SLOT
        }
    }
    setBelow("stone")
}
