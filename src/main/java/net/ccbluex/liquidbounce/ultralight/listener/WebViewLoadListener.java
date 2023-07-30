/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2016 - 2023 CCBlueX
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

package net.ccbluex.liquidbounce.ultralight.listener;

import net.janrupf.ujr.api.UltralightView;
import net.janrupf.ujr.api.listener.UltralightLoadListener;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class WebViewLoadListener implements UltralightLoadListener {
    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    public void onBeginLoading(UltralightView view, long frameId, boolean isMainFrame, String url) {
        LOGGER.info("Started to load {} in frame {} (is main = {})", url, frameId, isMainFrame);
    }

    @Override
    public void onFinishLoading(UltralightView view, long frameId, boolean isMainFrame, String url) {
        LOGGER.info("Finished loading {} in frame {} (is main = {})", url, frameId, isMainFrame);

    }

    @Override
    public void onFailLoading(
            UltralightView view,
            long frameId,
            boolean isMainFrame,
            String url,
            String description,
            String errorDomain,
            int errorCode
    ) {
        LOGGER.warn("Failed to load {} in frame {} (is main = {}): {} ({}) [{}]", url, frameId, isMainFrame, description, errorDomain, errorCode);
    }
}
