//package net.ccbluex.liquidbounce.integration.theme.component.types
//
//import net.ccbluex.liquidbounce.config.types.NamedChoice
//import net.ccbluex.liquidbounce.config.types.nesting.Configurable
//import net.ccbluex.liquidbounce.integration.theme.component.ComponentTweak
//
//@Suppress("unused")
//class PlayerListComponent (
//    tweaks: Array<ComponentTweak> = emptyArray()
//) : IntegratedComponent("PlayerList", tweaks) {
//
//    init {
//        registerComponentListen()
//    }
//
//    private val highlight = tree(object : Configurable("Highlight") {
//        val friend by boolean("Friend",true)
//        val staff by  boolean("Staff",true)
//        val self by boolean("Self",true)
//    })
//
//    private val sortBy by enumChoice("SortBy", SortType.VANILLA)
//
//    enum class SortType(override val choiceName: String) : NamedChoice {
//        VANILLA("Vanilla"),
//        LATENCY("Latency"),
//        NAME_LENGTH("NameLength"),
//        ALPHABETICAL("Alphabetical"),
//        REVERSE_ALPHABETICAL("ReverseAlphabetical"),
//    }
//}

