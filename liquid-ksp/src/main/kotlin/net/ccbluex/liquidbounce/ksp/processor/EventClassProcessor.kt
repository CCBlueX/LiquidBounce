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
        environment.codeGenerator.writeObjectListFile(
            packageName = "net.ccbluex.liquidbounce.event",
            fileName = "EventClasses",
            itemType = "java.lang.Class<out net.ccbluex.liquidbounce.event.Event>",
            receiverName = "net.ccbluex.liquidbounce.event.EventManager",
            extensionName = "allEventClasses",
            objects = eventClasses,
            mapper = { it.normalizedQualifierName() + "::class.java" }
        )
    }

}
