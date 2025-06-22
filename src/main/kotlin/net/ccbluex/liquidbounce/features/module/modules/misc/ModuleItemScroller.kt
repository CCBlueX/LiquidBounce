package net.ccbluex.liquidbounce.features.module.modules.misc

import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.minecraft.screen.ScreenHandler
import net.minecraft.screen.slot.Slot
import net.minecraft.screen.slot.SlotActionType
import org.lwjgl.glfw.GLFW

private typealias MouseClick = (Slot?, Int, Int, SlotActionType) -> Unit
private typealias ClickAction = (handler: ScreenHandler, slot: Slot, callback: MouseClick) -> Unit

/**
 * Quick item movement
 *
 * @author sqlerrorthing
 */
object ModuleItemScroller : ClientModule("ItemScroller", Category.MISC) {
    @JvmStatic
    val clickMode by enumChoice("ClickMode", ClickMode.QUICK_MOVE)

    @JvmStatic
    @Suppress("MagicNumber")
    val delay by intRange("Delay", 2..3, 0..20, suffix = "ticks")
}

@Suppress("UNUSED")
enum class ClickMode(
    override val choiceName: String,
    val action: ClickAction
) : NamedChoice {
    QUICK_MOVE("QuickMove", { _, slot, callback ->
        callback(slot, slot.id, GLFW.GLFW_MOUSE_BUTTON_LEFT, SlotActionType.QUICK_MOVE)
    })
}
