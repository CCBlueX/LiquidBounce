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
package net.ccbluex.liquidbounce.features.module.modules.misc;

import kotlin.Unit;
import net.ccbluex.liquidbounce.config.types.Value;
import net.ccbluex.liquidbounce.event.EventHook;
import net.ccbluex.liquidbounce.event.EventManager;
import net.ccbluex.liquidbounce.event.events.GameTickEvent;
import net.ccbluex.liquidbounce.features.module.Category;
import net.ccbluex.liquidbounce.features.module.ClientModule;
import net.ccbluex.liquidbounce.utils.input.InputBind;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;

// Closes GenericContainerScreen with its title contains specified words
// Copyright (c) 2025 a114mc
// Please do not convert to kotlin
public class ModuleGUICloser extends ClientModule {
    public static final ModuleGUICloser INSTANCE = new ModuleGUICloser();

    private final Value<String> badWord = text("BadWordToClose", "Vote");

    @SuppressWarnings({"unused", "FieldCanBeLocal"})
    private EventHook<GameTickEvent> tickHandler;

    private ModuleGUICloser() {
        super("GUICloser",
                Category.MISC,
                InputUtil.UNKNOWN_KEY.getCode(),
                InputBind.BindAction.TOGGLE,
                false,
                false,
                false,
                false,
                false,
                new String[]{"AutoClose", "ContainerCloser"}
        );

        tickHandler = EventManager.INSTANCE.registerEventHook(
                GameTickEvent.class,
                new EventHook<>(
                        this,
                        event -> {
                            Screen sc = MinecraftClient.getInstance().currentScreen;
                            // Filter
                            if (sc instanceof GenericContainerScreen) {
                                Text title = sc.getTitle();
                                if (title != null && title.getString().contains(badWord.get())) {
                                    sc.close();
                                }
                            }
                            // I love kotlin
                            return Unit.INSTANCE;
                        },
                        (short) 0
                )
        );
    }
}
