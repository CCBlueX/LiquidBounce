import {onDestroy, tick} from "svelte";
import {location} from "svelte-spa-router";
import {deleteScreen, keepScreenEscape} from "./rest";
import {listenAlways} from "./ws";

/**
 * Something Esc closes before the screen, like a dialog, a dropdown or a search with text.
 */
export interface EscapeLayer {
    setOpen(open: boolean): void;
}

interface Layer {
    close: () => void;
}

// Innermost last
const layers: Layer[] = [];

let screenName = "";
let requested = {name: "", keep: false};
// Whether the client confirmed that it leaves Esc to this page
let kept = false;
let supported = true;
let queue = Promise.resolve();
let scheduled = false;

function sync() {
    scheduled = false;

    const keep = layers.length > 0;
    if (!supported || keep === requested.keep) {
        return;
    }

    const request = requested = {name: screenName, keep};
    queue = queue
        .then(() => keepScreenEscape(request.name, request.keep))
        .then(status => {
            // Older clients close the screen on Esc as before
            if (status === 404 || status === 405) {
                supported = false;
            }

            if (requested === request) {
                kept = request.keep && status === 204;
            }
        })
        .catch(() => {
            if (requested === request) {
                kept = false;
            }
        });
}

function schedule() {
    if (scheduled) {
        return;
    }

    scheduled = true;
    tick().then(sync);
}

// The client lets go of Esc on every screen change, even when the same screen opens again
function forget() {
    requested = {name: screenName, keep: false};
    kept = false;
    schedule();
}

function add(layer: Layer) {
    if (!layers.includes(layer)) {
        layers.push(layer);
        schedule();
    }
}

function remove(layer: Layer) {
    const index = layers.indexOf(layer);
    if (index !== -1) {
        layers.splice(index, 1);
        schedule();
    }
}

location.subscribe(path => {
    screenName = path.slice(1);
    forget();
});

listenAlways("virtualScreen", forget);

// The cached in-game ClickGUI page stays loaded while hidden and only learns that it opens again from this
listenAlways("moduleActivation", event => {
    if (event.moduleName === "ClickGUI") {
        forget();
    }
});

window.addEventListener("keydown", event => {
    const layer = layers[layers.length - 1];
    if (event.key !== "Escape" || !kept || !layer) {
        return;
    }

    layer.close();

    // The client kept this Esc, so a layer that stays open would leave the screen without a way out
    tick().then(() => {
        if (layers.includes(layer)) {
            remove(layer);
            deleteScreen();
        }
    });
});

/**
 * Registers a layer for the calling component that Esc closes with `close` while it is open.
 * The page keeps Esc from the client while any layer is open, so Esc closes the innermost layer instead of the screen.
 */
export function escapeLayer(close: () => void): EscapeLayer {
    const layer = {close};
    onDestroy(() => remove(layer));

    return {
        setOpen(open: boolean) {
            if (open) {
                add(layer);
            } else {
                remove(layer);
            }
        }
    };
}
