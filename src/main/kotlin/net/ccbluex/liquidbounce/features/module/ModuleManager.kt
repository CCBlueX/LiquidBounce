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
package net.ccbluex.liquidbounce.features.module

import it.unimi.dsi.fastutil.objects.ObjectRBTreeSet
import net.ccbluex.fastutil.mapToArray
import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.config.autoconfig.AutoConfig
import net.ccbluex.liquidbounce.config.types.VALUE_NAME_ORDER
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.events.DisconnectEvent
import net.ccbluex.liquidbounce.event.events.KeyboardKeyEvent
import net.ccbluex.liquidbounce.event.events.MouseButtonEvent
import net.ccbluex.liquidbounce.event.events.WorldChangeEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.sequenceHandler
import net.ccbluex.liquidbounce.event.tickUntil
import net.ccbluex.liquidbounce.features.module.modules.combat.ModuleAimbot
import net.ccbluex.liquidbounce.features.module.modules.combat.ModuleAutoClicker
import net.ccbluex.liquidbounce.features.module.modules.combat.ModuleAutoLeave
import net.ccbluex.liquidbounce.features.module.modules.combat.ModuleAutoRod
import net.ccbluex.liquidbounce.features.module.modules.combat.ModuleAutoShoot
import net.ccbluex.liquidbounce.features.module.modules.combat.ModuleAutoWeapon
import net.ccbluex.liquidbounce.features.module.modules.combat.ModuleFakeLag
import net.ccbluex.liquidbounce.features.module.modules.combat.ModuleHitbox
import net.ccbluex.liquidbounce.features.module.modules.combat.ModuleKeepSprint
import net.ccbluex.liquidbounce.features.module.modules.combat.ModuleMaceKill
import net.ccbluex.liquidbounce.features.module.modules.combat.ModuleNoMissCooldown
import net.ccbluex.liquidbounce.features.module.modules.combat.ModuleSuperKnockback
import net.ccbluex.liquidbounce.features.module.modules.combat.ModuleSwordBlock
import net.ccbluex.liquidbounce.features.module.modules.combat.ModuleTickBase
import net.ccbluex.liquidbounce.features.module.modules.combat.ModuleTimerRange
import net.ccbluex.liquidbounce.features.module.modules.combat.aimbot.ModuleAutoBow
import net.ccbluex.liquidbounce.features.module.modules.combat.autoarmor.ModuleAutoArmor
import net.ccbluex.liquidbounce.features.module.modules.combat.backtrack.ModuleBacktrack
import net.ccbluex.liquidbounce.features.module.modules.combat.criticals.ModuleCriticals
import net.ccbluex.liquidbounce.features.module.modules.combat.crystalaura.ModuleCrystalAura
import net.ccbluex.liquidbounce.features.module.modules.combat.elytratarget.ModuleElytraTarget
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.ModuleKillAura
import net.ccbluex.liquidbounce.features.module.modules.combat.tpaura.ModuleTpAura
import net.ccbluex.liquidbounce.features.module.modules.combat.velocity.ModuleVelocity
import net.ccbluex.liquidbounce.features.module.modules.exploit.ModuleAbortBreaking
import net.ccbluex.liquidbounce.features.module.modules.exploit.ModuleAntiHunger
import net.ccbluex.liquidbounce.features.module.modules.exploit.ModuleAntiReducedDebugInfo
import net.ccbluex.liquidbounce.features.module.modules.exploit.ModuleClickTp
import net.ccbluex.liquidbounce.features.module.modules.exploit.ModuleClip
import net.ccbluex.liquidbounce.features.module.modules.exploit.ModuleDamage
import net.ccbluex.liquidbounce.features.module.modules.exploit.ModuleExtendedFirework
import net.ccbluex.liquidbounce.features.module.modules.exploit.ModuleGhostHand
import net.ccbluex.liquidbounce.features.module.modules.exploit.ModuleKick
import net.ccbluex.liquidbounce.features.module.modules.exploit.ModuleMoreCarry
import net.ccbluex.liquidbounce.features.module.modules.exploit.ModuleMultiActions
import net.ccbluex.liquidbounce.features.module.modules.exploit.ModuleNameCollector
import net.ccbluex.liquidbounce.features.module.modules.exploit.ModuleNoPitchLimit
import net.ccbluex.liquidbounce.features.module.modules.exploit.ModulePingSpoof
import net.ccbluex.liquidbounce.features.module.modules.exploit.ModulePlugins
import net.ccbluex.liquidbounce.features.module.modules.exploit.ModulePortalMenu
import net.ccbluex.liquidbounce.features.module.modules.exploit.ModuleResetVL
import net.ccbluex.liquidbounce.features.module.modules.exploit.ModuleSleepWalker
import net.ccbluex.liquidbounce.features.module.modules.exploit.ModuleTimeShift
import net.ccbluex.liquidbounce.features.module.modules.exploit.ModuleVehicleOneHit
import net.ccbluex.liquidbounce.features.module.modules.exploit.ModuleYggdrasilSignatureFix
import net.ccbluex.liquidbounce.features.module.modules.exploit.disabler.ModuleDisabler
import net.ccbluex.liquidbounce.features.module.modules.exploit.dupe.ModuleDupe
import net.ccbluex.liquidbounce.features.module.modules.exploit.phase.ModulePhase
import net.ccbluex.liquidbounce.features.module.modules.exploit.servercrasher.ModuleServerCrasher
import net.ccbluex.liquidbounce.features.module.modules.`fun`.ModuleDankBobbing
import net.ccbluex.liquidbounce.features.module.modules.`fun`.ModuleDerp
import net.ccbluex.liquidbounce.features.module.modules.`fun`.ModuleHandDerp
import net.ccbluex.liquidbounce.features.module.modules.`fun`.ModuleSkinDerp
import net.ccbluex.liquidbounce.features.module.modules.`fun`.ModuleTwerk
import net.ccbluex.liquidbounce.features.module.modules.`fun`.ModuleVomit
import net.ccbluex.liquidbounce.features.module.modules.`fun`.notebot.ModuleNotebot
import net.ccbluex.liquidbounce.features.module.modules.misc.ModuleAntiCheatDetect
import net.ccbluex.liquidbounce.features.module.modules.misc.ModuleAntiStaff
import net.ccbluex.liquidbounce.features.module.modules.misc.ModuleAutoAccount
import net.ccbluex.liquidbounce.features.module.modules.misc.ModuleAutoChatGame
import net.ccbluex.liquidbounce.features.module.modules.misc.ModuleAutoConfig
import net.ccbluex.liquidbounce.features.module.modules.misc.ModuleAutoPearl
import net.ccbluex.liquidbounce.features.module.modules.misc.ModuleBetterTab
import net.ccbluex.liquidbounce.features.module.modules.misc.ModuleBookBot
import net.ccbluex.liquidbounce.features.module.modules.misc.ModuleEasyPearl
import net.ccbluex.liquidbounce.features.module.modules.misc.ModuleElytraSwap
import net.ccbluex.liquidbounce.features.module.modules.misc.ModuleFlagCheck
import net.ccbluex.liquidbounce.features.module.modules.misc.ModuleGUICloser
import net.ccbluex.liquidbounce.features.module.modules.misc.ModuleInventoryTracker
import net.ccbluex.liquidbounce.features.module.modules.misc.ModuleItemScroller
import net.ccbluex.liquidbounce.features.module.modules.misc.ModuleMacros
import net.ccbluex.liquidbounce.features.module.modules.misc.ModuleMiddleClickAction
import net.ccbluex.liquidbounce.features.module.modules.misc.ModuleNotifier
import net.ccbluex.liquidbounce.features.module.modules.misc.ModulePacketLogger
import net.ccbluex.liquidbounce.features.module.modules.misc.ModuleSpammer
import net.ccbluex.liquidbounce.features.module.modules.misc.ModuleTargetLock
import net.ccbluex.liquidbounce.features.module.modules.misc.ModuleTeams
import net.ccbluex.liquidbounce.features.module.modules.misc.ModuleTextFieldProtect
import net.ccbluex.liquidbounce.features.module.modules.misc.antibot.ModuleAntiBot
import net.ccbluex.liquidbounce.features.module.modules.misc.betterchat.ModuleBetterChat
import net.ccbluex.liquidbounce.features.module.modules.misc.debugrecorder.ModuleDebugRecorder
import net.ccbluex.liquidbounce.features.module.modules.misc.nameprotect.ModuleNameProtect
import net.ccbluex.liquidbounce.features.module.modules.misc.reporthelper.ModuleReportHelper
import net.ccbluex.liquidbounce.features.module.modules.movement.ModuleAirJump
import net.ccbluex.liquidbounce.features.module.modules.movement.ModuleAnchor
import net.ccbluex.liquidbounce.features.module.modules.movement.ModuleAntiBounce
import net.ccbluex.liquidbounce.features.module.modules.movement.ModuleAntiLevitation
import net.ccbluex.liquidbounce.features.module.modules.movement.ModuleAvoidHazards
import net.ccbluex.liquidbounce.features.module.modules.movement.ModuleBlockBounce
import net.ccbluex.liquidbounce.features.module.modules.movement.ModuleBlockWalk
import net.ccbluex.liquidbounce.features.module.modules.movement.ModuleElytraRecast
import net.ccbluex.liquidbounce.features.module.modules.movement.ModuleEntityControl
import net.ccbluex.liquidbounce.features.module.modules.movement.ModuleFreeze
import net.ccbluex.liquidbounce.features.module.modules.movement.ModuleNoClip
import net.ccbluex.liquidbounce.features.module.modules.movement.ModuleNoJumpDelay
import net.ccbluex.liquidbounce.features.module.modules.movement.ModuleNoPose
import net.ccbluex.liquidbounce.features.module.modules.movement.ModuleNoPush
import net.ccbluex.liquidbounce.features.module.modules.movement.ModuleParkour
import net.ccbluex.liquidbounce.features.module.modules.movement.ModuleSafeWalk
import net.ccbluex.liquidbounce.features.module.modules.movement.ModuleSneak
import net.ccbluex.liquidbounce.features.module.modules.movement.ModuleSprint
import net.ccbluex.liquidbounce.features.module.modules.movement.ModuleStrafe
import net.ccbluex.liquidbounce.features.module.modules.movement.ModuleTargetStrafe
import net.ccbluex.liquidbounce.features.module.modules.movement.ModuleTeleport
import net.ccbluex.liquidbounce.features.module.modules.movement.ModuleVehicleBoost
import net.ccbluex.liquidbounce.features.module.modules.movement.ModuleVehicleControl
import net.ccbluex.liquidbounce.features.module.modules.movement.autododge.ModuleAutoDodge
import net.ccbluex.liquidbounce.features.module.modules.movement.elytrafly.ModuleElytraFly
import net.ccbluex.liquidbounce.features.module.modules.movement.fly.ModuleFly
import net.ccbluex.liquidbounce.features.module.modules.movement.highjump.ModuleHighJump
import net.ccbluex.liquidbounce.features.module.modules.movement.inventorymove.ModuleInventoryMove
import net.ccbluex.liquidbounce.features.module.modules.movement.liquidwalk.ModuleLiquidWalk
import net.ccbluex.liquidbounce.features.module.modules.movement.longjump.ModuleLongJump
import net.ccbluex.liquidbounce.features.module.modules.movement.noslow.ModuleNoSlow
import net.ccbluex.liquidbounce.features.module.modules.movement.noweb.ModuleNoWeb
import net.ccbluex.liquidbounce.features.module.modules.movement.speed.ModuleSpeed
import net.ccbluex.liquidbounce.features.module.modules.movement.spider.ModuleSpider
import net.ccbluex.liquidbounce.features.module.modules.movement.step.ModuleReverseStep
import net.ccbluex.liquidbounce.features.module.modules.movement.step.ModuleStep
import net.ccbluex.liquidbounce.features.module.modules.movement.terrainspeed.ModuleTerrainSpeed
import net.ccbluex.liquidbounce.features.module.modules.player.ModuleAntiAFK
import net.ccbluex.liquidbounce.features.module.modules.player.ModuleAntiExploit
import net.ccbluex.liquidbounce.features.module.modules.player.ModuleAutoBreak
import net.ccbluex.liquidbounce.features.module.modules.player.ModuleAutoFish
import net.ccbluex.liquidbounce.features.module.modules.player.ModuleAutoRespawn
import net.ccbluex.liquidbounce.features.module.modules.player.ModuleAutoWalk
import net.ccbluex.liquidbounce.features.module.modules.player.ModuleAutoWindCharge
import net.ccbluex.liquidbounce.features.module.modules.player.ModuleBlink
import net.ccbluex.liquidbounce.features.module.modules.player.ModuleChestCleaner
import net.ccbluex.liquidbounce.features.module.modules.player.ModuleEagle
import net.ccbluex.liquidbounce.features.module.modules.player.ModuleFastExp
import net.ccbluex.liquidbounce.features.module.modules.player.ModuleFastUse
import net.ccbluex.liquidbounce.features.module.modules.player.ModuleNoBlockInteract
import net.ccbluex.liquidbounce.features.module.modules.player.ModuleNoEntityInteract
import net.ccbluex.liquidbounce.features.module.modules.player.ModuleNoRotateSet
import net.ccbluex.liquidbounce.features.module.modules.player.ModuleNoSlotSet
import net.ccbluex.liquidbounce.features.module.modules.player.ModulePotionSpoof
import net.ccbluex.liquidbounce.features.module.modules.player.ModuleReach
import net.ccbluex.liquidbounce.features.module.modules.player.ModuleReplenish
import net.ccbluex.liquidbounce.features.module.modules.player.ModuleSmartEat
import net.ccbluex.liquidbounce.features.module.modules.player.antivoid.ModuleAntiVoid
import net.ccbluex.liquidbounce.features.module.modules.player.autobuff.ModuleAutoBuff
import net.ccbluex.liquidbounce.features.module.modules.player.autoqueue.ModuleAutoQueue
import net.ccbluex.liquidbounce.features.module.modules.player.autoshop.ModuleAutoShop
import net.ccbluex.liquidbounce.features.module.modules.player.cheststealer.ModuleChestStealer
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.ModuleInventoryCleaner
import net.ccbluex.liquidbounce.features.module.modules.player.nofall.ModuleNoFall
import net.ccbluex.liquidbounce.features.module.modules.player.offhand.ModuleOffhand
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleAnimations
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleAntiBlind
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleAspect
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleAutoF5
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleBedPlates
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleBetterInventory
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleBlockESP
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleBlockOutline
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleBreadcrumbs
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleChams
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleClickGui
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleCombineMobs
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleCrystalView
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleCustomAmbience
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleDamageParticles
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleFreeCam
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleFreeLook
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleFullBright
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleHoleESP
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleHud
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleItemChams
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleItemESP
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleItemTags
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleJumpEffect
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleLogoffSpot
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleMobOwners
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleNewChunks
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleNoBob
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleNoFov
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleNoHurtCam
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleNoSwing
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleParticles
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleProphuntESP
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleProtectionZones
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleQuickPerspectiveSwap
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleRadar
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleRotations
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleSilentHotbar
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleSkinChanger
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleSmoothCamera
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleStorageESP
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleTNTTimer
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleTracers
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleTrueSight
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleVoidESP
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleXRay
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleZoom
import net.ccbluex.liquidbounce.features.module.modules.render.cameraclip.ModuleCameraClip
import net.ccbluex.liquidbounce.features.module.modules.render.crosshair.ModuleCrosshair
import net.ccbluex.liquidbounce.features.module.modules.render.esp.ModuleESP
import net.ccbluex.liquidbounce.features.module.modules.render.hats.ModuleHats
import net.ccbluex.liquidbounce.features.module.modules.render.hitfx.ModuleHitFX
import net.ccbluex.liquidbounce.features.module.modules.render.murdermystery.ModuleMurderMystery
import net.ccbluex.liquidbounce.features.module.modules.render.nametags.ModuleNametags
import net.ccbluex.liquidbounce.features.module.modules.render.trajectories.ModuleTrajectories
import net.ccbluex.liquidbounce.features.module.modules.world.ModuleAirPlace
import net.ccbluex.liquidbounce.features.module.modules.world.ModuleAutoDisable
import net.ccbluex.liquidbounce.features.module.modules.world.ModuleAutoTool
import net.ccbluex.liquidbounce.features.module.modules.world.ModuleBedDefender
import net.ccbluex.liquidbounce.features.module.modules.world.ModuleBlockIn
import net.ccbluex.liquidbounce.features.module.modules.world.ModuleBlockTrap
import net.ccbluex.liquidbounce.features.module.modules.world.ModuleExtinguish
import net.ccbluex.liquidbounce.features.module.modules.world.ModuleFastBreak
import net.ccbluex.liquidbounce.features.module.modules.world.ModuleFastPlace
import net.ccbluex.liquidbounce.features.module.modules.world.ModuleHoleFiller
import net.ccbluex.liquidbounce.features.module.modules.world.ModuleLiquidPlace
import net.ccbluex.liquidbounce.features.module.modules.world.ModuleNoSlowBreak
import net.ccbluex.liquidbounce.features.module.modules.world.ModuleProjectilePuncher
import net.ccbluex.liquidbounce.features.module.modules.world.ModuleSurround
import net.ccbluex.liquidbounce.features.module.modules.world.ModuleTimer
import net.ccbluex.liquidbounce.features.module.modules.world.autobuild.ModuleAutoBuild
import net.ccbluex.liquidbounce.features.module.modules.world.autofarm.ModuleAutoFarm
import net.ccbluex.liquidbounce.features.module.modules.world.fucker.ModuleFucker
import net.ccbluex.liquidbounce.features.module.modules.world.nuker.ModuleNuker
import net.ccbluex.liquidbounce.features.module.modules.world.packetmine.ModulePacketMine
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold
import net.ccbluex.liquidbounce.features.module.modules.world.traps.ModuleAutoTrap
import net.ccbluex.liquidbounce.script.ScriptApiRequired
import net.ccbluex.liquidbounce.utils.client.inGame
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.input.InputBind
import net.ccbluex.liquidbounce.utils.input.toModifierOrNull
import org.lwjgl.glfw.GLFW

