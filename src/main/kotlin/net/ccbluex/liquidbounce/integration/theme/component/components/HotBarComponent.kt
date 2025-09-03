//package net.ccbluex.liquidbounce.integration.theme.component.types
//
//import net.ccbluex.liquidbounce.config.types.NamedChoice
//import net.ccbluex.liquidbounce.integration.theme.component.FeatureTweak
//
//@Suppress("unused")
//class HotBarComponent(
//    tweaks: Array<FeatureTweak> = emptyArray()
//) : IntegratedComponent("HotBar", tweaks) {
//
//    private val mode by enumChoice("Mode", Mode.Rise)
//
//    init {
//        registerComponentListen()
//    }
//
//    enum class Mode(override val choiceName: String) : NamedChoice {
//        Simple("Simple"),
//        Modern("Modern"),
//        Rise("Rise"),
//        Hanabi("Hanabi")
//    }
//}
//
