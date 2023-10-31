/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2023 CCBlueX
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
package net.ccbluex.liquidbounce.features.module

import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.event.KeyEvent
import net.ccbluex.liquidbounce.event.Listenable
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.combat.*
import net.ccbluex.liquidbounce.features.module.modules.exploit.*
import net.ccbluex.liquidbounce.features.module.modules.`fun`.ModuleDankBobbing
import net.ccbluex.liquidbounce.features.module.modules.`fun`.ModuleDerp
import net.ccbluex.liquidbounce.features.module.modules.`fun`.ModuleSkinDerp
import net.ccbluex.liquidbounce.features.module.modules.misc.*
import net.ccbluex.liquidbounce.features.module.modules.movement.*
import net.ccbluex.liquidbounce.features.module.modules.movement.autododge.ModuleAutoDodge
import net.ccbluex.liquidbounce.features.module.modules.player.*
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.ModuleInventoryCleaner
import net.ccbluex.liquidbounce.features.module.modules.render.*
import net.ccbluex.liquidbounce.features.module.modules.render.minimap.ModuleMinimap
import net.ccbluex.liquidbounce.features.module.modules.world.*
import net.ccbluex.liquidbounce.features.module.modules.world.crystalAura.ModuleCrystalAura
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold
import org.lwjgl.glfw.GLFW

private val modules = mutableListOf<Module>()

/**
 * A fairly simple module manager
 */
object ModuleManager : Listenable, Iterable<Module> by modules {

    val modulesConfigurable = ConfigSystem.root("modules", modules)

    /**
     * Handle key input for module binds
     */
    val keyHandler = handler<KeyEvent> { ev ->
        if (ev.action == GLFW.GLFW_PRESS) {
            filter { it.bind == ev.key.code } // modules bound to a specific key
                .forEach { it.enabled = !it.enabled } // toggle modules
        }
    }

    /**
     * Register inbuilt client modules
     */
    fun registerInbuilt() {
        val builtin = arrayOf(
            // Combat
            ModuleAimbot,
            ModuleAutoArmor,
            ModuleAutoBow,
            ModuleAutoClicker,
            ModuleAutoGapple,
            ModuleAutoLeave,
            ModuleAutoPot,
            ModuleAutoSoup,
            ModuleAutoWeapon,
            ModuleBadWifi,
            ModuleCriticals,
            ModuleHitbox,
            ModuleKillAura,
            ModulePerfectHit,
            ModuleSuperKnockback,
            ModuleTickBase,
            ModuleTimerRange,
            ModuleTrigger,
            ModuleVelocity,

            // Exploit
            ModuleAbortBreaking,
            ModuleAntiReducedDebugInfo,
            ModuleAntiVanish,
            ModuleClip,
            ModuleDamage,
            ModuleDisabler,
            ModuleForceUnicodeChat,
            ModuleGhostHand,
            ModuleGodMode,
            ModuleKick,
            ModuleMoreCarry,
            ModuleNameCollector,
            ModuleNoPitchLimit,
            ModulePingSpoof,
            ModulePlugins,
            ModulePortalMenu,
            ModuleResourceSpoof,
            ModuleSleepWalker,
            ModuleVehicleOneHit,

            // Fun
            ModuleDankBobbing,
            ModuleDerp,
            ModuleSkinDerp,

            // Misc
            ModuleAntiBot,
            ModuleClickRecorder,
            ModuleFriendClicker,
            ModuleKeepChatAfterDeath,
            ModuleNameProtect,
            ModuleNotifier,
            ModuleSpammer,
            ModuleTeams,
            ModuleAutoChatGame,
            ModuleDebugRecorder,
            ModuleCapeTransfer,

            // Movement
            ModuleAirJump,
            ModuleAntiLevitation,
            ModuleAutoDodge,
            ModuleAvoidHazards,
            ModuleBlockBounce,
            ModuleBlockWalk,
            ModuleBugUp,
            ModuleElytraFly,
            ModuleFly,
            ModuleFreeze,
            ModuleHighJump,
            ModuleInventoryMove,
            ModuleLiquidWalk,
            ModuleLongJump,
            ModuleNoClip,
            ModuleNoJumpDelay,
            ModuleNoPush,
            ModuleNoSlow,
            ModuleNoWeb,
            ModuleParkour,
            ModulePerfectHorseJump,
            ModuleSafeWalk,
            ModuleSneak,
            ModuleSpeed,
            ModuleSprint,
            ModuleStep,
            ModuleStrafe,
            ModuleTerrainSpeed,
            ModuleVehicleFly,

            // Player
            ModuleAntiAFK,
            ModuleAntiExploit,
            ModuleAutoBreak,
            ModuleAutoFish,
            ModuleAutoRespawn,
            ModuleAutoTotem,
            ModuleAutoWalk,
            ModuleBlink,
            ModuleChestStealer,
            ModuleEagle,
            ModuleFastUse,
            ModuleInventoryCleaner,
            ModuleNoFall,
            ModuleNoRotateSet,
            ModuleReach,
            ModuleRegen,
            ModuleZoot,

            // Render
            ModuleAnimation,
            ModuleAntiBlind,
            ModuleBlockESP,
            ModuleBreadcrumbs,
            ModuleCameraClip,
            ModuleClickGui,
            ModuleESP,
            ModuleFreeCam,
            ModuleFullBright,
            ModuleHoleESP,
            ModuleHud,
            ModuleItemESP,
            ModuleJumpEffect,
            ModuleMobOwners,
            ModuleMurderMystery,
            ModuleAttackEffects,

            // ModuleNametags,
            ModuleNoBob,
            ModuleNoFov,
            ModuleNoHurtCam,
            ModuleNoSignRender,
            ModuleNoSwing,
            ModuleOverrideTime,
            ModuleOverrideWeather,
            ModuleQuickPerspectiveSwap,
            ModuleRotations,
            ModuleStorageESP,
            ModuleTracers,
            ModuleTrajectories,
            ModuleTrueSight,
            ModuleXRay,
            ModuleDebug,
            ModuleMinimap,
            ModuleScoreboard,

            // World
            ModuleAutoDisable,
            ModuleAutoFarm,
            ModuleAutoTool,
            ModuleChestAura,
            ModuleCrystalAura,
            ModuleFastBreak,
            ModuleFastPlace,
            ModuleFucker,
            ModuleIgnite,
            ModuleNoSlowBreak,
            ModuleProjectilePuncher,
            ModuleScaffold,
            ModuleTimer,
            ModuleNuker
        )

        builtin.apply {
            sortBy { it.name }
            forEach(::addModule)
        }
    }

    fun addModule(module: Module) {
        module.initConfigurable()
        module.init()
        modules += module
    }

    /**
     * Allow `ModuleManager += Module` syntax
     */
    operator fun plusAssign(module: Module) {
        addModule(module)
    }

    fun autoComplete(begin: String, validator: (Module) -> Boolean = { true }): List<String> {
        return filter { it.name.startsWith(begin, true) && validator(it) }.map { it.name }
    }

    /**
     * This is being used by UltralightJS for the implementation of the ClickGUI. DO NOT REMOVE!
     */
    fun getCategories() = Category.values().map { it.readableName }.toTypedArray()

}
