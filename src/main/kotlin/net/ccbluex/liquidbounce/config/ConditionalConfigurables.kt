package net.ccbluex.liquidbounce.config

import net.ccbluex.liquidbounce.config.util.Exclude
import net.ccbluex.liquidbounce.event.Listenable
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.extensions.toLowerCamelCase
import net.minecraft.client.MinecraftClient
import net.minecraft.client.network.ClientPlayNetworkHandler
import net.minecraft.client.network.ClientPlayerEntity
import net.minecraft.client.world.ClientWorld
import net.minecraft.text.TranslatableText

/**
 * Should handle events when enabled. Allows the client-user to toggle features. (like modules)
 */
open class ToggleableConfigurable(@Exclude val module: Module? = null, name: String, enabled: Boolean) : Listenable,
    Configurable(name) {

    val translationBaseKey: String
        get() = "${module?.translationBaseKey}.value.${name.toLowerCamelCase()}"

    val description: TranslatableText
        get() = TranslatableText("$translationBaseKey.description")

    var enabled by boolean("Enabled", enabled)

    override fun handleEvents() = super.handleEvents() && enabled

    override fun parent() = module

}

/**
 * Allows to configure and manage modes
 */
open class ChoiceConfigurable(
    @Exclude val module: Module,
    name: String,
    var active: String,
    val initialize: (ChoiceConfigurable) -> Unit
) : Configurable(name) {

    val translationBaseKey: String
        get() = "${module.translationBaseKey}.value.${name.toLowerCamelCase()}"

    val description: TranslatableText
        get() = TranslatableText("$translationBaseKey.description")

    fun newState(state: Boolean) {
        val choice = choices.find { active.equals(it.name, true) } ?: return

        if (state) {
            choice.enable()
        } else {
            choice.disable()
        }
    }

    @Exclude(keepInternal = true)
    val choices: MutableList<Choice> = mutableListOf()

}

/**
 * A mode is sub-module to separate different bypasses into extra classes
 */
open class Choice(name: String, @Exclude private val configurable: ChoiceConfigurable) : Configurable(name), Listenable {

    private val translationBaseKey: String
        get() = "${configurable.translationBaseKey}.choice.${name.toLowerCamelCase()}"

    val description: TranslatableText
        get() = TranslatableText("$translationBaseKey.description")

    init {
        configurable.choices += this
    }

    /**
     * Quick access
     */
    protected val mc: MinecraftClient
        get() = net.ccbluex.liquidbounce.utils.mc
    protected val player: ClientPlayerEntity
        get() = mc.player!!
    protected val world: ClientWorld
        get() = mc.world!!
    protected val network: ClientPlayNetworkHandler
        get() = mc.networkHandler!!

    val isActive: Boolean
        get() = configurable.active.equals(name, true)

    /**
     * Called when module is turned on
     */
    open fun enable() { }

    /**
     * Called when module is turned off
     */
    open fun disable() { }

    /**
     * Events should be handled when mode is enabled
     */
    override fun handleEvents() = super.handleEvents() && isActive

    /**
     * Parent listenable
     */
    override fun parent() = configurable.module

}

/**
 * Empty mode. It does nothing. Use it when you want a client-user to disable a feature.
 */
class NoneChoice(configurable: ChoiceConfigurable) : Choice("None", configurable)
