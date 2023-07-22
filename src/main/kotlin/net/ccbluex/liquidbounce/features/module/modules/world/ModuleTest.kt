package net.ccbluex.liquidbounce.features.module.modules.world

import net.ccbluex.liquidbounce.config.requestSettingsList
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.io.HttpClient

object ModuleTest : Module("Test", Category.WORLD) {
    override fun enable() {
        chat(requestSettingsList())
    }
}