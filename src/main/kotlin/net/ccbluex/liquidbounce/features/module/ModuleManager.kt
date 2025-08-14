/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2025 CCBlueX
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
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.events.DisconnectEvent
import net.ccbluex.liquidbounce.event.events.KeyboardKeyEvent
import net.ccbluex.liquidbounce.event.events.MouseButtonEvent
import net.ccbluex.liquidbounce.event.events.WorldChangeEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.client.*
import net.ccbluex.liquidbounce.features.module.modules.combat.*
import net.ccbluex.liquidbounce.features.module.modules.combat.aimbot.ModuleAutoBow
import net.ccbluex.liquidbounce.features.module.modules.combat.autoarmor.ModuleAutoArmor
import net.ccbluex.liquidbounce.features.module.modules.combat.backtrack.ModuleBacktrack
import net.ccbluex.liquidbounce.features.module.modules.combat.criticals.ModuleCriticals
import net.ccbluex.liquidbounce.features.module.modules.combat.crystalaura.ModuleCrystalAura
import net.ccbluex.liquidbounce.features.module.modules.combat.elytratarget.ModuleElytraTarget
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.ModuleKillAura
import net.ccbluex.liquidbounce.features.module.modules.combat.tpaura.ModuleTpAura
import net.ccbluex.liquidbounce.features.module.modules.combat.velocity.ModuleVelocity
import net.ccbluex.liquidbounce.features.module.modules.exploit.*
import net.ccbluex.liquidbounce.features.module.modules.exploit.disabler.ModuleDisabler
import net.ccbluex.liquidbounce.features.module.modules.exploit.dupe.ModuleDupe
import net.ccbluex.liquidbounce.features.module.modules.exploit.phase.ModulePhase
import net.ccbluex.liquidbounce.features.module.modules.exploit.servercrasher.ModuleServerCrasher
import net.ccbluex.liquidbounce.features.module.modules.`fun`.*
import net.ccbluex.liquidbounce.features.module.modules.misc.*
import net.ccbluex.liquidbounce.features.module.modules.misc.antibot.ModuleAntiBot
import net.ccbluex.liquidbounce.features.module.modules.misc.betterchat.ModuleBetterChat
import net.ccbluex.liquidbounce.features.module.modules.misc.debugrecorder.ModuleDebugRecorder
import net.ccbluex.liquidbounce.features.module.modules.misc.nameprotect.ModuleNameProtect
import net.ccbluex.liquidbounce.features.module.modules.movement.*
import net.ccbluex.liquidbounce.features.module.modules.movement.autododge.ModuleAutoDodge
import net.ccbluex.liquidbounce.features.module.modules.movement.elytrafly.ModuleElytraFly
import net.ccbluex.liquidbounce.features.module.modules.movement.fly.ModuleFly
import net.ccbluex.liquidbounce.features.module.modules.movement.highjump.ModuleHighJump
import net.ccbluex.liquidbounce.features.module.modules.movement.inventorymove.ModuleInventoryMove
import net.ccbluex.liquidbounce.features.module.modules.movement.liquidwalk.ModuleLiquidWalk
import net.ccbluex.liquidbounce.features.module.modules.movement.longjump.ModuleLongJump
import net.ccbluex.liquidbounce.features.module.modules.movement.noslow.ModuleNoSlow
import net.ccbluex.liquidbounce.features.module.modules.movement.speed.ModuleSpeed
import net.ccbluex.liquidbounce.features.module.modules.movement.spider.ModuleSpider
import net.ccbluex.liquidbounce.features.module.modules.movement.step.ModuleReverseStep
import net.ccbluex.liquidbounce.features.module.modules.movement.step.ModuleStep
import net.ccbluex.liquidbounce.features.module.modules.movement.terrainspeed.ModuleTerrainSpeed
import net.ccbluex.liquidbounce.features.module.modules.player.*
import net.ccbluex.liquidbounce.features.module.modules.player.antivoid.ModuleAntiVoid
import net.ccbluex.liquidbounce.features.module.modules.player.autobuff.ModuleAutoBuff
import net.ccbluex.liquidbounce.features.module.modules.player.autoqueue.ModuleAutoQueue
import net.ccbluex.liquidbounce.features.module.modules.player.autoshop.ModuleAutoShop
import net.ccbluex.liquidbounce.features.module.modules.player.cheststealer.ModuleChestStealer
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.ModuleInventoryCleaner
import net.ccbluex.liquidbounce.features.module.modules.player.nofall.ModuleNoFall
import net.ccbluex.liquidbounce.features.module.modules.player.offhand.ModuleOffhand
import net.ccbluex.liquidbounce.features.module.modules.render.*
import net.ccbluex.liquidbounce.features.module.modules.render.esp.ModuleESP
import net.ccbluex.liquidbounce.features.module.modules.render.murdermystery.ModuleMurderMystery
import net.ccbluex.liquidbounce.features.module.modules.render.nametags.ModuleNametags
import net.ccbluex.liquidbounce.features.module.modules.render.trajectories.ModuleTrajectories
import net.ccbluex.liquidbounce.features.module.modules.world.*
import net.ccbluex.liquidbounce.features.module.modules.world.autobuild.ModuleAutoBuild
import net.ccbluex.liquidbounce.features.module.modules.world.autofarm.ModuleAutoFarm
import net.ccbluex.liquidbounce.features.module.modules.world.fucker.ModuleFucker
import net.ccbluex.liquidbounce.features.module.modules.world.nuker.ModuleNuker
import net.ccbluex.liquidbounce.features.module.modules.world.packetmine.ModulePacketMine
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold
import net.ccbluex.liquidbounce.features.module.modules.world.traps.ModuleAutoTrap
import net.ccbluex.liquidbounce.script.ScriptApiRequired
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.input.InputBind
import net.ccbluex.liquidbounce.utils.kotlin.mapArray
import net.ccbluex.liquidbounce.utils.kotlin.sortedInsert
import org.lwjgl.glfw.GLFW

