/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2024 CCBlueX
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
import net.ccbluex.liquidbounce.event.Listenable
import net.ccbluex.liquidbounce.event.events.KeyEvent
import net.ccbluex.liquidbounce.event.events.WorldChangeEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.client.*
import net.ccbluex.liquidbounce.features.module.modules.combat.*
import net.ccbluex.liquidbounce.features.module.modules.combat.autoarmor.ModuleAutoArmor
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.ModuleKillAura
import net.ccbluex.liquidbounce.features.module.modules.exploit.*
import net.ccbluex.liquidbounce.features.module.modules.exploit.servercrasher.ModuleServerCrasher
import net.ccbluex.liquidbounce.features.module.modules.`fun`.ModuleDankBobbing
import net.ccbluex.liquidbounce.features.module.modules.`fun`.ModuleDerp
import net.ccbluex.liquidbounce.features.module.modules.`fun`.ModuleHandDerp
import net.ccbluex.liquidbounce.features.module.modules.`fun`.ModuleSkinDerp
import net.ccbluex.liquidbounce.features.module.modules.misc.*
import net.ccbluex.liquidbounce.features.module.modules.misc.antibot.ModuleAntiBot
import net.ccbluex.liquidbounce.features.module.modules.movement.*
import net.ccbluex.liquidbounce.features.module.modules.movement.autododge.ModuleAutoDodge
import net.ccbluex.liquidbounce.features.module.modules.movement.fly.ModuleFly
import net.ccbluex.liquidbounce.features.module.modules.movement.highjump.ModuleHighJump
import net.ccbluex.liquidbounce.features.module.modules.movement.liquidwalk.ModuleLiquidWalk
import net.ccbluex.liquidbounce.features.module.modules.movement.longjump.ModuleLongJump
import net.ccbluex.liquidbounce.features.module.modules.movement.speed.ModuleSpeed
import net.ccbluex.liquidbounce.features.module.modules.movement.step.ModuleReverseStep
import net.ccbluex.liquidbounce.features.module.modules.movement.step.ModuleStep
import net.ccbluex.liquidbounce.features.module.modules.movement.terrainspeed.ModuleTerrainSpeed
import net.ccbluex.liquidbounce.features.module.modules.movement.ModuleVehicleControl
import net.ccbluex.liquidbounce.features.module.modules.player.*
import net.ccbluex.liquidbounce.features.module.modules.player.autoplay.ModuleAutoPlay
import net.ccbluex.liquidbounce.features.module.modules.player.autoshop.ModuleAutoShop
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.ModuleInventoryCleaner
import net.ccbluex.liquidbounce.features.module.modules.player.nofall.ModuleNoFall
import net.ccbluex.liquidbounce.features.module.modules.render.*
import net.ccbluex.liquidbounce.features.module.modules.render.minimap.ModuleMinimap
import net.ccbluex.liquidbounce.features.module.modules.render.nametags.ModuleNametags
import net.ccbluex.liquidbounce.features.module.modules.world.*
import net.ccbluex.liquidbounce.features.module.modules.world.autoFarm.ModuleAutoFarm
import net.ccbluex.liquidbounce.features.module.modules.combat.crystalAura.ModuleCrystalAura
import net.ccbluex.liquidbounce.features.module.modules.render.murdermystery.ModuleMurderMystery
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold
import net.ccbluex.liquidbounce.script.ScriptApi
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
            filter { it.bind == ev.key.keyCode } // modules bound to a specific key
                .forEach { it.enabled = !it.enabled } // toggle modules
        }
    }

    val worldHandler = handler<WorldChangeEvent> {
        ConfigSystem.storeConfigurable(modulesConfigurable)
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
            ModuleAutoHead,
            ModuleAutoWeapon,
            ModuleFakeLag,
            ModuleCriticals,
            ModuleHitbox,
            ModuleKillAura,
            ModuleSuperKnockback,
            ModuleTimerRange,
            ModuleVelocity,
            ModuleBacktrack,
            ModuleSwordBlock,
            ModuleAutoShoot,
            ModuleKeepSprint,

            // Exploit
            ModuleAbortBreaking,
            ModuleAntiReducedDebugInfo,
            ModuleAntiHunger,
            ModuleClip,
            ModuleDamage,
            ModuleDisabler,
            ModuleForceUnicodeChat,
            ModuleGhostHand,
            ModuleKick,
            ModuleMoreCarry,
            ModuleNameCollector,
            ModuleNoPitchLimit,
            ModulePingSpoof,
            ModulePlugins,
            ModulePortalMenu,
            ModuleResourceSpoof,
            ModuleSleepWalker,
            ModuleSpoofer,
            ModuleVehicleOneHit,
            ModuleServerCrasher,
            ModuleSwingFix,

            // Fun
            ModuleDankBobbing,
            ModuleDerp,
            ModuleSkinDerp,
            ModuleHandDerp,

            // Misc
            ModuleAntiBot,
            ModuleFriendClicker,
            ModuleKeepChatAfterDeath,
            ModuleNameProtect,
            ModuleNotifier,
            ModuleSpammer,
            ModuleAutoAccount,
            ModuleTeams,
            ModuleAutoChatGame,
            ModuleDebugRecorder,
            ModuleFocus,
            ModuleAntiStaff,

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
            ModuleReverseStep,
            ModuleStrafe,
            ModuleTerrainSpeed,
            ModuleVehicleControl,

            // Player
            ModuleAntiAFK,
            ModuleAntiExploit,
            ModuleAutoBreak,
            ModuleAutoFish,
            ModuleAutoPlay,
            ModuleAutoRespawn,
            ModuleAutoTotem,
            ModuleAutoShop,
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
            ModuleAnimations,
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
            ModuleNametags,
            ModuleCombineMobs,

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
            ModuleNuker,

            // Client
            ModuleAutoConfig,
            ModuleRichPresence,
            ModuleCapeTransfer,
            ModuleEnemies,
            ModuleLiquidChat
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

    fun removeModule(module: Module) {
        if (module.enabled) {
            module.disable()
        }
        module.unregister()
        modules -= module
    }

    /**
     * Allow `ModuleManager += Module` syntax
     */
    operator fun plusAssign(module: Module) {
        addModule(module)
    }

    operator fun plusAssign(modules: MutableList<Module>) {
        modules.forEach(this::addModule)
    }

    operator fun minusAssign(module: Module) {
        removeModule(module)
    }

    operator fun minusAssign(modules: MutableList<Module>) {
        modules.forEach(this::removeModule)
    }

    fun autoComplete(begin: String, args: List<String>, validator: (Module) -> Boolean = { true }): List<String> {
        return filter { it.name.startsWith(begin, true) && validator(it) }.map { it.name }
    }

    /**
     * This is being used by UltralightJS for the implementation of the ClickGUI. DO NOT REMOVE!
     */
    @JvmName("getCategories")
    @ScriptApi
    fun getCategories() = Category.values().map { it.readableName }.toTypedArray()

    @JvmName("getModules")
    fun getModules() = modules

    @JvmName("getModuleByName")
    @ScriptApi
    fun getModuleByName(module: String) = find { it.name.equals(module, true) }

    operator fun get(moduleName: String) = modules.find { it.name.equals(moduleName, true) }

}
