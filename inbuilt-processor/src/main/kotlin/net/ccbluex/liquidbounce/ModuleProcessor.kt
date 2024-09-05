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
package net.ccbluex.liquidbounce

import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import net.ccbluex.liquidbounce.register.IncludeModule

/**
 * Generates an object that contains an array with all modules annotated with [IncludeModule].
 */
class ModuleProcessor(private val codeGenerator: CodeGenerator) : SymbolProcessor {

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val symbols = resolver.getSymbolsWithAnnotation(IncludeModule::class.qualifiedName!!)
            .filterIsInstance<KSClassDeclaration>()

        if (symbols.none()) {
            return emptyList()
        }

        generateModuleList(symbols)

        return emptyList()
    }

    private fun generateModuleList(symbols: Sequence<KSClassDeclaration>) {
        val moduleNames = symbols.map { it.simpleName.asString() }.toList()
        val importedClasses = symbols.map {
            it.packageName.asString().replace("fun", "`fun`") to it.simpleName.asString()
        }.toList()

        val arrayContent = buildString {
            appendLine("package net.ccbluex.liquidbounce.features.module")
            appendLine()
            importedClasses.forEach {
                appendLine("import ${it.first}.${it.second}")
            }
            appendLine()
            appendLine("object CollectedModules {")
            appendLine()
            appendLine("    var builtin = arrayOf(")
            moduleNames.forEach { moduleName ->
                appendLine("        $moduleName,")
            }
            appendLine("    )")
            appendLine()
            appendLine("}")
        }

        val file = codeGenerator.createNewFile(
            Dependencies(false),
            "net.ccbluex.liquidbounce.features.module",
            "CollectedModules"
        )

        file.write(arrayContent.toByteArray())
    }

}
