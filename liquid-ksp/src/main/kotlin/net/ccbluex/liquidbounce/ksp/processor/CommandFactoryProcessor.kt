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
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import net.ccbluex.liquidbounce.annotations.InbuiltCommandFactory

class CommandFactoryProcessor(private val environment: SymbolProcessorEnvironment) : SymbolProcessor {

    lateinit var commands: Sequence<KSClassDeclaration>

    override fun process(resolver: Resolver): List<KSAnnotated> {
        commands = resolver.getSymbolsWithAnnotation(InbuiltCommandFactory::class.qualifiedName!!)
            .filterIsInstance<KSClassDeclaration>()
            .filter {
                // @InbuiltCommandFactory object XXX : CommandFactory
                it.classKind == ClassKind.OBJECT && it.isPublic()
            }
        return emptyList()
    }

    override fun finish() {
        val count = environment.codeGenerator.writeObjectListFile(
            packageName = "net.ccbluex.liquidbounce.features.command",
            fileName = "CommandFactories",
            itemType = "net.ccbluex.liquidbounce.features.command.CommandFactory",
            receiverName = "net.ccbluex.liquidbounce.features.command.CommandManager",
            extensionName = "allCommandFactories",
            objects = commands,
        )

        environment.logger.info("[KSP] Collected $count command factories.")
    }

}
