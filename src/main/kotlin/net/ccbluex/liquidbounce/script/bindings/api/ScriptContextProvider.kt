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
package net.ccbluex.liquidbounce.script.bindings.api

import net.ccbluex.liquidbounce.script.bindings.features.ScriptSetting
import net.ccbluex.liquidbounce.utils.client.mc
import net.minecraft.util.Hand
import net.minecraft.util.math.*
import org.graalvm.polyglot.Context
import org.graalvm.polyglot.Value
import java.util.concurrent.ConcurrentHashMap

/**
 * The main hub of the ScriptAPI that provides access to a useful set of members.
 */
object ScriptContextProvider {

    private val localStorage = ConcurrentHashMap<String, Any>()

    internal fun cleanup() {
        localStorage.clear()
    }

    internal fun Context.setupContext(language: String, bindings: Value) {
        bindings.apply {
            // Class bindings
            // -> Client API
            putMember("Setting", ScriptSetting)

            // -> Minecraft API
            putMember("Vec3i", Vec3i::class.java)
            putMember("Vec3d", Vec3d::class.java)
            putMember("MathHelper", MathHelper::class.java)
            putMember("BlockPos", BlockPos::class.java)
            putMember("Hand", Hand::class.java)
            putMember("RotationAxis", RotationAxis::class.java)

            // Variable bindings
            putMember("mc", mc)
            putMember("Client", ScriptClient)

            // Register utilities
            putMember("RotationUtil", ScriptRotationUtil)
            putMember("ItemUtil", ScriptItemUtil)
            putMember("NetworkUtil", ScriptNetworkUtil)
            putMember("InteractionUtil", ScriptInteractionUtil)
            putMember("BlockUtil", ScriptBlockUtil)
            putMember("MovementUtil", ScriptMovementUtil)
            putMember("ReflectionUtil", ScriptReflectionUtil())
            putMember("ParameterValidator", ScriptParameterValidator(bindings))
            putMember("UnsafeThread", ScriptUnsafeThread)
            putMember("Primitives", ScriptPrimitives)

            // Global variables
            putMember("localStorage", localStorage)

            // Async support (JavaScript only)
            if (language.equals("js", true)) {
                // Init Promise constructor
                val asyncUtil = ScriptAsyncUtil(getBindings(language).getMember("Promise"))
                putMember("AsyncUtil", asyncUtil)
            }
        }
    }
}
