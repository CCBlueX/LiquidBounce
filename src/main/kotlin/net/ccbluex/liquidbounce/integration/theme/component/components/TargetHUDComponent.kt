package net.ccbluex.liquidbounce.integration.theme.component.types

import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.integration.theme.component.FeatureTweak

@Suppress("unused")
class TargetHUDComponent(
    tweaks: Array<FeatureTweak> = emptyArray()
) : IntegratedComponent("TargetHUD", tweaks) {

    private val mode by enumChoice("Mode", Mode.Modern)
    private val timeout by int("Timeout",2000,0..5000,"ms")
    init {
        registerComponentListen()
    }

    enum class Mode(override val choiceName: String) : NamedChoice {
        Simple("Simple"),
        Modern("Modern"),
        HU_JI("户籍"),
    }
}


