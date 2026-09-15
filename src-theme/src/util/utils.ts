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

export function isAnniversary() {
    const now = new Date();

    const start = new Date(now.getFullYear(), 2, 31); // March 31
    const end = new Date(now.getFullYear(), 3, 7);   // April 7

    return now >= start && now <= end;
}

/**
 * Input types that accept typed characters. Types such as `checkbox`, `radio`,
 * `range` or `color` are operated with clicks only and therefore do not swallow
 * key presses.
 */
const TEXT_INPUT_TYPES = new Set([
    "text", "search", "url", "tel", "email", "password", "number",
    "date", "datetime-local", "month", "time", "week",
]);

/**
 * Whether the given event target is an element that consumes typed characters,
 * meaning key presses reaching it must not be treated as game input.
 */
export function isTextEntry(target: EventTarget | null): boolean {
    if (target instanceof HTMLTextAreaElement) {
        return true;
    }

    if (target instanceof HTMLInputElement) {
        return TEXT_INPUT_TYPES.has(target.type);
    }

    return target instanceof HTMLElement && target.isContentEditable;
}
