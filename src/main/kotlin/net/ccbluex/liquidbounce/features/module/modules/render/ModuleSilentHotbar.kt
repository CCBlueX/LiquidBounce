package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.injection.mixins.minecraft.item.MixinHeldItemRenderer
import net.ccbluex.liquidbounce.utils.client.SilentHotbar

/**
 * Module SilentHotbar
 *
 * Disables showing the item selected in [SilentHotbar] in the player's hand.
 *
 * Handled in [MixinHeldItemRenderer].
 */
object ModuleSilentHotbar : Module("SilentHotbar", Category.RENDER)
