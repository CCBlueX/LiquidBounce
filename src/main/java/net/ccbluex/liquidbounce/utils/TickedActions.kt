/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils

import com.google.common.collect.HashBasedTable
import com.google.common.collect.Table
import com.google.common.collect.Tables
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.Listenable
import net.ccbluex.liquidbounce.event.TickEvent
import net.ccbluex.liquidbounce.event.WorldEvent
import net.ccbluex.liquidbounce.features.module.Module

object TickedActions : Listenable {

    private val actions: Table<Module, () -> Unit, Int> = HashBasedTable.create()

    fun add(action: () -> Unit, id: Int, module: Module): Boolean {
        val cell = Tables.immutableCell(module, action, id)

        if (!actions.cellSet().contains(cell)) {
            actions.put(cell.rowKey, cell.columnKey, cell.value)
            return true
        }

        return false
    }

    fun containsId(id: Int, module: Module): Boolean =
        actions.cellSet().count { it.value == id && it.rowKey == module } == 1

    fun clear(module: Module) {
        actions.cellSet().removeIf { it.rowKey == module }
    }

    @EventTarget(priority = 1)
    fun onTick(event: TickEvent) {
        for (action in actions.cellSet()) {
            action.columnKey.invoke()
        }

        actions.clear()
    }

    @EventTarget
    fun onWorld(event: WorldEvent) {
        actions.clear()
    }

    override fun handleEvents() = true
}