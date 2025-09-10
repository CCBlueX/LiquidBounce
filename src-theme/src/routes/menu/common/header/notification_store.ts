import {type Writable, writable} from "svelte/store";

export interface TNotification {
    title: string;
    message: string;
    error: boolean;
    delay?: number;
    id?: string;
    url?: string;
}

let lastMessage: string | null = null;
let lastMessageTime = 0;
const cooldown = 1000;

function createNotificationStore(): Writable<TNotification | null> {
    const store = writable<TNotification | null>(null);

    return {
        subscribe: store.subscribe,
        update: store.update,
        set: (value: TNotification | null) => {
            const now = Date.now();
            if (
                value &&
                value.message === lastMessage &&
                now - lastMessageTime < cooldown
            ) {
                return;
            }

            lastMessage = value?.message || null;
            lastMessageTime = now;
            store.set(value);
        },
    };
}

export const notification = createNotificationStore();
