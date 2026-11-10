import {writable} from "svelte/store";
import {REST_BASE} from "../../../../integration/host";
import {getClientUser, getConfigTracker, getCurrentServer} from "../../../../integration/rest";
import type {
    ClientUser,
    ConfigTracker,
    ConfigTrackerState,
    MarketplaceConfig,
    MarketplaceConfigDetail,
    MarketplaceItemType,
    MarketplacePublishedConfig,
    MarketplaceRevision,
    MarketplaceSort
} from "../../../../integration/types";
import {formatTimeAgo} from "../../../../util/utils";

export const UNKNOWN_SERVER_ICON = `${REST_BASE}/api/v1/client/resource?id=minecraft:textures/misc/unknown_server.png`;
export const UNKNOWN_PACK_ICON = `${REST_BASE}/api/v1/client/resource?id=minecraft:textures/misc/unknown_pack.png`;

/**
 * The types the marketplace lists, in the order of the sidebar.
 */
export const TYPE_TITLES = {
    Config: "Configs",
    Theme: "Themes",
    Addon: "Add-ons",
    Script: "Scripts"
} as const;

export type ListedType = keyof typeof TYPE_TITLES;

export const TYPE_NAMES: Record<MarketplaceItemType, string> = {
    Config: "Config",
    Theme: "Theme",
    Addon: "Add-on",
    Script: "Script",
    Other: "Other"
};

export const SORTS: Record<string, MarketplaceSort> = {
    "Top": "top",
    "New": "new"
};

export interface ConfigFilters {
    sort: string;
    featured: boolean;
    onServer: boolean;
    tags: string[];
}

export type View =
    | { kind: "browse" }
    | { kind: "config" | "item"; id: number };

/**
 * `ondone` runs once an edit or a delete went through.
 */
export type DialogRequest =
    | { kind: "load"; config: Pick<MarketplaceConfig, "id" | "address"> }
    | { kind: "publish" }
    | { kind: "edit"; detail: MarketplaceConfigDetail; ondone: () => void }
    | { kind: "update" }
    | { kind: "delete"; config: MarketplaceConfig; ondone: () => void }
    | { kind: "published"; config: MarketplacePublishedConfig };

export const dialog = writable<DialogRequest | null>(null);

export const user = writable<ClientUser | null>(null);

export const tracker = writable<ConfigTracker | null>(null);

/**
 * The root domain of the server the player is on.
 */
export const currentServer = writable<string | null>(null);

export async function refreshUser() {
    user.set(await getClientUser().catch(() => null));
}

export async function refreshTracker() {
    tracker.set(await getConfigTracker().catch(() => null));
}

export async function refreshCurrentServer() {
    const server = await getCurrentServer().catch(() => null);
    currentServer.set(server?.rootDomain ?? null);
}

export function getTrackingState(configId: number, tracker: ConfigTracker | null): ConfigTrackerState {
    return tracker?.id === configId ? tracker.state : "None";
}

/**
 * "1.2.0" as "v1.2.0". Versions that are not numbers, such as build names, stay as they are.
 */
export function formatVersion(revision: MarketplaceRevision): string {
    return /^\d/.test(revision.version) ? `v${revision.version}` : revision.version;
}

/**
 * "Updated 3 days ago · hypixel.net +1 · made on 26.2", naming the Minecraft version only when it is another
 * one than the client runs.
 */
export function formatConfigSubtitle(config: MarketplaceConfig): string {
    const [server, ...moreServers] = config.servers;
    const parts = [
        config.updatedAt === undefined ? "" : `Updated ${formatTimeAgo(config.updatedAt)}`,
        moreServers.length > 0 ? `${server} +${moreServers.length}` : server ?? "any server",
        config.protocol && !config.protocolMatches ? `made on ${config.protocol}` : config.protocol ?? ""
    ];
    return parts.filter(part => part !== "").join(" · ");
}
