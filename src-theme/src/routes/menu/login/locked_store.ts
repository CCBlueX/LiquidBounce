// ../../../components/locked_store.ts
import {writable} from "svelte/store";

const STORAGE_KEY = 'unlocked';
const initial = !(localStorage.getItem(STORAGE_KEY) === "true");
export const locked = writable(initial);

export function unlock() {
    locked.set(false);
    localStorage.setItem(STORAGE_KEY, "true");
}

export function lock() {
    locked.set(true);
    localStorage.setItem(STORAGE_KEY, "false");
    // Keep title_visited as true so it doesn't show the initial screen again
}

export const shouldZoom = writable(false);
