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

@file:Suppress("deprecation")
package net.ccbluex.liquidbounce.utils.mappings

import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.mappings.EnvironmentRemapper.intermediaryToYarn
import net.ccbluex.liquidbounce.utils.mappings.LabyFabricWrapper.obfToIntermediary
import net.fabricmc.mappings.ClassEntry
import net.fabricmc.mappings.FieldEntry
import net.fabricmc.mappings.Mappings
import net.fabricmc.mappings.MethodEntry
import kotlin.collections.addAll

// why
typealias ACE = Array<ClassEntry>
typealias AFE = Array<FieldEntry>
typealias AME = Array<MethodEntry>

// TODO: clean up this mess... (but detekt doesn't detect it LOL)
object GarbageCodeHolder {
    class ObjectMappings(
        private val namespaces: Collection<String>,
        private val classEntries: Collection<ClassEntry>,
        private val fieldEntries: Collection<FieldEntry>,
        private val methodEntries: Collection<MethodEntry>
    ) : Mappings {
        override fun getNamespaces(): Collection<String> = namespaces
        override fun getClassEntries(): Collection<ClassEntry> = classEntries
        override fun getFieldEntries(): Collection<FieldEntry> = fieldEntries
        override fun getMethodEntries(): Collection<MethodEntry> = methodEntries
    }

    fun mapClassEntries(o2i: ACE, i2y: ACE): List<ClassEntry> {
        val combinedCE = mutableListOf<ClassEntry>().apply {
            addAll(o2i)
            addAll(i2y)
        }

        val groupedCE = combinedCE.groupBy { it.get("intermediary") }
        val pairedCE = groupedCE.mapNotNull { (_, v) ->
            val o2i = v.find { o2i.contains(it) } ?: return@mapNotNull null
            val i2y = v.find { i2y.contains(it) } ?: return@mapNotNull null
            o2i to i2y
        }

        return pairedCE.map {
            val (o2i, i2y) = it
            ClassEntry { ns ->
                val intermediary = i2y.get("intermediary")
                val obfuscated = o2i.get("official")
                when (ns) {
                    "named" -> i2y.get("named") ?: intermediary
                    "intermediary" -> intermediary
                    "official" -> obfuscated
                    else -> {
                        logger.warn("Unhandled namespace in class entry: $ns")
                        null
                    }
                }
            }
        }
    }

    fun mapFieldEntries(o2i: AFE, i2y: AFE): List<FieldEntry> {
        val combined = mutableListOf<FieldEntry>().apply {
            addAll(o2i)
            addAll(i2y)
        }

        val grouped = combined.groupBy { it.get("intermediary") }
        val paired = grouped.mapNotNull { (_, v) ->
            val o2i = v.find { o2i.contains(it) } ?: return@mapNotNull null
            val i2y = v.find { i2y.contains(it) } ?: return@mapNotNull null
            o2i to i2y
        }

        return paired.map {
            val (o2i, i2y) = it
            FieldEntry { ns ->
                val intermediary = i2y.get("intermediary")
                val obfuscated = o2i.get("official")
                when (ns) {
                    "named" -> i2y.get("named") ?: intermediary
                    "intermediary" -> intermediary
                    "official" -> obfuscated
                    else -> {
                        logger.warn("Unhandled namespace in field entry: $ns")
                        null
                    }
                }
            }
        }
    }

    fun mapMethodEntries(o2i: AME, i2y: AME): List<MethodEntry> {
        val combined = mutableListOf<MethodEntry>().apply {
            addAll(o2i)
            addAll(i2y)
        }

        val grouped = combined.groupBy { it.get("intermediary") }
        val paired = grouped.mapNotNull { (_, v) ->
            val o2i = v.find { o2i.contains(it) } ?: return@mapNotNull null
            val i2y = v.find { i2y.contains(it) } ?: return@mapNotNull null
            o2i to i2y
        }

        return paired.map {
            val (o2i, i2y) = it
            MethodEntry { ns ->
                val intermediary = i2y.get("intermediary")
                val obfuscated = o2i.get("official")
                when (ns) {
                    "named" -> i2y.get("named") ?: intermediary
                    "intermediary" -> intermediary
                    "official" -> obfuscated
                    else -> {
                        logger.warn("Unhandled namespace in method entry: $ns")
                        null
                    }
                }
            }
        }
    }

    fun handleLabyMod(): ObjectMappings? {
        val im2y = intermediaryToYarn ?: return null
        val o2i = obfToIntermediary ?: return null

        val o2iNamespaces = o2i.namespaces
        val i2yNamespaces = im2y.namespaces

        val o2iCE = o2i.classEntries.toTypedArray()
        val i2yCE = im2y.classEntries.toTypedArray()
        val ce = mapClassEntries(o2iCE, i2yCE)

        val o2iFE = o2i.fieldEntries.toTypedArray()
        val i2yFE = im2y.fieldEntries.toTypedArray()
        val fe = mapFieldEntries(o2iFE, i2yFE)

        val o2iME = o2i.methodEntries.toTypedArray()
        val i2yME = im2y.methodEntries.toTypedArray()
        val me = mapMethodEntries(o2iME, i2yME)

        return ObjectMappings(
            namespaces = (o2iNamespaces + i2yNamespaces).distinct(),
            classEntries = ce,
            fieldEntries = fe,
            methodEntries = me
        )
    }
}
