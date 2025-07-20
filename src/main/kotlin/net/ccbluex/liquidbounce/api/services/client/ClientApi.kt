/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2025 CCBlueX
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
package net.ccbluex.liquidbounce.api.services.client

import net.ccbluex.liquidbounce.api.core.ApiConfig.Companion.API_BRANCH
import net.ccbluex.liquidbounce.api.core.ApiConfig.Companion.config
import net.ccbluex.liquidbounce.api.core.BaseApi
import net.ccbluex.liquidbounce.api.models.client.AutoSettings
import net.ccbluex.liquidbounce.api.models.client.Build
import net.ccbluex.liquidbounce.api.models.client.MessageOfTheDay
import java.io.Reader

object ClientApi : BaseApi(config.apiEndpointV1) {

    suspend fun requestNewestBuildEndpoint(branch: String = API_BRANCH, release: Boolean = false) =
        get<Build>("/version/newest/$branch${if (release) "/release" else ""}")

    suspend fun requestMessageOfTheDayEndpoint(branch: String = API_BRANCH) =
        get<MessageOfTheDay>("/client/$branch/motd")

    suspend fun requestSettingsList(branch: String = API_BRANCH) =
        get<Array<AutoSettings>>("/client/$branch/settings")

    suspend fun requestSettingsScript(settingId: String, branch: String = API_BRANCH) =
        get<Reader>("/client/$branch/settings/$settingId")

}
