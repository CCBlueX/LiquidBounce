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
package net.ccbluex.liquidbounce.features.module.modules.movement.inventorymove

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.mojang.blaze3d.platform.InputConstants
import net.ccbluex.fastutil.fastIterable
import net.ccbluex.fastutil.referenceBooleanArrayMapOf
import net.ccbluex.liquidbounce.config.types.group.ValueGroup
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.features.module.modules.movement.inventorymove.features.InventoryMoveBlinkFeature
import net.ccbluex.liquidbounce.features.module.modules.movement.inventorymove.features.InventoryMoveSafeFeature
import net.ccbluex.liquidbounce.features.module.modules.movement.inventorymove.features.InventoryMoveSneakControlFeature
import net.ccbluex.liquidbounce.features.module.modules.movement.inventorymove.features.InventoryMoveSprintControlFeature
import net.ccbluex.liquidbounce.features.module.modules.movement.inventorymove.features.InventoryMoveStopOnActionFeature
import net.ccbluex.liquidbounce.features.module.modules.movement.inventorymove.features.InventoryMoveTimerFeature
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleClickGui
import net.minecraft.client.KeyMapping
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.components.MultiLineEditBox
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen
import net.minecraft.client.gui.screens.inventory.InventoryScreen
import net.minecraft.client.input.KeyEvent

/**
 * InventoryMove module
 *
 * Allows you to walk while an inventory is opened.
 */

object ModuleInventoryMove : ClientModule("InventoryMove", ModuleCategories.MOVEMENT) {

    private object Screens: ValueGroup("Screens") {
        val container by boolean("Container", true)
        val inventory by boolean("Inventory", true)
        val gui by boolean("Gui", true)

        fun shouldHandle(screen: Screen) = when (screen) {
            is InventoryScreen, is CreativeModeInventoryScreen -> inventory
            is AbstractContainerScreen<*> -> container
            else -> gui && !screen.isInEditBox() && !ModuleClickGui.isInSearchBar
        }

        private fun Screen.isInEditBox() = when (this.focused) {
            is EditBox, is MultiLineEditBox -> true
            else -> false
        }
    }

    private object Passthrough : ValueGroup("Passthrough") {
        val sneak by boolean("Sneak", false)
        val jump by boolean("Jump", true)
    }

    // states of movement keys, using mc.options.<key>.isPressed doesn't work for some reason
    val movementKeys =
        referenceBooleanArrayMapOf(
            mc.options.keyUp, false,
            mc.options.keyLeft, false,
            mc.options.keyDown, false,
            mc.options.keyRight, false,
            mc.options.keyJump, false,
            mc.options.keyShift, false,
        )

    /**
     * Restricts user from clicking while moving or sprinting in inventory.
     */
    val doNotAllowClicking
        get() = InventoryMoveSafeFeature.enabled && movementKeys.fastIterable().any {
            it.booleanValue && shouldHandleInputs(it.key)
        }

    init {
        tree(Screens)
        tree(Passthrough)

        tree(InventoryMoveSprintControlFeature)
        tree(InventoryMoveSneakControlFeature)
        tree(InventoryMoveTimerFeature)
        tree(InventoryMoveBlinkFeature)
        tree(InventoryMoveSafeFeature)
        tree(InventoryMoveStopOnActionFeature)
    }

    @JvmStatic
    @JvmOverloads
    fun shouldHandleInputs(key: KeyMapping, screen: Screen? = mc.gui.screen()): Boolean {
        if (!running) return false
        val screen = screen ?: return true

        when (key) {
            mc.options.keyShift -> Passthrough.sneak
            mc.options.keyJump -> Passthrough.jump
            mc.options.keyUp, mc.options.keyDown, mc.options.keyLeft, mc.options.keyRight -> true
            else -> false
        }.also { if (!it) return false }

        // If we are in a handled screen, we should handle the inputs only if the undetectable option is not enabled
        return Screens.shouldHandle(screen)
    }

    @JvmStatic
    fun shouldHandleInputs(event: KeyEvent) = KeyMapping.MAP[InputConstants.getKey(event)]
        ?.any(::shouldHandleInputs)
        ?: false

    override fun prepareDeserialize(jsonObject: JsonObject) {
        jsonObject["value"].asJsonArray
            .also { values ->
                println(values)

                values
                    .map { it.asJsonObject }
                    .map { it["name"].asString to it["value"] }
                    .forEach { (name, value) ->
                        when (name) {
                            "Behavior" -> when(value.asString) {
                                "Safe" -> {
                                    values.add(
                                        JsonObject()
                                            .apply {
                                                addProperty("name", "Screens")
                                                add("value", JsonArray()
                                                    .apply {
                                                        add(
                                                            JsonObject()
                                                                .apply {
                                                                    addProperty("name", "Container")
                                                                    addProperty("value", false)
                                                                }
                                                        )
                                                    }
                                                )
                                            }
                                    )
                                    values.add(
                                        JsonObject()
                                            .apply {
                                                addProperty("name", "Safe")
                                                add("value", JsonArray()
                                                    .apply {
                                                        add("value",  JsonObject()
                                                            .apply {
                                                                addProperty("name", "Enable")
                                                                addProperty("value", true)
                                                            }
                                                        )
                                                    }
                                                )
                                            }
                                    )
                                }
                                "Undetectable" -> {
                                    values.add(
                                        JsonObject()
                                            .apply {
                                                addProperty("name", "Screens")
                                                add("value", JsonArray()
                                                    .apply {
                                                        add(
                                                            JsonObject()
                                                                .apply {
                                                                    addProperty("name", "Container")
                                                                    addProperty("value", false)
                                                                }
                                                        )
                                                        add(
                                                            JsonObject()
                                                                .apply {
                                                                    addProperty("name", "Inventory")
                                                                    addProperty("value", false)
                                                                }
                                                        )
                                                    }
                                                )
                                            }
                                    )
                                }
                                "StopOnAction" -> values.add(
                                    JsonObject()
                                        .apply {
                                            addProperty("name", "StopOnAction")
                                            add("value",  JsonArray()
                                                .apply {
                                                    add(
                                                        JsonObject()
                                                            .apply {
                                                                addProperty("name", "Enable")
                                                                addProperty("value", true)
                                                            }
                                                    )
                                                }
                                            )
                                        }

                                )
                            }
                            "PassthroughSneak" -> {
                                values.add(
                                    JsonObject()
                                        .apply {
                                            addProperty("name", "Passthrough")
                                            add("value", JsonArray()
                                                .apply {
                                                    add(
                                                        JsonObject()
                                                            .apply {
                                                                addProperty("name", "Sneak")
                                                                addProperty("value", value.asBoolean)
                                                            }
                                                    )
                                                }
                                            )
                                        }
                                )
                            }
                        }
                    }

                println(values)
            }
    }

}
