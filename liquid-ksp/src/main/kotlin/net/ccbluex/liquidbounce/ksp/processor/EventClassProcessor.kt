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

package net.ccbluex.liquidbounce.ksp.processor

import com.google.devtools.ksp.isPublic
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import net.ccbluex.liquidbounce.annotations.InbuiltEvent

class EventClassProcessor(private val environment: SymbolProcessorEnvironment) : SymbolProcessor {

    lateinit var eventClasses: Sequence<KSClassDeclaration>

    override fun process(resolver: Resolver): List<KSAnnotated> {
        eventClasses = resolver.getSymbolsWithAnnotation(InbuiltEvent::class.qualifiedName!!)
            .filterIsInstance<KSClassDeclaration>()
            .filter {
                // @InbuiltEvent("...") class/object XXEvent
                it.isPublic()
            }
        return emptyList()
    }

    override fun finish() {
        val count = environment.codeGenerator.writeObjectListFile(
            packageName = "net.ccbluex.liquidbounce.event",
            fileName = "EventClasses",
            itemType = "java.lang.Class<out net.ccbluex.liquidbounce.event.Event>",
            receiverName = "net.ccbluex.liquidbounce.event.EventManager",
            extensionName = "allEventClasses",
            objects = eventClasses,
            mapper = { it.normalizedQualifierName() + "::class.java" }
        )

        environment.logger.info("[KSP] Collected $count event classes.")
    }

}
