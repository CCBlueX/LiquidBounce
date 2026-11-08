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

import com.mojang.authlib.GameProfile
import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.features.module.modules.combat.ModuleAutoWeapon
import net.ccbluex.liquidbounce.gametest.onClient
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext
import net.fabricmc.fabric.api.entity.FakePlayer
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.InteractionHand
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.phys.Vec3
import java.util.UUID

/**
 * AutoShieldBreak takes an axe from the inventory for a click on a raised shield, breaks the shield with it and
 * puts the axe back. A config that has AutoShieldBreak turned off as a plain boolean keeps it off.
 */
class AutoWeaponShieldGameTest : FabricClientGameTest {

    override fun runTest(context: ClientGameTestContext) = context.runCombatChecks(
        listOf(ModuleAutoWeapon),
        CombatScene::checkAxeFromInventory,
        CombatScene::checkBooleanConfig,
    )

}

private const val SWITCH_BACK = 40
private const val TIMEOUT_TICKS = 100
private const val IDLE_TICKS = 10
private const val TICKS_PER_SECOND = 20
private const val FACING_PLAYER_YAW = 180f
private const val TARGET_DISTANCE = 2.2

private const val BOOLEAN_CONFIG = """{"name": "AutoWeapon", "value": [{"name": "AutoShieldBreak", "value": false}]}"""

private val SHIELD = ItemStack(Items.SHIELD)
private val AXE = ItemStack(Items.IRON_AXE)

private fun CombatScene.click(id: Int) {
    context.waitFor({ client -> crosshairEntity(client) == id }, CombatScene.ENTITY_TIMEOUT_TICKS)
    context.input.holdKeyFor({ it.keyAttack }, 1)
}

/**
 * The inventory slot of the axe on the server, -1 without one.
 */
private fun CombatScene.axeSlot(server: MinecraftServer): Int =
    serverPlayer(server).inventory.findSlotMatchingItem(AXE)

private fun CombatScene.checkAxeFromInventory() {
    reset()
    world.server.runCommand("item replace entity @a hotbar.0 with minecraft:stick")
    world.server.runCommand("item replace entity @a inventory.0 with minecraft:iron_axe")
    val inventorySlot = onServer { axeSlot(it) }
    val target = spawnShieldPlayer(at(0.0, 0.0, TARGET_DISTANCE))
    try {
        context.onClient {
            ModuleAutoWeapon.set("AutoShieldBreak/ConsiderInventory/Enabled", "true")
            ModuleAutoWeapon.set("SwitchBack", "$SWITCH_BACK..$SWITCH_BACK")
            ModuleAutoWeapon.enabled = true
        }

        // The click that finds no axe in the hotbar lands on the shield with the stick
        click(target.id)
        world.server.waitFor({ server -> Inventory.isHotbarSlot(axeSlot(server)) }, TIMEOUT_TICKS)
        check(onServer { target.isUsingItem }) { "AutoWeapon: the shield went down before the axe was in the hotbar" }

        click(target.id)
        world.server.waitFor({ target.cooldowns.isOnCooldown(SHIELD) }, TIMEOUT_TICKS)

        world.server.waitFor({ server -> axeSlot(server) == inventorySlot }, TIMEOUT_TICKS)
    } finally {
        removeShieldPlayer(target)
        context.onClient { ModuleAutoWeapon.enabled = false }
    }
}

private fun CombatScene.checkBooleanConfig() {
    reset()
    world.server.runCommand("item replace entity @a hotbar.0 with minecraft:stick")
    world.server.runCommand("item replace entity @a hotbar.1 with minecraft:iron_axe")
    val target = spawnShieldPlayer(at(0.0, 0.0, TARGET_DISTANCE))
    try {
        context.onClient {
            ConfigSystem.deserializeValueGroup(ModuleAutoWeapon, BOOLEAN_CONFIG)
            ModuleAutoWeapon.enabled = true
        }

        click(target.id)
        context.waitTicks(IDLE_TICKS)
        val (blocking, slot) = onServer { target.isUsingItem to serverPlayer(it).inventory.selectedSlot }
        check(blocking && slot == 0) {
            "AutoWeapon: AutoShieldBreak was off in the config, shield up: $blocking, server slot $slot"
        }
    } finally {
        removeShieldPlayer(target)
        context.onClient { ModuleAutoWeapon.enabled = false }
    }
}

/**
 * A server-side player that can be hurt. The server only disables the shields of players, and this one does not
 * tick, so it keeps blocking until its shield is disabled.
 */
private class ShieldPlayer(level: ServerLevel) : FakePlayer(level, GameProfile(UUID.randomUUID(), "ShieldPlayer")) {

    override fun isInvulnerableTo(level: ServerLevel, damageSource: DamageSource) = false

    fun raiseShield() {
        startUsingItem(InteractionHand.OFF_HAND)
        // Skip the shield's block delay, which a ticking player would wait out
        useItemRemaining -= TICKS_PER_SECOND
    }

}

/**
 * Adds a shield-blocking player facing the scene's player and waits until the client sees it blocking.
 */
private fun CombatScene.spawnShieldPlayer(pos: Vec3): ShieldPlayer {
    val target = onServer { server ->
        val level = server.overworld()
        ShieldPlayer(level).apply {
            snapTo(pos.x, pos.y, pos.z, FACING_PLAYER_YAW, 0f)
            yHeadRot = FACING_PLAYER_YAW
            setItemInHand(InteractionHand.OFF_HAND, SHIELD.copy())
            // The client only adds players it has a tab list entry for
            server.playerList.broadcastAll(ClientboundPlayerInfoUpdatePacket.createPlayerInitializing(listOf(this)))
            level.addNewPlayer(this)
        }
    }
    context.waitFor({ client -> client.level?.getEntity(target.id) != null }, CombatScene.ENTITY_TIMEOUT_TICKS)
    onServer { target.raiseShield() }
    context.waitFor({ client -> (clientEntity(client, target.id) as LivingEntity).isBlocking },
        CombatScene.ENTITY_TIMEOUT_TICKS)
    return target
}

private fun CombatScene.removeShieldPlayer(target: ShieldPlayer) = onServer { server ->
    server.overworld().removePlayerImmediately(target, Entity.RemovalReason.DISCARDED)
    server.playerList.broadcastAll(ClientboundPlayerInfoRemovePacket(listOf(target.uuid)))
}
