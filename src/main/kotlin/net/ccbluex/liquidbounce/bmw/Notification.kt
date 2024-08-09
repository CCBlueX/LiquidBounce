package net.ccbluex.liquidbounce.bmw

import net.ccbluex.liquidbounce.event.events.NotificationEvent.Severity
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.client.notification
import net.minecraft.text.Text

fun notifyAsMessage(content: String) {
    mc.player!!.sendMessage(Text.of("§e[BMW Client]§f $content"))
}

fun notifyAsNotification(content: String, severity: Severity = Severity.INFO) {
    notification("BMW Client", Text.of(content), severity)
}
