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

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.isPublic
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import net.ccbluex.liquidbounce.annotations.Nameable

typealias InbuiltEvent = Nameable

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

    @OptIn(KspExperimental::class)
    override fun finish() {
        val packageName = "net.ccbluex.liquidbounce.event"
        val className = "EventTypeRegistry"

        val eventClasses = eventClasses.toList()

        val content = buildString(DEFAULT_BUFFER_SIZE) {
            appendLine("// Generated class")
            appendLine("package $packageName;")
            appendLine()

            appendLine("import java.util.Arrays;")
            appendLine("import java.util.Collection;")
            appendLine("import java.util.Collections;")
            appendLine("import java.util.List;")
            eventClasses.forEach {
                appendLine("import ${it.qualifiedName?.asString()};")
            }
            appendLine()

            // all classes
            appendLine("final class $className {")
            appendLine("    static final Class<? extends Event>[] ALL_EVENT_CLASSES = (Class<? extends Event>[]) new Class[] {")
            eventClasses.forEach {
                appendLine("        ${it.simpleName.asString()}.class,")
            }
            appendLine("    };")
            appendLine()

            // event to name
            appendLine("    static final String getEventName(Event event) {")
            appendLine("        return switch (event) {")
            eventClasses.forEach {
                val annotation = it.getAnnotationsByType(InbuiltEvent::class).first()
                appendLine("            case ${it.simpleName.asString()} e -> \"${annotation.name}\";")
            }
            appendLine("            default -> throw new IllegalStateException(\"Unknown event type: \" + event.getClass().getSimpleName());")
            appendLine("        };")
            appendLine("    }")
            appendLine()

            // event class to name
            appendLine("    static final String getEventName(Class<? extends Event> eventClass) {")
            eventClasses.forEachIndexed { index, it ->
                val annotation = it.getAnnotationsByType(InbuiltEvent::class).first()
                if (index != 0) {
                    append(" else ")
                } else {
                    append("        ")
                }
                appendLine("if (eventClass == ${it.simpleName.asString()}.class) {")
                appendLine("            return \"${annotation.name}\";")
                append("        }")
            }
            appendLine(" else {")
            appendLine("            throw new IllegalStateException(\"Unknown event class: \" + eventClass.getSimpleName());")
            appendLine("        }")
            appendLine("    }")
            appendLine()

            // event name to class
            appendLine("    static final Class<? extends Event> getEventClassByName(String eventName) {")
            appendLine("        return switch (eventName) {")
            eventClasses.forEach {
                val annotation = it.getAnnotationsByType(InbuiltEvent::class).first()
                appendLine("            case \"${annotation.name}\" -> ${it.simpleName.asString()}.class;")
            }
            appendLine("            default -> throw new IllegalStateException(\"Unknown event name: \" + eventName);")
            appendLine("        };")
            appendLine("    }")
            appendLine()

            // instance fields & methods
            eventClasses.forEach {
                appendLine("    private final EventHookRegistry<${it.simpleName.asString()}> hooks${it.simpleName.asString()} = new EventHookRegistry<>();")
            }
            appendLine()

            appendLine("    protected <E extends Event> EventHookRegistry<E> getEventHooks(E event) {")
            appendLine("        return (EventHookRegistry<E>) switch (event) {")
            eventClasses.forEach {
                appendLine("            case ${it.simpleName.asString()} e -> hooks${it.simpleName.asString()};")
            }
            appendLine("            default -> throw new IllegalStateException(\"Unknown event type: \" + event.getClass().getSimpleName());")
            appendLine("        };")
            appendLine("    }")
            appendLine()

            appendLine("    protected <E extends Event> EventHookRegistry<E> getEventHooks(Class<E> eventClass) {")
            eventClasses.forEachIndexed { index, it ->
                if (index != 0) {
                    append(" else ")
                } else {
                    append("        ")
                }
                appendLine("if (eventClass == ${it.simpleName.asString()}.class) {")
                appendLine("            return (EventHookRegistry<E>) hooks${it.simpleName.asString()};")
                append("        }")
            }
            appendLine(" else {")
            appendLine("            throw new IllegalStateException(\"Unknown event class: \" + eventClass.getSimpleName());")
            appendLine("        }")
            appendLine("    }")
            appendLine()

            appendLine("    private final List<EventHookRegistry<?>> allEventHooks = Collections.unmodifiableList(Arrays.asList(")
            eventClasses.forEachIndexed { index, it ->
                append("            hooks${it.simpleName.asString()}")
                if (index < eventClasses.size - 1) {
                    appendLine(",")
                } else {
                    appendLine()
                }
            }
            appendLine("    ));")
            appendLine()

            appendLine("    protected Collection<EventHookRegistry<?>> getAllEventHooks() {")
            appendLine("        return allEventHooks;")
            appendLine("    }")
            appendLine()

            // constructor
            appendLine()
            appendLine("    protected $className() {}")
            appendLine()

            appendLine("}")
        }

        environment.codeGenerator.createNewFile(
            dependencies = Dependencies.ALL_FILES,
            packageName = packageName,
            fileName = className,
            extensionName = "java",
        ).bufferedWriter().use {
            it.write(content)
        }
    }

}
