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
package net.ccbluex.liquidbounce.utils.input

import com.mojang.blaze3d.platform.InputConstants
import it.unimi.dsi.fastutil.ints.Int2ReferenceMap
import it.unimi.dsi.fastutil.ints.Int2ReferenceOpenHashMap
import net.ccbluex.fastutil.enumSetOf
import net.ccbluex.fastutil.unmodifiable
import net.ccbluex.liquidbounce.config.types.Value
import net.ccbluex.liquidbounce.config.types.list.Tagged
import net.ccbluex.liquidbounce.config.types.list.Tagged.Companion.makeLookupTable
import net.ccbluex.liquidbounce.event.events.KeyboardKeyEvent
import net.ccbluex.liquidbounce.utils.client.asPlainText
import net.ccbluex.liquidbounce.utils.client.asText
import net.ccbluex.liquidbounce.utils.client.bold
import net.ccbluex.liquidbounce.utils.client.copyable
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.client.onHover
import net.ccbluex.liquidbounce.utils.client.regular
import net.ccbluex.liquidbounce.utils.client.variable
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent
import net.minecraft.util.Util
import org.lwjgl.glfw.GLFW

/**
 * Data class representing a key binding.
 * It holds the key to be bound and the action that will be triggered by the binding.
 *
 * @param boundKey The key that is bound to an action.
 * @param action The action triggered by the bound key (e.g., TOGGLE, HOLD).
 */
