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
package net.ccbluex.liquidbounce.features.module.modules.combat.crystalaura.trigger

import net.ccbluex.liquidbounce.config.types.Configurable
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.features.module.MinecraftShortcuts
import net.ccbluex.liquidbounce.features.module.modules.combat.crystalaura.ModuleCrystalAura
import net.ccbluex.liquidbounce.features.module.modules.combat.crystalaura.trigger.triggers.*
import net.ccbluex.liquidbounce.injection.mixins.minecraft.network.MixinClientPlayNetworkHandler
import net.ccbluex.liquidbounce.injection.mixins.minecraft.network.MixinClientPlayerInteractionManager
import net.ccbluex.liquidbounce.utils.combat.CombatManager
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.function.BooleanSupplier

// TODO no duplicate place and break options per tick in both place and break
/**
 * Catches events that should start a new place or break action.
 *
 * This is basically the managing class of the crystal aura.
 *
 * Mixins: [MixinClientPlayNetworkHandler], [MixinClientPlayerInteractionManager]
 */
object CrystalAuraTriggerer : Configurable("Triggers"), EventListener, MinecraftShortcuts {

    // avoids grim multi action flags
    private val notWhileUsingItem by boolean("NotWhileUsingItem", false)

    /**
     * Runs the calculations on a separate thread avoiding overhead on the render thread.
     */
    val offThread by boolean("Off-Thread", true)

    private val service = Executors.newSingleThreadExecutor()

    /**
     * The currently executed placement task.
     */
    private var currentPlaceTask: Future<*>? = null

    /**
     * The currently executed destroy task.
     */
    private var currentDestroyTask: Future<*>? = null

    private var canCache: BooleanSupplier

    init {
        // register all triggers
        val triggers = arrayOf(
            TickTrigger,
            BlockChangeTrigger,
            ClientBlockBreakTrigger,
            CrystalSpawnTrigger,
            CrystalDestroyTrigger,
            ExplodeSoundTrigger,
            EntityMoveTrigger,
            SelfMoveTrigger
        )

        canCache = BooleanSupplier {
            triggers.filter { it.enabled }.all { it.allowsCaching }
        }

        triggers.forEach {
            it.apply {
                it.option = boolean(it.name, it.default)
            }
        }
    }

    fun terminateRunningTasks() {
        currentPlaceTask?.cancel(true)
        currentDestroyTask?.cancel(true)
    }

    fun runPlace(runnable: Runnable) {
        currentPlaceTask?.let {
            if (!it.isDone) {
                return
            }
        }

        if (offThread) {
            currentPlaceTask = service.submit(runnable)
        } else {
            currentPlaceTask?.cancel(true)
            currentPlaceTask = null
            mc.execute(runnable)
        }
    }

    fun runDestroy(runnable: Runnable) {
        currentDestroyTask?.let {
            if (!it.isDone) {
                return
            }
        }

        if (offThread) {
            currentDestroyTask = service.submit(runnable)
        } else {
            currentDestroyTask?.cancel(true)
            currentDestroyTask = null
            mc.execute(runnable)
        }
    }

    /**
     * We should not cache if the calculation is done off-tread because the cache gets cleared on tick,
     * that means calculation which runs on a separate thread could run parallel to the clearing.
     *
     * Additionally, the caching is not needed if the calculation is multithreaded and therefore already has no
     * performance impact on the render thread.
     *
     * Event triggers don't normally allow caching either because between clearing and the next execution could be
     * almost a whole tick leading to wrong data when, for example, entities moved.
     */
    fun canCache() = !offThread && canCache.asBoolean

    /**
     * Also pauses when the combat manager tells combat modules to pause or option
     * (e.g. [notWhileUsingItem]) require it.
     */
    override val running: Boolean
        get() = ModuleCrystalAura.running
            && !CombatManager.shouldPauseCombat
            && (!player.isUsingItem || !notWhileUsingItem)

}
