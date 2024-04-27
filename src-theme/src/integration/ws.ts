import { WS_BASE } from "./host";

console.log("Connecting to server at: ", WS_BASE);

let ws: WebSocket;

function connect() {
    ws = new WebSocket(WS_BASE);
    
    ws.onopen = () => {
        console.log("[WS] Connected to server");
        if (alwaysListeners["socketReady"]) {
            for (const h of alwaysListeners["socketReady"]) {
                h();
            }
        }
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

        if (alwaysListeners[eventName]) {
            for (const callback of alwaysListeners[eventName]) {
                callback(eventData);
            }
        }

        if (listeners[eventName]) {
            for (const callback of listeners[eventName]) {
                callback(eventData);
            }
        }
    }
}

const alwaysListeners: {[name: string]: Function[]} = {};
let listeners: {[name: string]: Function[]}  = {};

export function listenAlways(eventName: string, callback: Function) {
    if (!alwaysListeners[eventName]) alwaysListeners[eventName] = [];

    alwaysListeners[eventName].push(callback)
}

export function listen(eventName: string, callback: Function) {
    if (!listeners[eventName]) listeners[eventName] = [];

    listeners[eventName].push(callback)
}

export function cleanupListeners() {
    listeners = {};
    console.log("[WS] Cleaned up event listeners");
}

export function deleteListener(eventName: string, cb: Function) {
    listeners[eventName] = listeners[eventName].filter(handler => handler !== cb);
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
