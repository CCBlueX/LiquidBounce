import {WS_BASE} from "./host";
import type {EventMap} from "./events";

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

export function listen<NAME extends keyof EventMap>(eventName: NAME, callback: (event: EventMap[NAME]) => void) {
    if (!listeners.has(eventName)) {
        listeners.set(eventName, []);
    }

    listeners.get(eventName)!!.push(callback);

    return () => deleteListener(eventName, callback);
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
