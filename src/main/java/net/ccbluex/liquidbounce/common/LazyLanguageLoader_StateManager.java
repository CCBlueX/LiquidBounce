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

package net.ccbluex.liquidbounce.common;

import net.minecraft.resource.ResourceReloader;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


/*
* Code From https://github.com/ChachyDev/lazy-language-loader/blob/1.21.x/dev/src/main/java/dev/chachy/lazylanguageloader/client/impl/state/StateManager.java
 */
public class LazyLanguageLoader_StateManager {
    private static final List<ResourceReloader> reloaders = new ArrayList<>();
    private static boolean resourceLoadViaLanguage = false;

    public static boolean isResourceLoadViaLanguage() {
        return resourceLoadViaLanguage;
    }

    public static void setResourceLoadViaLanguage(boolean resourceLoadViaLanguage) {
        LazyLanguageLoader_StateManager.resourceLoadViaLanguage = resourceLoadViaLanguage;
    }

    public static List<ResourceReloader> getResourceReloaders() {
        return reloaders;
    }

    /**
     * If any developer wants to workaround lazy-language-loader you could depend on it via Jitpack and add your resource reloader here
     * if not it will not be reloaded. Sadly if your resource reloader doesn't derive from SearchManager or LanguageManager there isn't much
     * more I can do to determine that you do stuff with languages...
     *
     * @param reloader Reloader to be used on language reloads
     */
    public static void addResourceReloader(ResourceReloader reloader) {
        reloaders.add(reloader);
    }

    public static boolean isMatchable(String input, Text definition) {
        return isMatchable(input, definition.getString());
    }

    public static boolean isMatchable(String input, String definition) {
        return definition.toLowerCase(Locale.ROOT).contains(input.toLowerCase(Locale.ROOT));
    }
}
