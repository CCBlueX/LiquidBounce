import {type Writable, writable} from "svelte/store";

export interface TDescription {
    description: string;
    x: number;
    y: number;
}

export const description: Writable<TDescription | null> = writable(null);

export const maxPanelZIndex: Writable<number> = writable(0);

export const highlightModuleName: Writable<string | null> = writable(null);

export const scaleFactor: Writable<number> = writable(2);