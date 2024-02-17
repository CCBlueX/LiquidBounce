package net.ccbluex.liquidbounce.features.module.modules.player.autoshop

import net.ccbluex.liquidbounce.config.AutoShopConfig
import net.ccbluex.liquidbounce.event.events.NotificationEvent
import net.ccbluex.liquidbounce.utils.client.notification
import net.minecraft.text.Text

/**
 * Loads [configFileName] and displays a notification depending on the result
 */
fun loadAutoShopConfig(configFileName: String, moduleName: String = ModuleAutoShop.name) {
    val result = AutoShopConfig.load(configFileName)
    val message = if (result)
        Text.translatable("liquidbounce.module.autoShop.reload.success")
    else Text.translatable("liquidbounce.module.autoShop.reload.error")

    notification(message, moduleName,
        if (result) NotificationEvent.Severity.INFO else NotificationEvent.Severity.ERROR
    )
}
