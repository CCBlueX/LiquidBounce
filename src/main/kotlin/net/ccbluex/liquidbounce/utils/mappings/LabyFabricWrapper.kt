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

package net.ccbluex.liquidbounce.utils.mappings

import net.ccbluex.liquidbounce.utils.client.logger
import net.fabricmc.mappings.MappingsProvider
import java.nio.file.Path
import kotlin.io.path.inputStream

/**
 * Wraps around the `net.labymod.addons.labyfabric.FabricConstants` class from LabyFabric.
 * We need this for [EnvironmentRemapper] since LabyMod uses NeoForge,
 * and doesn't give us a custom [ClassLoader] that maps mc classes to intermediary if present.
 **/
object LabyFabricWrapper {
    val fcCls by lazy {
        runCatching {
            Class.forName("net.labymod.addons.labyfabric.FabricConstants")
        }.onFailure {
            logger.error("Couldn't find FabricConstants class from LabyFabric. " +
                "Ignore this if you aren't using LabyMod.", it)
        }.getOrNull()
    }
    val loaded by lazy {
        fcCls !== null
    }

    val mpFmt by lazy {
        val v: String? = fcCls?.getStaticFieldOrNull("INTERMEDIARY_MAPPINGS_PATH")
        return@lazy v
    }

    val obfToIntermediaryMappingsPath by lazy {
        val p: Path? = fcCls?.runStatic("versionedPath", String::class.java, mpFmt)
        return@lazy p
    }
    val obfToIntermediary by lazy {
        val mp = obfToIntermediaryMappingsPath ?: return@lazy null
        val st = mp.inputStream()

        // they use tiny v1
        val r = runCatching {
            MappingsProvider.readTinyMappings(st, true)
        }.getOrNull()

        st.close()

        return@lazy r
    }
}

// reflection utils lol

private fun <T, X> Class<X>.getFieldOrNull(obj: X?, name: String) = runCatching {
    this.getDeclaredField(name).get(obj)
}.getOrNull() as T?

private fun <T, S> Class<S>.getStaticFieldOrNull(name: String)
    = getFieldOrNull<T, S>(null, name)

private fun <R, S> Class<S>.run(
    name: String, self: S?, parameterTypes: Array<Class<*>>, arg: Any?): R? {
    return runCatching {
        // TODO: fix detekt here
        this.getMethod(name, *parameterTypes).invoke(self, arg) as R?
    }.onFailure {
        logger.error("Failed to invoke $name", it)
    }.getOrNull()
}

private fun <R, S> Class<S>.runStatic(
    name: String, parameterTypes: Array<Class<*>>, arg: Any?
) = run<R, S>(name, null, parameterTypes, arg)

private fun <R, S> Class<S>.run(
    name: String, self: S?, p: Class<*>, arg: Any?): R?
    = run(name, self, arrayOf(p), arg)

private fun <R, S> Class<S>.runStatic(
    name: String, p: Class<*>, arg: Any?
) = runStatic<R, S>(name, arrayOf(p), arg)
