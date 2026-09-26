import {writable} from "svelte/store";
import {REST_BASE} from "../../../../integration/host";
import {ago, errorMessage, present} from "../../../../util/utils";
import type {
    ConfigTrackerState,
    MarketplaceConfig,
    MarketplaceConfigDetail,
    MarketplaceInstallResult,
    MarketplaceItem,
    MarketplaceItemType,
    MarketplacePublished,
    MarketplaceRevision
} from "../../../../integration/types";

export const UNKNOWN_SERVER = `${REST_BASE}/api/v1/client/resource?id=minecraft:textures/misc/unknown_server.png`;
export const UNKNOWN_PACK = `${REST_BASE}/api/v1/client/resource?id=minecraft:textures/misc/unknown_pack.png`;

export function reports(works: number, fails: number): string {
    return works + fails === 0 ? "no reports yet" : `${works} working · ${fails} broken`;
}

export function version(revision: MarketplaceRevision): string {
    return /^v\d/i.test(revision.version) ? revision.version : `v${revision.version}`;
}

export function trackingName(state: ConfigTrackerState): string {
    return state === "Editing" ? "Edited" : state === "Tracked" ? "Tracked" : "";
}

/**
 * The badges of a config in its [tracking] state, with [overlay] telling what it loads on.
 */
export function configBadges(config: MarketplaceConfig, tracking: ConfigTrackerState, overlay?: string | false): string[] {
    return present(
        trackingName(tracking),
        config.featured && "Featured",
        config.own && "Yours",
        config.visibility === "unlisted" && "Unlisted",
        overlay,
        config.binds && "Binds"
    );
}

/**
 * When a config was updated, which servers it is for, and the Minecraft version it was made on.
 */
export function configLine(config: MarketplaceConfig): string {
    const [server, ...more] = config.servers;
    return present(
        `Updated ${ago(config.updatedAt)}`,
        more.length > 0 ? `${server} +${more.length}` : server ?? "any server",
        config.protocol && !config.protocolMatches ? `made on ${config.protocol}` : config.protocol
    ).join(" · ");
}

/**
 * The badges of an add-on, script or theme after [first].
 */
export function itemBadges(item: MarketplaceItem, first: string | false): string[] {
    return present(first, item.restartRequired && "Restart needed", item.inUse && "In use", item.featured && "Featured");
}

export function reviews(amount: number): string {
    return `${amount} ${amount === 1 ? "review" : "reviews"}`;
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

/**
 * The dialog on screen. [ondone] runs after an edit or delete went through.
 */
export type DialogRequest =
    | { kind: "load"; config: { id: number; address: string } }
    | { kind: "publish" }
    | { kind: "published"; published: MarketplacePublished }
    | { kind: "update" }
    | { kind: "edit"; detail: MarketplaceConfigDetail; ondone: () => void }
    | { kind: "delete"; config: MarketplaceConfig; ondone: () => void };

export const dialog = writable<DialogRequest | null>(null);

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
        notify(errorMessage(e), true);
        return undefined;
    }
}
