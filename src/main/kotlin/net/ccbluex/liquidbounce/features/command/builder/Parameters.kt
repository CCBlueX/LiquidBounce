package net.ccbluex.liquidbounce.features.command.builder

import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleManager
import net.minecraft.registry.Registries

fun blockParameter(name: String = "block") =
    ParameterBuilder
        .begin<String>(name)
        .verifiedBy(ParameterBuilder.STRING_VALIDATOR)
        .autocompletedWith { begin ->
            Registries.BLOCK.map {
                it.translationKey
                    .removePrefix("block.")
                    .replace('.', ':')
            }
        }

fun itemParameter(name: String = "item") =
    ParameterBuilder
        .begin<String>(name)
        .verifiedBy(ParameterBuilder.STRING_VALIDATOR)
        .autocompletedWith { begin ->
            Registries.ITEM.map {
                it.translationKey
                    .removePrefix("item.")
                    .removePrefix("block.")
                    .replace('.', ':')
            }
        }

fun enchantmentParameter(name: String = "enchantment") =
    ParameterBuilder
        .begin<String>(name)
        .verifiedBy(ParameterBuilder.STRING_VALIDATOR)
        .autocompletedWith { begin ->
            Registries.ENCHANTMENT.map {
                it.translationKey
                    .removePrefix("enchantment.")
                    .replace('.', ':')
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
        .autocompletedWith { begin, args ->
            ModuleManager.autoComplete(begin, args, validator = validator)
        }


