import dateFormat from "dateformat";
import {writable} from "svelte/store";
import {REST_BASE} from "../../../../integration/host";
import {setTyping} from "../../../../integration/rest";
import type {MarketplaceInstallResult, MarketplaceItemType, MarketplaceRevision} from "../../../../integration/types";

export const UNKNOWN_SERVER = `${REST_BASE}/api/v1/client/resource?id=minecraft:textures/misc/unknown_server.png`;
export const UNKNOWN_PACK = `${REST_BASE}/api/v1/client/resource?id=minecraft:textures/misc/unknown_pack.png`;

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

export function count(value: number): string {
    return value < 1000 ? value.toString() : `${(value / 1000).toFixed(1).replace(/\.0$/, "")}k`;
}

export function reports(works: number, fails: number): string {
    return works + fails === 0 ? "no reports yet" : `${works} working · ${fails} broken`;
}

export function message(e: unknown): string {
    return e instanceof Error ? e.message : String(e);
}

export function ordinal(value: number): string {
    const tens = value % 100;
    const suffix = tens >= 11 && tens <= 13 ? "th" : ["th", "st", "nd", "rd"][value % 10] ?? "th";
    return `${value}${suffix}`;
}

export function version(revision: MarketplaceRevision): string {
    return /^v\d/i.test(revision.version) ? revision.version : `v${revision.version}`;
}

export interface MenuEntry {
    title: string;
    hint?: string;
    danger?: boolean;
    onclick: () => void;
}

export function typeName(type: MarketplaceItemType): string {
    return type === "Addon" ? "Add-on" : type;
}

export const toast = writable<{ message: string; error: boolean; id: number } | null>(null);

let toasts = 0;

export function notify(message: string, error = false) {
    toast.set({message, error, id: ++toasts});
}

export function notifyInstalled(result: MarketplaceInstallResult) {
    if (result.installed.length > 0) {
        notify(`Installed ${result.installed.join(", ")}.`);
    }
}

/**
 * Runs [action], telling the user when it fails.
 */
export async function attempt<T>(action: () => Promise<T>): Promise<T | undefined> {
    try {
        return await action();
    } catch (e) {
        notify(message(e), true);
        return undefined;
    }
}

/**
 * Keeps key presses in the text inputs below [node] from reaching the game.
 */
export function typing(node: HTMLElement) {
    let focused = false;
    const update = (target: EventTarget | null) => {
        const next = target instanceof HTMLInputElement && target.type !== "checkbox" && node.contains(target);
        if (next !== focused) {
            focused = next;
            setTyping(next);
        }
    };
    const focus = (e: FocusEvent) => update(e.target);
    const blur = (e: FocusEvent) => update(e.relatedTarget);

    node.addEventListener("focusin", focus);
    node.addEventListener("focusout", blur);

    return {
        destroy() {
            node.removeEventListener("focusin", focus);
            node.removeEventListener("focusout", blur);
            // Chromium does not blur an input that leaves the page.
            if (focused) {
                setTyping(false);
            }
        }
    };
}

/**
 * Calls [onVisible] whenever [node] scrolls into view.
 */
export function visible(node: HTMLElement, onVisible: () => void) {
    const observer = new IntersectionObserver(entries => {
        if (entries.some(entry => entry.isIntersecting)) {
            onVisible();
        }
    });
    observer.observe(node);

    return {
        update(next: () => void) {
            onVisible = next;
        },
        destroy() {
            observer.disconnect();
        }
    };
}
