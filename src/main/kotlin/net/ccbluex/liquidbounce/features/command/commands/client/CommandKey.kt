/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2024 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LiquidBounce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 */
package net.ccbluex.liquidbounce.features.command.commands.client

import net.ccbluex.liquidbounce.bmw.*
import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.event.events.NotificationEvent.Severity
import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.features.command.builder.CommandBuilder
import net.ccbluex.liquidbounce.features.command.builder.ParameterBuilder
import java.time.LocalDateTime
import java.time.Duration

/**
 * Bind Command
 *
 * Allows you to bind a key to a module, which means that the module will be activated when the key is pressed.
 */
object CommandKey {
    fun createCommand(): Command {
        return CommandBuilder
            .begin("key")
            .parameter(
                ParameterBuilder
                    .begin<String>("key")
                    .verifiedBy(ParameterBuilder.STRING_VALIDATOR)
                    .required()
                    .build()
            )
            .handler { _, args ->
                val key = args[0] as String
                val result = getTimeFromKey(key)
                if (!result.successful) {
                    notifyAsMessage("激活失败，原因：激活码格式错误")
                    notifyAsNotification("激活失败，原因：激活码格式错误", Severity.ERROR)
                    return@handler
                }
                val now = LocalDateTime.now()
                if (Duration.between(result.startTime, now).toSeconds() in 0..60*10 &&
                    Duration.between(result.startTime, now).toDays() in 0..result.days) {

                    LiquidBounce.key = key
                    notifyAsMessage("激活成功！有效时长：" + result.days + "天，请在下次激活时使用新的激活码，此激活码无法再次使用")
                    notifyAsNotification("激活成功！有效时长：" + result.days + "天，请在下次激活时使用新的激活码，此激活码无法再次使用", Severity.SUCCESS)
                } else {
                    notifyAsMessage("激活失败，原因：激活码已过期或不存在")
                    notifyAsNotification("激活失败，原因：激活码已过期或不存在", Severity.ERROR)
                }
            }
            .build()
    }
}
