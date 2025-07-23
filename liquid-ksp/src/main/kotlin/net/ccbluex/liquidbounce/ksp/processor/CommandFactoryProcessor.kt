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
        environment.codeGenerator.writeObjectListFile(
            packageName = "net.ccbluex.liquidbounce.features.command",
            fileName = "CommandFactories",
            itemType = "net.ccbluex.liquidbounce.features.command.CommandFactory",
            receiverName = "net.ccbluex.liquidbounce.features.command.CommandManager",
            extensionName = "allCommandFactories",
            objects = commands,
        )
    }

}