private val modules = ObjectRBTreeSet<ClientModule>(VALUE_NAME_ORDER)

/**
 * A fairly simple module manager
 */
object ModuleManager : EventListener, Collection<ClientModule> by modules {

    val modulesConfig = ConfigSystem.root("modules", modules)

    /**
     * Handles keystrokes for module binds.
     * This also runs in GUIs, so that if a GUI is opened while a key is pressed,
     * any modules that need to be disabled on key release will be properly disabled.
     */
    @Suppress("unused")
    private val keyboardKeyHandler = handler<KeyboardKeyEvent> { event ->
        when (event.action) {
            GLFW.GLFW_PRESS -> if (mc.screen == null) {
                filter { m ->
                    m.bind.matchesKey(event.keyCode, event.scanCode) && m.bind.matchesModifiers(event.mods)
                }.forEach { m ->
                    m.enabled = !m.enabled || m.bind.action == InputBind.BindAction.HOLD
                }
            }
            GLFW.GLFW_RELEASE ->
                filter { m ->
                    m.bind.action == InputBind.BindAction.HOLD && (
                        m.bind.matchesKey(event.keyCode, event.scanCode)
                            || event.key.toModifierOrNull().let { it in m.bind.modifiers && !it!!.isAnyPressed }
                        )
                }.forEach { m ->
                    m.enabled = false
                }
        }
    }

