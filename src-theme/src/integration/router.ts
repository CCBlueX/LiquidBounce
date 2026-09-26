import {push as routerPush} from "svelte-spa-router";
import {writable, type Readable} from "svelte/store";

const routeChangeSequence = writable(0);

export const routeChangeStart: Readable<number> = {
    subscribe: routeChangeSequence.subscribe
};

export function push(location: string): Promise<void> {
    routeChangeSequence.update(sequence => sequence + 1);
    return routerPush(location);
}
