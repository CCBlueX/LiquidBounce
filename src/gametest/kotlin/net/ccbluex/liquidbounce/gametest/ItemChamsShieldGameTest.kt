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

import com.mojang.authlib.GameProfile
import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.features.global.viewmodel.GlobalSettingsViewModel
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleItemChams
import net.ccbluex.liquidbounce.utils.world.nextLocalEntityId
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.player.RemotePlayer
import net.minecraft.util.ARGB
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.levelgen.Heightmap
import java.util.UUID

/**
 * Your own shield is tinted by blocking and by the cooldown of an axe hit, the shield of another player by
 * its blocking and by the shield break sound at it, proven by [ModuleItemChams.Shield.stateOf] and by counting the
 * tinted pixels on screenshots. The first person shield also shrinks with the size of the offhand view model.
 */
class ItemChamsShieldGameTest : FabricClientGameTest {

    override fun runTest(context: ClientGameTestContext) {
        context.waitForClient()
        val savedItemChams = context.fromClient { ConfigSystem.serializeValueGroup(ModuleItemChams) }
        val savedViewModel = context.fromClient { ConfigSystem.serializeValueGroup(GlobalSettingsViewModel) }

        try {
            context.worldBuilder().create().use { world ->
                val server = world.server
                val y = server.computeOnServer<Int, RuntimeException> {
                    it.overworld().getHeight(Heightmap.Types.MOTION_BLOCKING, 0, 0)
                }
                server.runCommand("gamerule spawn_mobs false")
                server.runCommand("kill @e[type=!player]")
                server.runCommand("fill -12 $y -12 24 ${y + 10} 12 air")
                server.runCommand("fill -12 ${y - 1} -12 24 ${y - 1} 12 smooth_stone")
                server.runCommand("gamemode creative @a")
                server.runCommand("time set 6000")
                server.runCommand("weather clear")
                server.runCommand("tp @a 0.5 $y 0.5 -90 0")
                world.connection.waitForChunksRender()

                checkOwnShield(context, server)
                checkOtherShield(context, server, y)
            }
        } finally {
            context.onClient {
                ModuleItemChams.enabled = false
                ConfigSystem.deserializeValueGroup(ModuleItemChams, savedItemChams)
                ConfigSystem.deserializeValueGroup(GlobalSettingsViewModel, savedViewModel)
            }
        }
    }

    private fun checkOwnShield(context: ClientGameTestContext, server: TestServerContext) {
        server.runCommand("item replace entity @a weapon.offhand with shield")
        context.waitTicks(SETTLE_TICKS)

        context.onClient {
            setValue(ModuleItemChams, "Shield", "TintMode", to = "Override")
            setValue(ModuleItemChams, "Shield", "Others", to = "true")
            enableValue(ModuleItemChams, "Shield", "States")
            setValue(ModuleItemChams, "Shield", "States", "Blocking", to = "#FF00FF")
            setValue(ModuleItemChams, "Shield", "States", "Disabled", to = "#FF0000")
            ModuleItemChams.Lightmap.enabled = false
            ModuleItemChams.Shield.enabled = true
            ModuleItemChams.enabled = true
        }
        checkOffhandSize(context)

        context.input.holdKey { it.keyUse }
        context.waitTicks(BLOCK_DELAY_TICKS)
        context.checkShieldState(ModuleItemChams.ShieldState.BLOCKING) { checkNotNull(it.player) }
        context.checkShieldTint("shield-self-blocking", ::isTintedMagenta, ::lowerLeft)

        context.input.releaseKey { it.keyUse }
        server.computeOnServer<Unit, RuntimeException> {
            val player = it.playerList.players.first()
            player.cooldowns.addCooldown(player.offhandItem, DISABLE_TICKS)
        }
        context.waitTicks(SETTLE_TICKS)
        context.checkShieldState(ModuleItemChams.ShieldState.DISABLED) { checkNotNull(it.player) }
        context.checkShieldTint("shield-self-disabled", ::isTintedRed, ::lowerLeft)
    }

