import {WS_BASE} from "./host";
import type {EventMap} from "./events";
import {onDestroy} from "svelte";

console.log("Connecting to server at: ", WS_BASE);

let ws: WebSocket;

function connect() {
    ws = new WebSocket(WS_BASE);

    ws.onopen = () => {
        console.log("[WS] Connected to server");
        alwaysListeners.get("socketReady")?.forEach(callback => callback());
    };

    ws.onclose = () => {
        console.log("[WS] Disconnected from server, attempting to reconnect...");
        setTimeout(() => {
            connect();
        }, 1000);
    };

    ws.onerror = (error) => {
        console.error("[WS] WebSocket error: ", error)
    };

    ws.onmessage = (event) => {
        const json = JSON.parse(event.data);
        const eventName = json.name;
        const eventData = json.event;

        alwaysListeners.get(eventName)?.forEach(callback => callback(eventData));
        listeners.get(eventName)?.forEach(callback => callback(eventData));
    }
}

const alwaysListeners = new Map<keyof EventMap, Function[]>();
const listeners = new Map<keyof EventMap, Function[]>();

export function listenAlways<NAME extends keyof EventMap>(eventName: NAME, callback: (event: EventMap[NAME]) => void) {
    if (!alwaysListeners.has(eventName)) {
        alwaysListeners.set(eventName, []);
    }

    alwaysListeners.get(eventName)!!.push(callback);
}

/**
 * Registers a event listener that will be called on every event of the given name.
 *
 * @param eventName
 * @param callback
 * @return A function that can be called to remove the listener.
 */
function listenNonComponent<NAME extends keyof EventMap>(eventName: NAME, callback: (event: EventMap[NAME]) => void) {
    if (!listeners.has(eventName)) {
        listeners.set(eventName, []);
    }

    listeners.get(eventName)!!.push(callback);

    return () => deleteListener(eventName, callback);
}

/**
 * Registers a event listener that will be called on every event of the given name.
 *
 * The listener will be automatically removed when the component is destroyed.
 *
 * Should only be used inside a Svelte component.
 *
 * @param eventName
 * @param callback
 */
export function listen<NAME extends keyof EventMap>(eventName: NAME, callback: (event: EventMap[NAME]) => void) {
    const onDestroyHook = listenNonComponent(eventName, callback);
    onDestroy(onDestroyHook);
}

/**
 * Wait next event which matches given {@link predicate}.
 */
export async function waitMatches<NAME extends keyof EventMap>(eventName: NAME, predicate: (event: EventMap[NAME]) => boolean): Promise<EventMap[NAME]> {
    return new Promise((resolve, reject) => {
        const deleteHandler = listenNonComponent(eventName, (e) => {
            try {
                if (predicate(e)) {
                    resolve(e);
                    deleteHandler();
                }
            } catch (e) {
                reject(e);
                deleteHandler();
            }
        })
    });
}

export function cleanupListeners() {
    listeners.clear();
    console.log("[WS] Cleaned up event listeners");
}

export function deleteListener<NAME extends keyof EventMap>(eventName: NAME, cb: (event: EventMap[NAME]) => void) {
    listeners.set(
        eventName,
        listeners.get(eventName)?.filter(handler => handler !== cb) ?? []
    );
}

// Send ping to server every 5 seconds
setInterval(() => {
    if (!ws) return;
    if (ws.readyState !== 1) return;

    ws.send(JSON.stringify({
        name: "ping",
        event: {}
    }));
}, 5000);

connect();
