package net.ccbluex.liquidbounce.ksp.processor

import com.google.devtools.ksp.isPublic
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import net.ccbluex.liquidbounce.annotations.InbuiltModule

class ClientModuleProcessor(private val environment: SymbolProcessorEnvironment) : SymbolProcessor {

    lateinit var modules: Sequence<KSClassDeclaration>

    override fun process(resolver: Resolver): List<KSAnnotated> {
        modules = resolver.getSymbolsWithAnnotation(InbuiltModule::class.qualifiedName!!)
            .filterIsInstance<KSClassDeclaration>()
            .filter {
                // @InbuiltModule object XXX : ClientModule(...)
                it.classKind == ClassKind.OBJECT && it.isPublic()
            }
        return emptyList()
    }

    override fun finish() {
        val count = environment.codeGenerator.writeObjectListFile(
            packageName = "net.ccbluex.liquidbounce.features.module",
            fileName = "ClientModules",
            itemType = "net.ccbluex.liquidbounce.features.module.ClientModule",
            receiverName = "net.ccbluex.liquidbounce.features.module.ModuleManager",
            extensionName = "allClientModules",
            objects = modules,
        )

        environment.logger.info("[KSP] Collected $count client modules.")
    }

}