    /**
     * The idle shield, tinted magenta, shrinks with the offhand size of the view model.
     */
    private fun checkOffhandSize(context: ClientGameTestContext) {
        context.onClient { setValue(ModuleItemChams, "Shield", "Tint", to = "#FF00FF") }
        context.checkShieldState(ModuleItemChams.ShieldState.IDLE) { checkNotNull(it.player) }
        val base = context.countPixels("shield-size-base", ::isTintedMagenta, area = ::lowerLeft)[0]
        check(base > MIN_TINTED_PIXELS) { "Only $base pixels of the idle shield are tinted" }

        context.onClient {
            enableValue(GlobalSettingsViewModel, "OffHand")
            setValue(GlobalSettingsViewModel, "OffHand", "Size", to = "0.5")
        }
        val scaled = context.countPixels("shield-size-half", ::isTintedMagenta, area = ::lowerLeft)[0]
        check(scaled < base * MAX_SCALED_SHARE) { "The half size shield still covers $scaled of $base pixels" }

        context.onClient {
            setValue(GlobalSettingsViewModel, "OffHand", "Enabled", to = "false")
            setValue(ModuleItemChams, "Shield", "Tint", to = "#FFFFFF")
        }
    }

    private fun checkOtherShield(context: ClientGameTestContext, server: TestServerContext, y: Int) {
        val other = context.fromClient { mc ->
            val level = checkNotNull(mc.level)
            ShieldBearer(level).also { other ->
                other.id = level.nextLocalEntityId()
                other.snapTo(3.5, y.toDouble(), 0.5, 90f, 0f)
                other.yHeadRot = 90f
                other.yBodyRot = 90f
                other.setItemSlot(EquipmentSlot.OFFHAND, ItemStack(Items.SHIELD))
                level.addEntity(other)
                other.raiseShield()
            }
        }
        context.waitTicks(BLOCK_DELAY_TICKS)
        context.checkShieldState(ModuleItemChams.ShieldState.BLOCKING) { other }
        context.checkShieldTint("shield-other-blocking", ::isTintedMagenta, ::center)

        server.runCommand("playsound minecraft:item.shield.break player @a 3.5 $y 0.5")
        context.waitTicks(SETTLE_TICKS)
        context.checkShieldState(ModuleItemChams.ShieldState.DISABLED) { other }
        context.checkShieldTint("shield-other-disabled", ::isTintedRed, ::center)

        context.onClient { mc ->
            checkNotNull(mc.level).removeEntity(other.id, Entity.RemovalReason.DISCARDED)
        }
    }

    private fun ClientGameTestContext.checkShieldState(
        expected: ModuleItemChams.ShieldState,
        holder: (Minecraft) -> LivingEntity,
    ) {
        val state = fromClient { ModuleItemChams.Shield.stateOf(holder(it)) }
        check(state == expected) { "The shield is $state instead of $expected" }
    }

    private fun ClientGameTestContext.checkShieldTint(
        name: String,
        predicate: (Int) -> Boolean,
        area: (Float, Float) -> Boolean,
    ) {
        val tinted = countPixels(name, predicate, area = area)[0]
        check(tinted > MIN_TINTED_PIXELS) { "Only $tinted pixels of the shield in $name are tinted" }
    }

    /**
     * The first person offhand, left of the notifications.
     */
    private fun lowerLeft(x: Float, y: Float) = x < 0.5f && y > 0.5f

    private fun center(x: Float, y: Float) = x in 0.3f..0.6f && y in 0.3f..0.8f

    /**
     * A texture tinted magenta: red and blue clearly above green.
     */
    private fun isTintedMagenta(pixel: Int) =
        ARGB.red(pixel) - ARGB.green(pixel) > TINT_MARGIN && ARGB.blue(pixel) - ARGB.green(pixel) > TINT_MARGIN

    /**
     * A texture tinted red, more saturated than any skin tone.
     */
    private fun isTintedRed(pixel: Int) =
        ARGB.red(pixel) - ARGB.green(pixel) > RED_MARGIN && ARGB.red(pixel) - ARGB.blue(pixel) > RED_MARGIN

    /**
     * Another player raising the shield in its offhand, as the server announces it.
     */
    private class ShieldBearer(level: ClientLevel) :
        RemotePlayer(level, GameProfile(UUID.randomUUID(), "ShieldBearer")) {
        fun raiseShield() = setLivingEntityFlag(LIVING_ENTITY_FLAG_IS_USING or LIVING_ENTITY_FLAG_OFF_HAND, true)
    }

    private companion object {
        const val SETTLE_TICKS = 5
        const val BLOCK_DELAY_TICKS = 8
        const val MIN_TINTED_PIXELS = 300
        const val DISABLE_TICKS = 100
        const val TINT_MARGIN = 40
        const val RED_MARGIN = 100
        const val MAX_SCALED_SHARE = 0.6f
    }

}
