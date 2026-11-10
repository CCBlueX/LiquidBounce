import {onDestroy, tick} from "svelte";
import {get} from "svelte/store";
import {location} from "svelte-spa-router";
import {keepScreenEscape} from "./rest";
import {listenAlways} from "./ws";

interface Layer {
    close: () => void;
}

// In the order they opened, so the innermost is last
const openLayers = new Set<Layer>();

let clientKeepsEscape = false;
let syncQueued = false;
let requests = Promise.resolve();

/**
 * The client closes the screen on Esc unless the page keeps the key, which it does while a layer is open.
 */
async function syncClient() {
    if (syncQueued) {
        return;
    }
    syncQueued = true;

    // A route change closes the layers of the page it leaves, which Svelte has not done yet
    await tick();
    syncQueued = false;

    const keep = openLayers.size > 0;
    if (keep === clientKeepsEscape) {
        return;
    }
    clientKeepsEscape = keep;

    const screenName = get(location).slice(1);
    requests = requests
        .then(() => keepScreenEscape(screenName, keep))
        .catch(console.error);
}

// The client takes Esc back whenever a screen opens, so layers that are still open have to keep it again
function handleScreenOpen() {
    clientKeepsEscape = false;
    syncClient();
}

location.subscribe(handleScreenOpen);

// The in-game ClickGUI keeps its page loaded while closed, so its route does not change when it opens again
listenAlways("moduleActivation", event => {
    if (event.moduleName === "ClickGUI") {
        handleScreenOpen();
    }
});

window.addEventListener("keydown", event => {
    if (event.key === "Escape") {
        [...openLayers].at(-1)?.close();
    }
});

/**
 * Makes Esc close a dialog, dropdown or search of the calling component before it closes the screen.
 * The component says when that layer is open: `$: layer.setOpen(expanded)`.
 */
export function escapeLayer(close: () => void) {
    const layer = {close};

    function setOpen(open: boolean) {
        if (open) {
            openLayers.add(layer);
        } else {
            openLayers.delete(layer);
        }
        syncClient();
    }

    onDestroy(() => setOpen(false));

    return {setOpen};
}