/**
 * Should be sorted by Module::name
 */
private val modules = ArrayList<ClientModule>(256)

/**
 * A fairly simple module manager
 */
object ModuleManager : EventListener, Iterable<ClientModule> by modules {

    val modulesConfigurable = ConfigSystem.root("modules", modules)

    /**
     * Handles keystrokes for module binds.
     * This also runs in GUIs, so that if a GUI is opened while a key is pressed,
     * any modules that need to be disabled on key release will be properly disabled.
     */
    @Suppress("unused")
    private val keyboardKeyHandler = handler<KeyboardKeyEvent> { event ->
        when (event.action) {
            GLFW.GLFW_PRESS -> if (mc.currentScreen == null) {
                    filter { m -> m.bind.matchesKey(event.keyCode, event.scanCode) }
                    .forEach { m ->
                        m.enabled = !m.enabled || m.bind.action == InputBind.BindAction.HOLD
                    }
                }
            GLFW.GLFW_RELEASE ->
                filter { m ->
                    m.bind.matchesKey(event.keyCode, event.scanCode) &&
                        m.bind.action == InputBind.BindAction.HOLD
                }.forEach { m ->
                    m.enabled = false
                }
        }
    }

    @Suppress("unused")
    private val mouseButtonHandler = handler<MouseButtonEvent> { event ->
        when (event.action) {
            GLFW.GLFW_PRESS -> if (mc.currentScreen == null) {
                filter { m -> m.bind.matchesMouse(event.button) }
                    .forEach { m ->
                        m.enabled = !m.running || m.bind.action == InputBind.BindAction.HOLD
                    }
            }
            GLFW.GLFW_RELEASE ->
                filter { m ->
                    m.bind.matchesMouse(event.button) && m.bind.action == InputBind.BindAction.HOLD
                }.forEach { m -> m.enabled = false }
        }
    }

