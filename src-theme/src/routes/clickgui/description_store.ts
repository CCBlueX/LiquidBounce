import {type Writable, writable} from "svelte/store";

export interface TDescription {
    description: string;
    x: number;
    y: number;
}

export const description: Writable<TDescription | null> = writable(null);