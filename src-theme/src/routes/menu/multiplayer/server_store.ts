import {writable} from "svelte/store";
import {getLanServers, getServers} from "../../../integration/rest";
import type {Server} from "../../../integration/types";
import {listenAlways} from "../../../integration/ws";
import type {ServerPingedEvent} from "../../../integration/events";

export const serversStore = writable<Server[]>([]);
export const lanServersStore = writable<Server[]>([]);

let prefetchPromise: Promise<void> | null = null;
let listenerRegistered = false;
let refreshing = false;
const pendingPings = new Map<string, Server>();

function mergePingedServer(target: Server, pinged: Server): Server {
    const clone = structuredClone(pinged);
    clone.id = target.id;
    clone.name = target.name;
    clone.resourcePackPolicy = target.resourcePackPolicy;
    return clone;
}

function ensureListener() {
    if (listenerRegistered) {
        return;
    }
    listenerRegistered = true;

    listenAlways("serverPinged", (pingedEvent: ServerPingedEvent) => {
        const pinged = pingedEvent.server;
        if (refreshing) {
            pendingPings.set(pinged.address, pinged);
        }

        serversStore.update((list) =>
            list.map((s) => (s.address === pinged.address ? mergePingedServer(s, pinged) : s))
        );
    });
}

export async function refreshServersStore(): Promise<void> {
    ensureListener();
    refreshing = true;
    pendingPings.clear();
    try {
        const [servers, lanServers] = await Promise.all([getServers(), getLanServers()]);
        const merged = servers.map((s) => {
            const pinged = pendingPings.get(s.address);
            return pinged ? mergePingedServer(s, pinged) : s;
        });
        pendingPings.clear();
        serversStore.set(merged);
        lanServersStore.set(lanServers);
    } finally {
        refreshing = false;
    }
}

export function prefetchServers(): Promise<void> {
    ensureListener();
    if (!prefetchPromise) {
        prefetchPromise = refreshServersStore().catch(() => {
            prefetchPromise = null;
        });
    }
    return prefetchPromise;
}

export function clearServerPrefetch(): void {
    prefetchPromise = null;
}