@JvmRecord
data class InputBind(
    val boundKey: InputConstants.Key,
    val action: BindAction,
    val modifiers: Set<Modifier>,
) {

    /**
     * Alternative constructor to create a binding from the key type and key code.
     *
     * @param type The type of input (keyboard, mouse, etc.).
     * @param code The key or button code.
     * @param action The action to bind to this key.
     */
    constructor(type: InputConstants.Type, code: Int, action: BindAction) :
        this(type.getOrCreate(code), action, emptySet())

    /**
     * Constructor to create a binding using a key name.
     *
     * @param name The name of the key, which will be translated to an InputUtil.Key.
     */
    constructor(name: String) :
        this(inputByName(name), BindAction.TOGGLE, emptySet())

    /**
     * Retrieves the name of the key in uppercase format, excluding the category prefixes.
     *
     * @return A formatted string representing the bound key's name, or "None" if unbound.
     */
    val keyName: String
        get() = when {
            isUnbound -> "None"
            else -> this.boundKey.name
                .split('.')
                .drop(2) // Drops the "key.keyboard" or "key.mouse" part
                .joinToString(separator = "_") // Joins the remaining parts with underscores
                .uppercase() // Converts the key name to uppercase
        }

    /**
     * Checks if the key is unbound (i.e., set to UNKNOWN_KEY).
     *
     * @return True if the key is unbound, false otherwise.
     */
    val isUnbound: Boolean
        get() = this.boundKey == InputConstants.UNKNOWN

    /**
     * Determines if the specified key matches the bound key.
     *
     * @param keyCode The GLFW key code to check.
     * @param scanCode The scan code to check.
     * @return True if the key code or scan code matches the bound key, false otherwise.
     */
    fun matchesKey(keyCode: Int, scanCode: Int): Boolean {
        return if (keyCode == InputConstants.UNKNOWN.value) {
            this.boundKey.type == InputConstants.Type.SCANCODE && this.boundKey.value == scanCode
        } else {
            this.boundKey.type == InputConstants.Type.KEYSYM && this.boundKey.value == keyCode
        }
    }

    /**
     * Determines if the specified mouse button code matches the bound key.
     *
     * @param code The mouse button code to check.
     * @return True if the mouse button matches the bound key, false otherwise.
     */
    fun matchesMouse(code: Int): Boolean {
        return this.boundKey.type == InputConstants.Type.MOUSE && this.boundKey.value == code
    }

    /**
     * Determines if the given modifiers match the required modifiers.
     *
     * @param mods The bits of modifiers.
     * @see org.lwjgl.glfw.GLFW
     */
    fun matchesModifiers(mods: Int): Boolean {
        return this.modifiers.all { it.isActive(mods) }
    }

    /**
     * Handles the event. Returns the new state, assumes the original state is `false`.
     *
     * @param event The [KeyboardKeyEvent] to handle.
     * @param currentState The current state.
     * @return The new state.
     */
    fun getNewState(event: KeyboardKeyEvent, currentState: Boolean): Boolean {
        if (!matchesKey(event.keyCode, event.scanCode)) {
            return currentState
        }

        val eventAction = event.action
        return when (eventAction) {
            GLFW.GLFW_PRESS if mc.screen == null -> !currentState || action == BindAction.HOLD
            GLFW.GLFW_RELEASE -> false
            else -> currentState
        }
    }

    /**
     * Enum representing the action associated with a key binding.
     * It includes two actions: TOGGLE and HOLD.
     *
     * @param tag The display name of the action.
     */
    enum class BindAction(override val tag: String) : Tagged {
        TOGGLE("Toggle"),
        HOLD("Hold")
    }

    enum class Modifier(override val tag: String, val bitMask: Int, vararg val keyCodes: Int): Tagged {
        SHIFT("Shift", GLFW.GLFW_MOD_SHIFT, InputConstants.KEY_LSHIFT, InputConstants.KEY_RSHIFT),
        CONTROL("Control", GLFW.GLFW_MOD_CONTROL, InputConstants.KEY_LCONTROL, InputConstants.KEY_RCONTROL),
        ALT("Alt", GLFW.GLFW_MOD_ALT, InputConstants.KEY_LALT, InputConstants.KEY_RALT),
        SUPER("Super", GLFW.GLFW_MOD_SUPER, InputConstants.KEY_LSUPER, InputConstants.KEY_RSUPER);

        /**
         * Check if self is active in [modifiers] value.
         */
        fun isActive(modifiers: Int) = modifiers and this.bitMask != 0

        /**
         * Check if any one modifier key is pressed.
         */
        val isAnyPressed: Boolean get() = this.keyCodes.any { InputConstants.isKeyDown(mc.window, it) }

        /**
         * Performs the platform (OS) specified render name of a modifier.
         */
        val platformRenderName: String get() = when (Util.getPlatform()) {
            Util.OS.WINDOWS -> when (this) {
                CONTROL -> "Ctrl"
                SUPER -> "\u229e"
                else -> tag
            }
            Util.OS.OSX -> when (this) {
                SHIFT -> "\u21e7"
                CONTROL -> "^"
                ALT -> "\u2325"
                SUPER -> "\u2318"
                // else -> choiceName
            }
            else -> tag
        }

        companion object {
            @JvmStatic
            private val LOOKUP_TABLE = Modifier.entries.makeLookupTable()

            @JvmStatic
            private val KEY_CODE_LOOKUP: Int2ReferenceMap<Modifier> = run {
                val map = Int2ReferenceOpenHashMap<Modifier>()
                for (modifier in Modifier.entries) {
                    for (keyCode in modifier.keyCodes) {
                        map.put(keyCode, modifier)
                    }
                }
                map.unmodifiable()
            }

            @JvmStatic
            fun of(string: String?): Modifier? = LOOKUP_TABLE[string]

            @JvmStatic
            fun of(keyCode: Int): Modifier? = KEY_CODE_LOOKUP[keyCode]

            @JvmStatic
            fun fromRawValue(modifiers: Int) = entries.filterTo(enumSetOf()) {
                it.isActive(modifiers)
            }
        }
    }

    companion object {
        @JvmField
        val UNBOUND = InputBind(InputConstants.UNKNOWN, BindAction.TOGGLE, emptySet())
    }

}


/**
 * Binds to the given input name.
 */
fun Value<InputBind>.bind(name: String) = set(get().copy(boundKey = inputByName(name)))

/**
 * Binds to the given input type and code.
 */
fun Value<InputBind>.bind(key: InputConstants.Key, action: InputBind.BindAction, modifiers: Set<InputBind.Modifier>) =
    set(get().copy(boundKey = key, action = action, modifiers = modifiers))

/**
 * Unbinds the key by setting it to UNKNOWN_KEY.
 */
fun Value<InputBind>.unbind() = set(InputBind.UNBOUND)

fun InputBind.renderText(): Component = buildList {
    add(
        inputByName(keyName).let { key ->
            variable(key.displayName.copy()).bold(true)
                .copyable(copyContent = key.name)
        }
    )

    val divider = regular(" + ")
    if (modifiers.isNotEmpty()) {
        modifiers.forEach {
            add(divider)
            add(variable(it.platformRenderName).onHover(HoverEvent.ShowText(it.tag.asPlainText())))
        }
    }
    add(regular(" ("))
    add(variable(action.tag))
    add(regular(")"))
}.asText()
