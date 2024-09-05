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

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import kotlin.reflect.KClass

/**
 * Generates a Kotlin file that contains an array with all objects annotated with [annotationClass].
 *
 * @param targetPackage Defines where the file will be generated.
 * @param filename Defines the filename.
 * @param additionalOperation Can be used to call something in the objects, for example, a create function.
 */
abstract class FeatureProcessor<C : Any>(
    private val codeGenerator: CodeGenerator,
    private val annotationClass: KClass<C>,
    private val targetPackage: String,
    private val filename: String,
    private val additionalOperation: String
) : SymbolProcessor {

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val symbols = resolver.getSymbolsWithAnnotation(annotationClass.qualifiedName!!)
            .filterIsInstance<KSClassDeclaration>()

        if (symbols.none()) {
            return emptyList()
        }

        generateModuleList(symbols)

        return emptyList()
    }

    private fun generateModuleList(symbols: Sequence<KSClassDeclaration>) {
        val objectName = symbols.map { it.simpleName.asString() }.toList()
        val importedClasses = symbols.map {
            it.packageName.asString().replace("fun", "`fun`") to it.simpleName.asString()
        }.toList()

        val arrayContent = buildString {
            appendLine("package $targetPackage")
            appendLine()
            importedClasses.forEach {
                appendLine("import ${it.first}.${it.second}")
            }
            appendLine()
            appendLine("var builtin = arrayOf(")
            objectName.forEach {
                appendLine("    $it$additionalOperation,")
            }
            appendLine(")")
        }

        val file = codeGenerator.createNewFile(Dependencies(false), targetPackage, filename)
        file.write(arrayContent.toByteArray())
    }

}