    @Suppress("unused")
    private val mouseButtonHandler = handler<MouseButtonEvent> { event ->
        when (event.action) {
            GLFW.GLFW_PRESS -> if (mc.screen == null) {
                filter { m -> m.bind.matchesMouse(event.button) && m.bind.matchesModifiers(event.mods) }
                    .forEach { m ->
                        m.enabled = !m.running || m.bind.action == InputBind.BindAction.HOLD
                    }
            }
            GLFW.GLFW_RELEASE ->
                filter { m ->
                    m.bind.action == InputBind.BindAction.HOLD && (
                        m.bind.matchesMouse(event.button)
                            || event.key.toModifierOrNull().let { it in m.bind.modifiers && !it!!.isAnyPressed }
                        )
                }.forEach { m -> m.enabled = false }
        }
    }

    /**
     * Handles world change and enables modules that are not enabled yet
     */
    @Suppress("unused")
    private val handleWorldChange = sequenceHandler<WorldChangeEvent> { event ->
        // Delayed start handling
        if (event.world != null) {
            tickUntil { inGame }
            AutoConfig.withLoading {
                for (module in modules) {
                    if (!module.enabled || module.calledSinceStartup) continue

                    try {
                        module.calledSinceStartup = true
                        // inGame is false here, so use onToggle0
                        module.onToggled(true)
                    } catch (e: Exception) {
                        logger.error("Failed to enable module ${module.name}", e)
                    }
                }
            }
        }

        // Store modules configuration after world change, happens on disconnect as well
        ConfigSystem.store(modulesConfig)
    }

