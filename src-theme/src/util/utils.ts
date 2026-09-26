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

import dateFormat from "dateformat";
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

const DAY = 24 * 60 * 60 * 1000;

export function date(time: number | undefined): string {
    return time === undefined ? "" : dateFormat(time, "mmm d, yyyy");
}

export function ago(time: number | undefined): string {
    if (time === undefined) {
        return "";
    }

    const days = Math.floor((Date.now() - time) / DAY);
    if (days <= 0) {
        return "today";
    } else if (days === 1) {
        return "yesterday";
    } else if (days < 7) {
        return `${days} days ago`;
    } else if (days < 14) {
        return "last week";
    } else if (days <= 28) {
        return `${Math.floor(days / 7)} weeks ago`;
    }
    return `on ${date(time)}`;
}

export function compactNumber(value: number): string {
    return value < 1000 ? value.toString() : `${(value / 1000).toFixed(1).replace(/\.0$/, "")}k`;
}

export function errorMessage(e: unknown): string {
    return e instanceof Error ? e.message : String(e);
}

/**
 * The values that are set, for lists built from conditions.
 */
export function present(...values: (string | false | null | undefined)[]): string[] {
    return values.filter((value): value is string => !!value);
}