    /**
     * Handles world change and enables modules that are not enabled yet
     */
    @Suppress("unused")
    private val handleWorldChange = handler<WorldChangeEvent> { event ->
        // Delayed start handling
        if (event.world != null) {
            for (module in modules) {
                if (!module.enabled || module.calledSinceStartup) continue

                try {
                    module.calledSinceStartup = true
                    module.onEnabled()
                } catch (e: Exception) {
                    logger.error("Failed to enable module ${module.name}", e)
                }
            }
        }

        // Store modules configuration after world change, happens on disconnect as well
        ConfigSystem.storeConfigurable(modulesConfigurable)
    }

    /**
     * Handles disconnect and if [Module.disableOnQuit] is true disables module
     */
    @Suppress("unused")
    private val handleDisconnect = handler<DisconnectEvent> {
        for (module in modules) {
            if (module.disableOnQuit) {
                try {
                    module.enabled = false
                } catch (e: Exception) {
                    logger.error("Failed to disable module ${module.name}", e)
                }
            }
        }
    }

    /**
     * Register inbuilt client modules
     */
    @Suppress("LongMethod")
    fun registerInbuilt() {
        val builtin = arrayOf(
            // Combat
            ModuleAimbot,
            ModuleAutoArmor,
            ModuleAutoBow,
            ModuleAutoClicker,
            ModuleAutoLeave,
            ModuleAutoBuff,
            ModuleAutoWeapon,
            ModuleFakeLag,
            ModuleCriticals,
            ModuleHitbox,
            ModuleKillAura,
            ModuleTpAura,
            ModuleSuperKnockback,
            ModuleTimerRange,
            ModuleTickBase,
            ModuleVelocity,
            ModuleBacktrack,
            ModuleSwordBlock,
            ModuleAutoShoot,
            ModuleKeepSprint,
            ModuleMaceKill,
            ModuleNoMissCooldown,

            // Exploit
            ModuleAbortBreaking,
            ModuleAntiReducedDebugInfo,
            ModuleAntiHunger,
            ModuleClip,
            ModuleExtendedFirework,
            ModuleResetVL,
            ModuleDamage,
            ModuleDisabler,
            ModuleGhostHand,
            ModuleKick,
            ModuleMoreCarry,
            ModuleMultiActions,
            ModuleNameCollector,
            ModuleNoPitchLimit,
            ModulePingSpoof,
            ModulePlugins,
            ModulePortalMenu,
            ModuleSleepWalker,
            ModuleVehicleOneHit,
            ModuleServerCrasher,
            ModuleDupe,
            ModuleClickTp,
            ModuleTimeShift,
            ModuleTeleport,
            ModulePhase,

            // Fun
            ModuleDankBobbing,
            ModuleDerp,
            ModuleSkinDerp,
            ModuleHandDerp,
            ModuleTwerk,
            ModuleVomit,

            // Misc
            ModuleBookBot,
            ModuleAntiBot,
            ModuleBetterTab,
            ModuleItemScroller,
            ModuleBetterChat,
            ModuleElytraTarget,
            ModuleMiddleClickAction,
            ModuleInventoryTracker,
            ModuleNameProtect,
            ModuleNotifier,
            ModuleSpammer,
            ModuleAutoAccount,
            ModuleTeams,
            ModuleElytraSwap,
            ModuleAutoChatGame,
            ModuleTargetLock,
            ModuleAutoPearl,
            ModuleAntiStaff,
            ModuleFlagCheck,
            ModulePacketLogger,
            ModuleDebugRecorder,
            ModuleAntiCheatDetect,
            ModuleEasyPearl,

            // Movement
            ModuleAirJump,
            ModuleAntiBounce,
            ModuleAntiLevitation,
            ModuleAutoDodge,
            ModuleAvoidHazards,
            ModuleBlockBounce,
            ModuleBlockWalk,
            ModuleElytraRecast,
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
            ModuleEntityControl,
            ModuleSafeWalk,
            ModuleSneak,
            ModuleSpeed,
            ModuleSprint,
            ModuleStep,
            ModuleReverseStep,
            ModuleStrafe,
            ModuleTerrainSpeed,
            ModuleVehicleBoost,
            ModuleVehicleControl,
            ModuleSpider,
            ModuleTargetStrafe,
            ModuleAnchor,

            // Player
            ModuleAntiVoid,
            ModuleAntiAFK,
            ModuleAntiExploit,
            ModuleAutoBreak,
            ModuleAutoFish,
            ModuleAutoRespawn,
            ModuleOffhand,
            ModuleAutoShop,
            ModuleAutoWalk,
            ModuleBlink,
            ModuleChestStealer,
            ModuleEagle,
            ModuleFastExp,
            ModuleFastUse,
            ModuleInventoryCleaner,
            ModuleNoFall,
            ModuleNoRotateSet,
            ModuleReach,
            ModuleAutoQueue,
            ModuleSmartEat,
            ModuleReplenish,
            ModulePotionSpoof,

            // Render
            ModuleAnimations,
            ModuleAntiBlind,
            ModuleBlockESP,
            ModuleBlockOutline,
            ModuleBreadcrumbs,
            ModuleCameraClip,
            ModuleClickGui,
            ModuleDamageParticles,
            ModuleParticles,
            ModuleESP,
            ModuleLogoffSpot,
            ModuleFreeCam,
            ModuleFreeLook,
            ModuleFullBright,
            ModuleHoleESP,
            ModuleHud,
            ModuleItemESP,
            ModuleItemTags,
            ModuleJumpEffect,
            ModuleMobOwners,
            ModuleMurderMystery,
            ModuleAttackEffects,
            ModuleNametags,
            ModuleCombineMobs,
            ModuleAspect,
            ModuleAutoF5,
            ModuleChams,
            ModuleBedPlates,
            ModuleNoBob,
            ModuleNoFov,
            ModuleNoHurtCam,
            ModuleNoSignRender,
            ModuleNoSwing,
            ModuleCustomAmbience,
            ModuleProphuntESP,
            ModuleQuickPerspectiveSwap,
            ModuleRotations,
            ModuleSilentHotbar,
            ModuleStorageESP,
            ModuleTNTTimer,
            ModuleTracers,
            ModuleTrajectories,
            ModuleTrueSight,
            ModuleVoidESP,
            ModuleXRay,
            ModuleDebug,
            ModuleZoom,
            ModuleItemChams,
            ModuleCrystalView,
            ModuleSkinChanger,

            // World
            ModuleAutoBuild,
            ModuleAutoDisable,
            ModuleAutoFarm,
            ModuleAutoTool,
            ModuleCrystalAura,
            ModuleFastBreak,
            ModuleFastPlace,
            ModuleFucker,
            ModuleAutoTrap,
            ModuleBlockTrap,
            ModuleNoSlowBreak,
            ModuleLiquidPlace,
            ModuleProjectilePuncher,
            ModuleScaffold,
            ModuleTimer,
            ModuleNuker,
            ModuleExtinguish,
            ModuleBedDefender,
            ModuleBlockIn,
            ModuleSurround,
            ModulePacketMine,
            ModuleHoleFiller,

            // Client
            ModuleAutoConfig,
            ModuleRichPresence,
            ModuleTargets,
            ModuleTranslation,
            ModuleLiquidChat
        )

        builtin.forEach { module ->
            addModule(module)
            module.walkKeyPath()
            module.verifyFallbackDescription()
        }
    }

    fun addModule(module: ClientModule) {
        module.initConfigurable()
        module.onRegistration()
        modules.sortedInsert(module, ClientModule::name)
    }

    fun removeModule(module: ClientModule) {
        if (module.running) {
            module.onDisabled()
        }
        module.unregister()
        modules -= module
    }

    fun clear() {
        modules.clear()
    }

    /**
     * This is being used by UltralightJS for the implementation of the ClickGUI. DO NOT REMOVE!
     */
    @JvmName("getCategories")
    @ScriptApiRequired
    fun getCategories() = Category.entries.mapArray { it.readableName }

    @JvmName("getModules")
    @ScriptApiRequired
    fun getModules(): Iterable<ClientModule> = modules

    @JvmName("getModuleByName")
    @ScriptApiRequired
    fun getModuleByName(module: String) = find { it.name.equals(module, true) }

    operator fun get(moduleName: String) = modules.find { it.name.equals(moduleName, true) }

}