    /**
     * Handles disconnect and if [ClientModule.disableOnQuit] is true disables module
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
            ModuleAutoRod,
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
            ModuleNewChunks,
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
            ModuleYggdrasilSignatureFix,

            // Fun
            ModuleDankBobbing,
            ModuleDerp,
            ModuleNotebot,
            ModuleSkinDerp,
            ModuleHandDerp,
            ModuleTwerk,
            ModuleVomit,

            // Misc
            ModuleAutoConfig,
            ModuleGUICloser,
            ModuleBookBot,
            ModuleAntiBot,
            ModuleBetterTab,
            ModuleItemScroller,
            ModuleBetterChat,
            ModuleElytraTarget,
            ModuleMacros,
            ModuleMiddleClickAction,
            ModuleInventoryTracker,
            ModuleNameProtect,
            ModuleTextFieldProtect,
            ModuleNotifier,
            ModuleSpammer,
            ModuleAutoAccount,
            ModuleTeams,
            ModuleElytraSwap,
            ModuleAutoChatGame,
            ModuleReportHelper,
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
            ModuleNoPose,
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
            ModuleAutoWindCharge,
            ModuleOffhand,
            ModuleAutoShop,
            ModuleAutoWalk,
            ModuleBlink,
            ModuleChestCleaner,
            ModuleChestStealer,
            ModuleEagle,
            ModuleFastExp,
            ModuleFastUse,
            ModuleInventoryCleaner,
            ModuleNoBlockInteract,
            ModuleNoEntityInteract,
            ModuleNoFall,
            ModuleNoRotateSet,
            ModuleNoSlotSet,
            ModuleReach,
            ModuleAutoQueue,
            ModuleSmartEat,
            ModuleReplenish,
            ModulePotionSpoof,

            // Render
            ModuleAnimations,
            ModuleAntiBlind,
            ModuleBetterInventory,
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
            ModuleSmoothCamera,
            ModuleFreeLook,
            ModuleFullBright,
            ModuleHoleESP,
            ModuleHud,
            ModuleHats,
            ModuleItemESP,
            ModuleItemTags,
            ModuleJumpEffect,
            ModuleMobOwners,
            ModuleMurderMystery,
            ModuleHitFX,
            ModuleNametags,
            ModuleCombineMobs,
            ModuleAspect,
            ModuleAutoF5,
            ModuleChams,
            ModuleBedPlates,
            ModuleNoBob,
            ModuleNoFov,
            ModuleNoHurtCam,
            ModuleNoSwing,
            ModuleCustomAmbience,
            ModuleProphuntESP,
            ModuleQuickPerspectiveSwap,
            ModuleRadar,
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
            ModuleProtectionZones,
            ModuleCrosshair,

            // World
            ModuleAirPlace,
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
        )

        builtin.forEach { module ->
            addModule(module)
            module.walkKeyPath()
            module.verifyFallbackDescription()
        }
    }

    fun addModule(module: ClientModule) {
        if (!modules.add(module)) {
            error("Module '${module.name}' is already registered.")
        }
        module.walkInit()
        module.onRegistration()
    }

    fun removeModule(module: ClientModule) {
        if (!modules.remove(module)) {
            error("Module '${module.name}' is not registered.")
        }
        if (module.running) {
            module.onDisabled()
        }
        module.unregister()
    }

    fun clear() {
        modules.clear()
    }

    /**
     * This is being used by UltralightJS for the implementation of the ClickGUI. DO NOT REMOVE!
     */
    @JvmName("getCategories")
    @ScriptApiRequired
    fun getCategories() = ModuleCategories.entries.mapToArray { it.tag }

    @JvmName("getModules")
    @ScriptApiRequired
    fun getModules(): Collection<ClientModule> = modules

    @JvmName("getModuleByName")
    @ScriptApiRequired
    fun getModuleByName(module: String) = find { it.name.equals(module, true) }

    operator fun get(moduleName: String) = modules.find { it.name.equals(moduleName, true) }

}
