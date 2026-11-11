import {type Writable, writable} from "svelte/store";

export const os: Writable<string | null> = writable<string | null>(null);

export const scaleFactor: Writable<number> = writable(2);

export const darken = writable(true);
