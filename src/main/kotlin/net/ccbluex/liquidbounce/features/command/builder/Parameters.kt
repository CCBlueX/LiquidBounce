package net.ccbluex.liquidbounce.features.command.builder

import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleManager
import net.minecraft.registry.Registries

fun blockParameter(name: String = "block") =
    ParameterBuilder
    .begin<String>(name)
    .verifiedBy(ParameterBuilder.STRING_VALIDATOR)
    .autocompletedWith {
        Registries.BLOCK.map {
            it.translationKey.removePrefix("block.").replace('.', ':')
        }
    }

fun pageParameter(name: String = "page") =
    ParameterBuilder
        .begin<Int>(name)
        .verifiedBy(ParameterBuilder.POSITIVE_INTEGER_VALIDATOR)

fun moduleParameter(name: String = "module", validator: (Module) -> Boolean = { true }) =
    ParameterBuilder
        .begin<String>(name)
        .verifiedBy(ParameterBuilder.STRING_VALIDATOR)
        .autocompletedWith { ModuleManager.autoComplete(it, validator = validator) }


