/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils

import com.google.common.collect.Lists
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.Listenable
import net.ccbluex.liquidbounce.event.TickEvent
import net.ccbluex.liquidbounce.event.WorldEvent
import net.ccbluex.liquidbounce.features.module.Module

object TickedActions : Listenable {

    private val actions = Lists.newCopyOnWriteArrayList<Triple<Module, Int, () -> Unit>>()

    fun schedule(id: Int, module: Module, allowDuplicates: Boolean = false, action: () -> Unit): Boolean {
        if (allowDuplicates || !isScheduled(id, module)) {
            actions += Triple(module, id, action)
            return true
        }

        return false
    }

    fun isScheduled(id: Int, module: Module) =
        actions.any { it.first == module && it.second == id }

    fun clear(module: Module) =
        actions.removeIf { it.first == module }

    fun isEmpty(module: Module) =
        actions.none { it.first == module }

    @EventTarget(priority = 1)
    fun onTick(event: TickEvent) {
        val scheduledActions = Array<Triple<Module, Int, () -> Unit>>(actions.size) { actions[it] }

        // Clear actions before executing all scheduled tasks
        // This way the tasks can schedule actions to the same slot for next tick without having to allow for duplicates
        actions.clear()

        for (triple in scheduledActions)
            triple.third()
    }

    @EventTarget
    fun onWorld(event: WorldEvent) = actions.clear()

    override fun handleEvents() = true
}