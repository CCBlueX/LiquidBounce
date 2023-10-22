package net.ccbluex.liquidbounce.features.command.builder

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
