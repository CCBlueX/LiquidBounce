import {type Writable, writable} from "svelte/store";

export interface TDescription {
    description: string;
    anchor: "left" | "right",
    x: number;
    y: number;
}

export const os: Writable<string | null> = writable<string | null>(null);

export const description: Writable<TDescription | null> = writable(null);

export const maxPanelZIndex: Writable<number> = writable(0);

export const highlightModuleName: Writable<string | null> = writable(null);

export const scaleFactor: Writable<number> = writable(2);

export const showGrid: Writable<boolean> = writable(false);

export const snappingEnabled: Writable<boolean> = writable(true);

export const gridSize: Writable<number> = writable(10);

export const darken = writable(true);

/**
 * The message the ClickGUI's toast shows.
 */
export const toast = writable<{ message: string; error: boolean; id: number } | null>(null);

let toasts = 0;

export function notify(message: string, error = false) {
    toast.set({message, error, id: ++toasts});
}
