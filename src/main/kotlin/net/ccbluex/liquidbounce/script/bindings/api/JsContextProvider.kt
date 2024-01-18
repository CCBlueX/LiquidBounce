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
package net.ccbluex.liquidbounce.script.bindings.api

import net.ccbluex.liquidbounce.features.command.builder.CommandBuilder
import net.ccbluex.liquidbounce.features.command.builder.ParameterBuilder
import net.ccbluex.liquidbounce.script.bindings.features.JsSetting
import net.ccbluex.liquidbounce.script.bindings.globals.JsClient
import net.ccbluex.liquidbounce.utils.client.mc
import net.minecraft.util.Hand
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.MathHelper
import net.minecraft.util.math.Vec3d
import net.minecraft.util.math.Vec3i
import org.graalvm.polyglot.Value

/**
 * The main hub of the ScriptAPI that provides access to a useful set of members.
 */
object JsContextProvider {

    internal fun setupUsefulContext(context: Value) = context.apply {
        // Class bindings
        // -> Client API
        putMember("Setting", JsSetting)
        putMember("CommandBuilder", CommandBuilder)
        putMember("ParameterBuilder", ParameterBuilder)
        // -> Minecraft API
        putMember("Vec3i", Vec3i::class.java)
        putMember("Vec3d", Vec3d::class.java)
        putMember("MathHelper", MathHelper::class.java)
        putMember("BlockPos", BlockPos::class.java)
        putMember("Hand", Hand::class.java)

        // Variable bindings
        putMember("mc", mc)
        putMember("client", JsClient)

        // Register utilities
        putMember("rotationUtil", JsRotationUtil)
        putMember("itemUtil", JsItemUtil)
        putMember("networkUtil", JsNetworkUtil)
        putMember("interactionUtil", JsInteractionUtil)
        putMember("blockUtil", JsBlockUtil)
        putMember("movementUtil", JsMovementUtil)
        putMember("reflectionUtil", JsReflectionUtil)
    }

}
