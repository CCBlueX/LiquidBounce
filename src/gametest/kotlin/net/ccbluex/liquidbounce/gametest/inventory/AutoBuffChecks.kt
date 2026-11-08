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

import net.ccbluex.liquidbounce.features.module.modules.player.autobuff.ModuleAutoBuff
import net.ccbluex.liquidbounce.gametest.fromClient
import net.minecraft.core.Holder
import net.minecraft.core.component.DataComponents
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.EntityTypes
import net.minecraft.world.item.Items
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.alchemy.Potion
import net.minecraft.world.item.alchemy.Potions

private const val THROW_TICKS = 100

private fun splash(potion: String) = "minecraft:splash_potion[potion_contents={potion:\"minecraft:$potion\"}]"

/**
 * AutoBuff's Pot trigger, pitch, mid-air throws, disabling once buffed and the healing-only refill.
 */
fun InventoryBench.checkAutoBuff() {
    checkHeldTrigger()
    checkPitch()
    checkMidAir()
    checkDisableWhenBuffed()
    checkHealingOnlyRefill()
}

private fun InventoryBench.checkHeldTrigger() {
    // Slowness is no wanted effect, only the held trigger throws it
    command("item replace entity @a hotbar.0 with ${splash("slowness")}")
    withModule(ModuleAutoBuff, { ModuleAutoBuff.set("Pot", "Trigger", value = "Held") }) {
        waitUntil(THROW_TICKS, { "the held slowness potion was not thrown" }) {
            onServer { it.hasEffect(MobEffects.SLOWNESS) }
        }
    }
}

private fun InventoryBench.checkPitch() {
    command("item replace entity @a hotbar.0 with ${splash("swiftness")}")
    withModule(ModuleAutoBuff, { ModuleAutoBuff.set("Pot", "Pitch", value = "0.0..0.0") }) {
        var horizontalSpeed = 0.0
        waitUntil(THROW_TICKS, { "no potion was thrown" }) {
            horizontalSpeed = server.computeOnServer<Double, RuntimeException> { server ->
                server.overworld().getEntities(EntityTypes.SPLASH_POTION) { true }
                    .maxOfOrNull { it.deltaMovement.horizontalDistance() } ?: 0.0
            }
            horizontalSpeed > 0.0
        }
        // Straight down leaves 0.17 blocks a tick sideways, a level throw 0.47
        check(horizontalSpeed > LEVEL_THROW_SPEED) { "the potion flew $horizontalSpeed blocks a tick sideways" }
    }
}

private fun InventoryBench.checkMidAir() {
    withModule(ModuleAutoBuff, {
        ModuleAutoBuff.set("Pot", "MidAir", "Enabled", value = "true")
        ModuleAutoBuff.set("Pot", "MidAir", "Delay", value = "1..1")
        ModuleAutoBuff.set("Rotations", "RotationTiming", value = "OnUse")
    }) {
        // Far above TillGroundDistance, so the potions would otherwise wait for the landing
        command("execute as @a at @s run tp @s ~ ~$FALL_HEIGHT ~")
        command("item replace entity @a hotbar.0 with ${splash("swiftness")}")
        command("item replace entity @a hotbar.1 with ${splash("strength")}")
        waitUntil(THROW_TICKS, { "speed and strength were not thrown" }) {
            count(Items.SPLASH_POTION) == 0
        }
        val height = onServer { it.y } - origin.y
        check(height > MID_AIR_HEIGHT) { "the potions were thrown only $height blocks above the ground" }
    }
}

private fun InventoryBench.checkDisableWhenBuffed() {
    for (effect in arrayOf("regeneration", "strength", "fire_resistance", "jump_boost", "water_breathing")) {
        command("effect give @a $effect 120 0 true")
    }
    command("item replace entity @a hotbar.0 with ${splash("swiftness")}")
    withModule(ModuleAutoBuff, { ModuleAutoBuff.set("Pot", "DisableWhenBuffed", value = "true") }) {
        waitUntil(THROW_TICKS, { "AutoBuff stayed enabled with every effect active" }) {
            onServer { it.hasEffect(MobEffects.SPEED) } && !context.fromClient { ModuleAutoBuff.enabled }
        }
    }
}

private fun InventoryBench.checkHealingOnlyRefill() {
    command("item replace entity @a container.9 with ${splash("healing")}")
    command("item replace entity @a container.10 with ${splash("swiftness")}")
    withModule(ModuleAutoBuff, { ModuleAutoBuff.set("Refill", "HealingPotionsOnly", value = "true") }) {
        waitUntil(THROW_TICKS, { "the healing potion was not moved into the hotbar" }) {
            (0..8).any { slot -> stackAt(slot).isPotion(Potions.HEALING) }
        }
        context.waitTicks(GROUND_TICKS)
        check(stackAt(10).isPotion(Potions.SWIFTNESS)) { "the swiftness potion left the inventory" }
    }
}

private fun ItemStack.isPotion(potion: Holder<Potion>) = get(DataComponents.POTION_CONTENTS)?.`is`(potion) == true

private const val LEVEL_THROW_SPEED = 0.35
private const val FALL_HEIGHT = 10
private const val MID_AIR_HEIGHT = 5.0
private const val GROUND_TICKS = 20
