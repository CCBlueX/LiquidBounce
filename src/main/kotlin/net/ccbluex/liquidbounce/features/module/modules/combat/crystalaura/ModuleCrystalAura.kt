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
package net.ccbluex.liquidbounce.features.module.modules.combat.crystalaura

import net.ccbluex.liquidbounce.config.types.nesting.Configurable
import net.ccbluex.liquidbounce.event.events.RotationUpdateEvent
import net.ccbluex.liquidbounce.event.events.WorldRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.modules.combat.crystalaura.destroy.SubmoduleCrystalDestroyer
import net.ccbluex.liquidbounce.features.module.modules.combat.crystalaura.place.SubmoduleCrystalPlacer
import net.ccbluex.liquidbounce.features.module.modules.combat.crystalaura.post.CrystalPostAttackTracker
import net.ccbluex.liquidbounce.features.module.modules.combat.crystalaura.post.SubmoduleSetDead
import net.ccbluex.liquidbounce.features.module.modules.combat.crystalaura.trigger.CrystalAuraTriggerer
import net.ccbluex.liquidbounce.render.renderEnvironmentForWorld
import net.ccbluex.liquidbounce.utils.aiming.NoRotationMode
import net.ccbluex.liquidbounce.utils.aiming.NormalRotationMode
import net.ccbluex.liquidbounce.utils.client.FloatValueProvider
import net.ccbluex.liquidbounce.utils.combat.CombatManager
import net.ccbluex.liquidbounce.utils.combat.TargetTracker
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.ccbluex.liquidbounce.utils.render.WorldTargetRenderer

/**
 * Module CrystalAura
 *
 * Automatically places and explodes end crystals.
 *
 * @author ccetl
 */
object ModuleCrystalAura : ClientModule(
    "CrystalAura",
    Category.COMBAT,
    aliases = arrayOf("AutoCrystal"),
    disableOnQuit = true
) {

    val targetTracker = tree(TargetTracker(
        rangeValue =  FloatValueProvider("Range", 4.5f, 1f..12f)
    ))

    object PredictFeature : Configurable("Predict") {
        init {
            treeAll(SelfPredict, TargetPredict)
        }
    }

    init {
        treeAll(
            SubmoduleCrystalPlacer,
            SubmoduleCrystalDestroyer,
            CrystalAuraDamageOptions,
            CrystalAuraTriggerer,
            PredictFeature,
            SubmoduleIdPredict,
            SubmoduleSetDead,
            SubmoduleBasePlace
        )
    }

    private val targetRenderer = tree(WorldTargetRenderer(this))

    val rotationMode = choices(this, "RotationMode") {
        arrayOf(
            NormalRotationMode(it, this, Priority.IMPORTANT_FOR_USAGE_2, true),
            NoRotationMode(it, this)
        )
    }

    override fun onDisabled() {
        CrystalAuraTriggerer.terminateRunningTasks()
        SubmoduleCrystalPlacer.placementRenderer.clearSilently()
        SubmoduleCrystalDestroyer.postAttackHandlers.forEach(CrystalPostAttackTracker::onToggle)
        SubmoduleBasePlace.onDisabled()
        CrystalAuraDamageOptions.cacheMap.clear()
    }

    override fun onEnabled() {
        SubmoduleCrystalDestroyer.postAttackHandlers.forEach(CrystalPostAttackTracker::onToggle)
    }

    @Suppress("unused")
    private val simulatedTickHandler = handler<RotationUpdateEvent>(1) {
        CrystalAuraDamageOptions.cacheMap.clear()
        if (CombatManager.shouldPauseCombat) {
            return@handler
        }

        targetTracker.selectFirst()
    }

    @Suppress("unused")
    private val renderHandler = handler<WorldRenderEvent> {
        val target = targetTracker.target ?: return@handler

        renderEnvironmentForWorld(it.matrixStack) {
            targetRenderer.render(this, target, it.partialTicks)
        }
    }

}
