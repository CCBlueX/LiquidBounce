import { getPersistentStorageItems, setPersistentStorageItems } from "./rest";
import type { PersistentStorageItem } from "./types";

let loadedOnce = false;

export async function insertPersistentData() {
    const items = await getPersistentStorageItems();

    for (const { key, value } of items) {
        localStorage.setItem(key, value);
    }
    loadedOnce = true;
}

export async function updatePersistentData() {
    if (!loadedOnce) {
        return;
    }

    const items: PersistentStorageItem[] = [];

    for (let i = 0; i < localStorage.length; i++) {
        const key = localStorage.key(i)!!;
        const value = localStorage.getItem(key)!!;

        items.push({
            key,
            value
        });
    }

    await setPersistentStorageItems(items);
}

window.addEventListener("storage", updatePersistentData);