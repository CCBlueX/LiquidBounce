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
package net.ccbluex.liquidbounce.features.module.modules.player.autoshop.serializable.conditions

object ConditionCalculator {
    private val items = mutableMapOf<String, Int>()
    private val stack = mutableListOf<Pair<ConditionNode, Boolean>>()
    private val results = mutableMapOf<ConditionNode, Boolean>()

    fun items(newItems: Map<String, Int>) : ConditionCalculator {
        this.items.clear()
        this.items.putAll(newItems)
        return this
    }

    fun process(root: ConditionNode) : Boolean {
        stack.add(root to false)

        while (stack.isNotEmpty()) {
            val (currentNode, isVisited) = stack.removeLast()

            when (currentNode) {
                is ItemConditionNode -> processItemConditionNode(currentNode)
                is AllConditionNode -> processAllConditionNode(currentNode, isVisited)
                is AnyConditionNode -> processAnyConditionNode(currentNode, isVisited)
            }
        }

        val result = results[root] ?: false
        results.clear()
        return result
    }

    private fun processItemConditionNode(currentNode: ItemConditionNode) {
        val itemAmount = items[currentNode.id] ?: 0
        val result = itemAmount <= currentNode.max &&
            itemAmount >= currentNode.min.coerceAtMost(currentNode.max)

        results[currentNode] = result
    }

    private fun processAllConditionNode(currentNode: AllConditionNode, isVisited: Boolean) {
        if (currentNode.all.isEmpty()) {
            results[currentNode] = true
            return
        }

        if (isVisited) {
            results[currentNode] = currentNode.all.all { results[it] == true }
            return
        }

        stack.add(currentNode to true)
        currentNode.all.asReversed().forEach { childNode ->
            stack.add(childNode to false)
        }
    }

    private fun processAnyConditionNode(currentNode: AnyConditionNode, isVisited: Boolean) {
        if (currentNode.any.isEmpty()) {
            results[currentNode] = true
            return
        }

        if (isVisited) {
            results[currentNode] = currentNode.any.any { results[it] == true }
            return
        }

        stack.add(currentNode to true)
        currentNode.any.asReversed().forEach { childNode ->
            stack.add(childNode to false)
        }
    }
}
