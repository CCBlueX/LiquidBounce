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
 * The annotation can have a development-only parameter called `dev` which will be taken into account.
 *
 * @param targetPackage Defines where the file will be generated.
 * @param filename Defines the filename.
 */
abstract class FeatureProcessor<C : Any>(
    private val codeGenerator: CodeGenerator,
    private val annotationClass: KClass<C>,
    private val targetPackage: String,
    private val filename: String
) : SymbolProcessor {

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val symbols = resolver.getSymbolsWithAnnotation(annotationClass.qualifiedName!!)
            .filterIsInstance<KSClassDeclaration>()

        val filteredSymbols = filterDev(symbols)

        if (filteredSymbols.none()) {
            return emptyList()
        }

        generateModuleList(filteredSymbols)

        return emptyList()
    }

    private fun filterDev(symbols: Sequence<KSClassDeclaration>): Sequence<KSClassDeclaration> {
        if (IN_DEVELOPMENT) {
            return symbols
        }

        return symbols.filter { declaration ->
            val annotation = declaration.annotations.find {
                it.annotationType.resolve().declaration.qualifiedName?.asString() == annotationClass.qualifiedName
            }

            // extract the 'dev' value from the annotation (defaults to false)
            val dev = annotation?.arguments?.find { it.name?.asString() == "dev" }?.value as? Boolean ?: false
            !dev
        }
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
            appendLine("var inbuilt = arrayOf(")
            objectName.forEach {
                appendLine("    $it,")
            }
            appendLine(")")
        }

        val file = codeGenerator.createNewFile(Dependencies(false), targetPackage, filename)
        file.write(arrayContent.toByteArray())
    }

}
