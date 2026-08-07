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

import type {Screen} from "../integration/types";

export const UNKNOWN_KEY = "key.keyboard.unknown";

export const isClickGuiScreen = (screen: Screen | undefined) =>
    screen !== undefined &&
    screen.class.startsWith("net.ccbluex.liquidbounce") &&
    (screen.title === "ClickGUI" || screen.title === "VS-CLICKGUI");

export function portal(node: HTMLElement, target: HTMLElement = document.body) {
    target.appendChild(node);
    return {
        destroy() {
            if (node.parentNode) node.parentNode.removeChild(node);
        }
    };
}

export function clickOutside(node: HTMLElement, callback: (event: MouseEvent) => void) {
    const handleClick = (event: MouseEvent) => {
        if (!node.contains(event.target as Node)) {
            callback(event);
        }
    };

    const handleDrag = (event: DragEvent) => {
        if (!node.contains(event.target as Node)) {
            callback(event);
        }
    };

    document.addEventListener("click", handleClick, true);
    document.addEventListener("dragstart", handleDrag, true);

    return {
        destroy() {
            document.removeEventListener("click", handleClick, true);
            document.removeEventListener("dragstart", handleDrag, true);
        }
    };
}
